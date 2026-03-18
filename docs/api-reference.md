# API接口文档

> 本文档详细说明 VibeCoding001 项目的所有 RESTful API 接口。

## 1. 接口概览

### 1.1 基础信息
- **基础URL**: `http://localhost:8080`
- **响应格式**: JSON
- **字符编码**: UTF-8

### 1.2 响应结构
```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

### 1.3 状态码
| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 2. 支付服务 API

### 2.1 单笔支付

**接口**: `POST /api/payment/pay`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 用户姓名 |
| idcard | string | 是 | 身份证号 |
| bankcard | string | 是 | 银行卡号 |
| amount | string | 是 | 支付金额 |
| orderType | string | 否 | 订单类型：vip/normal，默认normal |

**请求示例**:
```json
{
  "name": "张三",
  "idcard": "33010119900101001X",
  "bankcard": "6217001234567890123",
  "amount": "100.00",
  "orderType": "vip"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "requestId": "REQ_202603181200001234",
    "status": "PENDING"
  }
}
```

---

### 2.2 获取系统状态

**接口**: `GET /api/payment/status`

**响应参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| queueSize | int | 总队列大小 |
| vipQueueSize | int | VIP队列大小 |
| normalQueueSize | int | 普通队列大小 |
| currentQps | int | 当前QPS |
| totalRequests | long | 总请求数 |
| successRate | string | 成功率 |
| totalBatches | long | 总批次数 |
| timeoutRequests | long | 超时请求数 |
| activeThreads | int | 活跃线程数 |
| waitingTasks | int | 等待任务数 |
| threadPoolQueueSize | int | 线程池队列大小 |
| threadPoolQueueCapacity | int | 线程池队列容量 |
| avgProcessTime | string | 平均处理耗时 |
| maxProcessTime | long | 最大处理耗时 |
| minProcessTime | long | 最小处理耗时 |
| avgThirdPartyTime | string | 平均第三方接口耗时 |
| thirdPartyTimeoutRate | string | 第三方接口超时率 |
| thirdPartyTps | string | 第三方接口TPS |
| avgQueueWaitTime | string | 平均队列等待时间 |
| processThroughput | string | 处理吞吐量 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "queueSize": 5,
    "vipQueueSize": 2,
    "normalQueueSize": 3,
    "currentQps": 150,
    "totalRequests": 10000,
    "successRate": "95.50%",
    "totalBatches": 1000,
    "timeoutRequests": 10,
    "activeThreads": 8,
    "waitingTasks": 0,
    "threadPoolQueueSize": 0,
    "threadPoolQueueCapacity": 1000,
    "avgProcessTime": "120.50",
    "maxProcessTime": 500,
    "minProcessTime": 50,
    "avgThirdPartyTime": "80.30",
    "thirdPartyTimeoutRate": "0.50%",
    "thirdPartyTps": "120.50",
    "avgQueueWaitTime": "15.20",
    "processThroughput": "10.50"
  }
}
```

---

### 2.3 获取线程池状态

**接口**: `GET /api/payment/threads`

**响应参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| threads | array | 线程列表 |
| threads[].threadName | string | 线程名称 |
| threads[].processingCount | int | 处理中数量 |
| threads[].isActive | boolean | 是否活跃 |
| threads[].batchId | string | 批次ID |
| threads[].processingTime | long | 处理耗时(ms) |
| activeCount | long | 活跃线程数 |
| totalCount | int | 总线程数 |

**响应示例**:
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
      },
      {
        "threadName": "payment-batch-processor-1",
        "processingCount": 0,
        "isActive": false,
        "batchId": null,
        "processingTime": 0
      }
    ],
    "activeCount": 1,
    "totalCount": 20
  }
}
```

---

### 2.4 重置指标

**接口**: `POST /api/payment/reset`

**响应示例**:
```json
{
  "code": 200,
  "message": "Metrics reset successfully"
}
```

---

## 3. 规则引擎 API

### 3.1 执行规则评估

**接口**: `POST /api/rule/evaluate`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ruleId | string | 是 | 规则ID |
| context | object | 是 | 评估上下文 |

**请求示例**:
```json
{
  "ruleId": "RULE_001",
  "context": {
    "age": 25,
    "status": "active",
    "amount": 1000
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "matched": true,
    "result": {
      "result": "APPROVED",
      "discount": 0.1
    },
    "executionTime": 15
  }
}
```

---

### 3.2 获取规则列表

**接口**: `GET /api/rule/list`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "ruleId": "RULE_001",
      "name": "VIP用户折扣规则",
      "condition": "{...}",
      "action": "return [...]",
      "priority": 10,
      "enabled": true,
      "version": 3,
      "versionComment": "调整折扣比例"
    }
  ]
}
```

---

### 3.3 添加规则

**接口**: `POST /api/rule/add`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ruleId | string | 是 | 规则ID |
| name | string | 是 | 规则名称 |
| condition | string | 是 | 条件表达式(JSON) |
| action | string | 是 | 动作代码 |
| priority | int | 否 | 优先级，默认0 |
| enabled | boolean | 否 | 是否启用，默认true |
| versionComment | string | 否 | 版本说明 |

**请求示例**:
```json
{
  "ruleId": "RULE_001",
  "name": "VIP用户折扣规则",
  "condition": "{\"type\":\"atomic\",\"field\":\"userLevel\",\"operator\":\">=\",\"value\":5}",
  "action": "return [result: \"APPROVED\", discount: 0.1]",
  "priority": 10,
  "enabled": true,
  "versionComment": "初始版本"
}
```

---

### 3.4 更新规则

**接口**: `PUT /api/rule/update`

**请求参数**: 同添加规则

**说明**: 更新规则会自动增加版本号，并保存历史记录

---

### 3.5 删除规则

**接口**: `DELETE /api/rule/delete/{ruleId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "Rule deleted successfully"
}
```

---

### 3.6 获取规则历史

**接口**: `GET /api/rule/history/{ruleId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "ruleId": "RULE_001",
      "version": 1,
      "name": "VIP用户折扣规则",
      "condition": "{...}",
      "action": "return [...]",
      "versionComment": "初始版本",
      "createdAt": "2026-03-18 10:00:00"
    },
    {
      "id": 2,
      "ruleId": "RULE_001",
      "version": 2,
      "name": "VIP用户折扣规则",
      "condition": "{...}",
      "action": "return [...]",
      "versionComment": "调整条件",
      "createdAt": "2026-03-18 11:00:00"
    }
  ]
}
```

---

### 3.7 回滚规则版本

**接口**: `POST /api/rule/rollback/{ruleId}/{version}`

**响应示例**:
```json
{
  "code": 200,
  "message": "Rollback to version 2 successful"
}
```

---

## 4. 外部API配置 API

### 4.1 获取API配置列表

**接口**: `GET /api/external-api/configs`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "apiId": "API_001",
      "apiName": "用户认证接口",
      "url": "http://example.com/api/auth",
      "method": "POST",
      "timeout": 30,
      "retryTimes": 3
    }
  ]
}
```

---

### 4.2 添加API配置

**接口**: `POST /api/external-api/config`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| apiId | string | 是 | API标识 |
| apiName | string | 是 | API名称 |
| url | string | 是 | 请求URL |
| method | string | 是 | HTTP方法 |
| headers | string | 否 | 请求头(JSON) |
| paramMapping | string | 否 | 参数映射(JSON) |
| resultMapping | string | 否 | 结果映射(JSON) |
| timeout | int | 否 | 超时时间(秒)，默认30 |
| retryTimes | int | 否 | 重试次数，默认3 |

**请求示例**:
```json
{
  "apiId": "API_001",
  "apiName": "用户认证接口",
  "url": "http://example.com/api/auth",
  "method": "POST",
  "headers": "{\"Content-Type\": \"application/json\"}",
  "paramMapping": "{\"userId\": \"${context.userId}\"}",
  "resultMapping": "{\"success\": \"${response.code == 200}\"}",
  "timeout": 30,
  "retryTimes": 3
}
```

---

### 4.3 更新API配置

**接口**: `PUT /api/external-api/config/{apiId}`

**请求参数**: 同添加API配置

---

### 4.4 删除API配置

**接口**: `DELETE /api/external-api/config/{apiId}`

---

### 4.5 测试API调用

**接口**: `POST /api/external-api/test/{apiId}`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| context | object | 是 | 请求上下文 |

**请求示例**:
```json
{
  "context": {
    "userId": "12345",
    "timestamp": "2026-03-18T12:00:00"
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "responseTime": 150,
    "result": {
      "authenticated": true,
      "userName": "张三"
    }
  }
}
```

---

## 5. 模拟第三方接口 API

### 5.1 模拟单笔支付

**接口**: `POST /api/mock/third-party/single-pay`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| requestId | string | 是 | 请求ID |
| name | string | 是 | 用户姓名 |
| idcard | string | 是 | 身份证号 |
| bankcard | string | 是 | 银行卡号 |
| amount | string | 是 | 支付金额 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "transactionId": "TXN_202603181200001234",
    "message": "支付成功",
    "processTime": 120
  }
}
```

**说明**: 此接口模拟50-200ms延迟，95%成功率

---

## 6. 错误码说明

### 6.1 支付服务错误码
| 错误码 | 说明 |
|--------|------|
| PAY_001 | 参数校验失败 |
| PAY_002 | 数据库操作失败 |
| PAY_003 | 第三方接口调用失败 |
| PAY_004 | 请求超时 |
| PAY_005 | 状态更新失败 |

### 6.2 规则引擎错误码
| 错误码 | 说明 |
|--------|------|
| RULE_001 | 规则不存在 |
| RULE_002 | 条件表达式解析失败 |
| RULE_003 | 动作执行失败 |
| RULE_004 | 规则版本不存在 |
| RULE_005 | 规则执行超时 |

### 6.3 外部API错误码
| 错误码 | 说明 |
|--------|------|
| API_001 | API配置不存在 |
| API_002 | 请求超时 |
| API_003 | 响应解析失败 |
| API_004 | 网络连接失败 |

---

## 7. 调用示例

### 7.1 cURL 示例

```bash
# 单笔支付
curl -X POST http://localhost:8080/api/payment/pay \
  -H "Content-Type: application/json" \
  -d '{
    "name": "张三",
    "idcard": "33010119900101001X",
    "bankcard": "6217001234567890123",
    "amount": "100.00",
    "orderType": "vip"
  }'

# 获取系统状态
curl http://localhost:8080/api/payment/status

# 执行规则评估
curl -X POST http://localhost:8080/api/rule/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "RULE_001",
    "context": {"age": 25, "status": "active"}
  }'
```

### 7.2 JavaScript 示例

```javascript
// 单笔支付
async function makePayment(data) {
  const response = await fetch('/api/payment/pay', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  return await response.json();
}

// 获取状态
async function getStatus() {
  const response = await fetch('/api/payment/status');
  return await response.json();
}
```

### 7.3 Java 示例

```java
// 使用 RestTemplate
RestTemplate restTemplate = new RestTemplate();

// 单笔支付
PaymentRequest request = new PaymentRequest();
request.setName("张三");
request.setIdcard("33010119900101001X");
request.setBankcard("6217001234567890123");
request.setAmount("100.00");
request.setOrderType(OrderType.VIP);

ResponseEntity<Map> response = restTemplate.postForEntity(
    "http://localhost:8080/api/payment/pay",
    request,
    Map.class
);
```

---

**文档版本**: v1.0  
**最后更新**: 2026-03-18  
**维护人员**: VibeCoding Team
