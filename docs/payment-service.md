# 支付服务文档

> 本文档详细说明支付服务的功能、架构、配置和使用方法。

## 1. 服务概述

### 1.1 功能简介
支付服务是一个高性能的支付处理系统，支持：
- 单笔支付接口接收
- 双队列优先级处理（VIP/普通订单）
- 批量聚合调用第三方接口
- 实时性能监控

### 1.2 性能指标
| 指标 | 目标值 | 说明 |
|------|--------|------|
| 峰值TPS | 600 | 每秒处理交易数 |
| 平均响应时间 | < 100ms | 单笔请求平均处理时间 |
| 吞吐量 | 100万笔/30分钟 | 总处理能力 |
| 可用性 | 99.99% | 系统可用性目标 |

## 2. 核心功能

### 2.1 双队列优先级处理

#### 2.1.1 队列设计
```java
// VIP队列 - 优先级队列
private final PriorityBlockingQueue<PaymentRequest> vipQueue = 
    new PriorityBlockingQueue<>(1000, Comparator.comparing(PaymentRequest::getCreateTime));

// 普通队列 - 普通链表队列
private final BlockingQueue<PaymentRequest> normalQueue = 
    new LinkedBlockingQueue<>(10000);
```

#### 2.1.2 优先级策略
```
获取批次时优先顺序：
1. 先检查VIP队列
2. VIP队列有数据 → 优先处理VIP订单
3. VIP队列为空 → 处理普通队列
4. 两个队列都为空 → 等待新订单
```

#### 2.1.3 订单类型枚举
```java
public enum OrderType {
    VIP("vip", "VIP订单", 1),
    NORMAL("normal", "普通订单", 0);
    
    private final String code;
    private final String description;
    private final int priority;
}
```

### 2.2 批次聚合机制

#### 2.2.1 触发条件
批次收集器在以下任一条件满足时触发：
- **数量触发**：累计收集到10条支付请求
- **时间触发**：距离上次处理超过1秒

#### 2.2.2 聚合流程
```java
public void collectRequest(PaymentRequest request) {
    // 1. 根据订单类型入队
    if (request.isVip()) {
        vipQueue.offer(request);
    } else {
        normalQueue.offer(request);
    }
    
    // 2. 触发批次收集检查
    batchLock.lock();
    try {
        currentBatch.add(request);
        
        // 3. 检查触发条件
        if (currentBatch.size() >= BATCH_SIZE || 
            System.currentTimeMillis() - lastBatchTime >= BATCH_TIMEOUT) {
            flushBatch();
        }
    } finally {
        batchLock.unlock();
    }
}
```

### 2.3 线程池管理

#### 2.3.1 线程池配置
```java
int cpuCores = Runtime.getRuntime().availableProcessors();
int coreThreads = Math.max(20, cpuCores * 2);  // 至少20个核心线程
int maxThreads = coreThreads;                   // 最大线程数等于核心线程数

batchProcessor = new ThreadPoolExecutor(
    coreThreads,
    maxThreads,
    60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),
    threadFactory,
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

#### 2.3.2 线程状态追踪
```java
public class ThreadStatus {
    private final String threadName;      // 线程名称
    private final int processingCount;    // 当前处理数量
    private final boolean isActive;       // 是否活跃
    private final long startTime;         // 开始时间
    private final String batchId;         // 批次ID
}

// 线程状态存储
private final ConcurrentHashMap<String, ThreadStatus> threadStatusMap = 
    new ConcurrentHashMap<>();
```

#### 2.3.3 线程命名规范
```java
r -> {
    int threadNum = threadStatusMap.size();
    Thread t = new Thread(r, "payment-batch-processor-" + threadNum);
    t.setDaemon(true);
    return t;
}
```
线程名称格式：`payment-batch-processor-{序号}`

### 2.4 状态管理

#### 2.4.1 支付状态枚举
```java
public enum PaymentStatus {
    PENDING("待处理"),
    PROCESSING("处理中"),
    SUCCESS("成功"),
    FAILED("失败"),
    TIMEOUT("超时");
}
```

#### 2.4.2 状态流转
```
┌─────────┐    ┌───────────┐    ┌─────────┐
│ PENDING │───▶│ PROCESSING│───▶│ SUCCESS │
└────┬────┘    └─────┬─────┘    └─────────┘
     │               │
     │               └────▶┌────────┐
     │                     │ FAILED │
     │                     └────────┘
     │
     └──────────▶┌─────────┐
                 │ TIMEOUT │
                 └─────────┘
```

#### 2.4.3 状态更新机制
```java
// CAS操作保证原子性
@Update("UPDATE payment_request SET status = 'PROCESSING', batch_id = #{batchId} " +
        "WHERE request_id = #{requestId} AND status = 'PENDING'")
int updateToProcessing(@Param("requestId") String requestId, 
                       @Param("batchId") String batchId);
```

### 2.5 服务恢复机制

#### 2.5.1 重启恢复
```java
@PostConstruct
public void init() {
    // 1. 恢复未处理的请求
    recoverPendingRequests();
    
    // 2. 初始化线程池
    initThreadPool();
    
    // 3. 启动定时任务
    startScheduledTasks();
}

private void recoverPendingRequests() {
    // 查询PENDING状态的请求
    List<PaymentRequest> pendingRequests = 
        paymentRequestRepository.findByStatus(PaymentStatus.PENDING.name());
    
    // 重新入队处理
    for (PaymentRequest request : pendingRequests) {
        if (!recoveredRequestIds.contains(request.getRequestId())) {
            collectRequest(request);
            recoveredRequestIds.add(request.getRequestId());
        }
    }
}
```

## 3. 核心类说明

### 3.1 PaymentAggregator
**位置**：`com.example.vibecoding001.payment.service.PaymentAggregator`

**核心方法**：
| 方法 | 说明 |
|------|------|
| `collectRequest()` | 收集支付请求入队 |
| `getNextBatch()` | 获取下一批次（优先VIP队列） |
| `submitBatchToProcessor()` | 提交批次到线程池 |
| `getQueueStatus()` | 获取队列状态 |
| `getThreadStatusList()` | 获取线程状态列表 |

### 3.2 PaymentController
**位置**：`com.example.vibecoding001.payment.controller.PaymentController`

**API接口**：
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/payment/pay` | POST | 单笔支付接口 |
| `/api/payment/status` | GET | 获取系统状态 |
| `/api/payment/threads` | GET | 获取线程池状态 |
| `/api/payment/reset` | POST | 重置指标 |

### 3.3 ThirdPartyPaymentService
**位置**：`com.example.vibecoding001.payment.service.ThirdPartyPaymentService`

**功能**：
- 模拟第三方支付接口
- 50-200ms随机延迟
- 95%成功率模拟

## 4. 配置参数

### 4.1 线程池配置
```properties
# 核心线程数（默认：max(20, CPU核数*2)）
payment.threadPool.coreThreads=20

# 最大线程数（默认：与核心线程数相同）
payment.threadPool.maxThreads=20

# 线程池队列容量
payment.threadPool.queueCapacity=1000
```

### 4.2 批次配置
```properties
# 批次大小（默认：10）
payment.batch.size=10

# 批次超时时间，单位毫秒（默认：1000）
payment.batch.timeout=1000
```

### 4.3 第三方接口配置
```properties
# 模拟延迟范围，单位毫秒
payment.thirdParty.minDelay=50
payment.thirdParty.maxDelay=200

# 模拟成功率
payment.thirdParty.successRate=0.95
```

## 5. 监控指标

### 5.1 队列指标
| 指标 | 说明 | 数据来源 |
|------|------|---------|
| VIP队列大小 | VIP订单等待数量 | `vipQueue.size()` |
| 普通队列大小 | 普通订单等待数量 | `normalQueue.size()` |
| 总队列大小 | 所有等待订单数 | 两者之和 |

### 5.2 线程池指标
| 指标 | 说明 | 数据来源 |
|------|------|---------|
| 处理中线程数 | 正在执行任务的线程 | `threadStatusMap`过滤 |
| 空闲线程数 | 等待任务的线程 | `threadStatusMap`过滤 |
| 线程池队列任务数 | 线程池内部队列堆积 | `batchProcessor.getQueue().size()` |
| 活跃线程数 | 线程池当前活跃线程 | `batchProcessor.getActiveCount()` |

### 5.3 性能指标
| 指标 | 说明 | 计算方式 |
|------|------|---------|
| 当前QPS | 每秒处理请求数 | 滑动窗口统计 |
| 平均处理耗时 | 批次处理平均时间 | 总耗时/批次数量 |
| 第三方接口TPS | 第三方接口吞吐量 | 成功调用数/时间 |
| 成功率 | 支付成功比例 | 成功数/总数 |

## 6. 使用示例

### 6.1 发起支付请求
```bash
curl -X POST http://localhost:8080/api/payment/pay \
  -H "Content-Type: application/json" \
  -d '{
    "name": "张三",
    "idcard": "33010119900101001X",
    "bankcard": "6217001234567890123",
    "amount": "100.00",
    "orderType": "vip"
  }'
```

### 6.2 查询系统状态
```bash
curl http://localhost:8080/api/payment/status
```

返回示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "queueSize": 5,
    "vipQueueSize": 2,
    "normalQueueSize": 3,
    "currentQps": 150,
    "activeThreads": 8,
    "waitingTasks": 0
  }
}
```

### 6.3 查询线程状态
```bash
curl http://localhost:8080/api/payment/threads
```

返回示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "threads": [
      {
        "threadName": "payment-batch-processor-0",
        "processingCount": 5,
        "isActive": true,
        "batchId": "BATCH_20260318120000_001",
        "processingTime": 1500
      }
    ],
    "activeCount": 8,
    "totalCount": 20
  }
}
```

## 7. 故障排查

### 7.1 常见问题

#### Q1: 队列积压严重
**原因**：
- 请求量超过处理能力
- 第三方接口响应慢

**解决**：
- 增加线程池大小
- 检查第三方接口状态
- 增加队列容量

#### Q2: 线程池队列始终为0
**原因**：
- 核心线程数较大，任务被立即执行
- 系统处理能力充足

**说明**：这是正常现象，表示系统运行健康

#### Q3: 状态更新失败
**原因**：
- 并发更新冲突
- 数据库连接问题

**解决**：
- 使用CAS操作保证原子性
- 检查数据库连接池配置

### 7.2 日志查看
```bash
# 查看应用日志
tail -f logs/application.log

# 查看支付相关日志
grep "Payment" logs/application.log

# 查看错误日志
grep "ERROR" logs/application.log
```

## 8. 最佳实践

### 8.1 线程池调优
```java
// IO密集型任务建议配置
// 线程数 = CPU核数 * (1 + 等待时间/计算时间)
// 假设等待时间100ms，计算时间10ms
// 线程数 = CPU核数 * (1 + 100/10) = CPU核数 * 11

int coreThreads = cpuCores * 11;
int maxThreads = coreThreads * 2;
```

### 8.2 队列容量设置
```java
// 队列容量 = 预期峰值QPS * 容忍等待时间
// 假设峰值600QPS，容忍等待2秒
// 队列容量 = 600 * 2 = 1200

int queueCapacity = 1200;
```

### 8.3 监控告警
```java
// 建议设置以下告警阈值
- 队列大小 > 5000
- 处理耗时 > 500ms
- 成功率 < 90%
- 线程池队列 > 800
```

---

**文档版本**：v1.0  
**最后更新**：2026-03-18  
**维护人员**：VibeCoding Team
