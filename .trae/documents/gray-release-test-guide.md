# 灰度发布功能操作测试文档

## 1. 环境准备

### 1.1 基础设施要求

- Nacos 服务（端口 8848）
- Redis 服务（端口 6379）
- Java 17+
- gray/servicesMaven 3.6+

### 1.2 启动基础设施

```bash
# 启动 Nacos（Docker 方式）
docker run -d \
  --name vibe-nacos \
  -e MODE=standalone \
  -p 8848:8848 \
  nacos/nacos-server:latest

# 启动 Redis（Docker 方式）
docker run -d \
  --name vibe-redis \
  -p 6379:6379 \
  redis:latest
```

### 1.3 验证基础设施

```bash
# 验证 Nacos
curl http://localhost:8848/nacos/v1/console/health/readiness

# 验证 Redis
redis-cli ping
```

## 2. 配置灰度规则

### 2.1 访问 Nacos 控制台

1. 打开浏览器访问：<http://localhost:8848/nacos>
2. 登录账号：nacos / nacos

### 2.2 创建灰度规则配置

1. 进入 **配置管理** → **配置列表**
2. 点击 **+** 按钮创建新配置
3. 填写配置信息：
   - **Data ID**: `gray-rules.yaml`
   - **Group**: `DEFAULT_GROUP`
   - **配置格式**: `YAML`
   - **配置内容**:

```yaml
# 灰度发布规则配置
# 在Nacos控制台中创建配置：
# 配置Data ID: gray-rules.yaml
# 配置Group: DEFAULT_GROUP
# 配置格式: YAML

gray:
  rules:
    # 支付服务灰度规则
    - service: vibe-payment-service
      enabled: true
      weight: 30  # 30%流量进入灰度版本
      conditions:
        # 用户ID条件
        - type: user_id
          operator: in
          values: ["1001", "1002", "1003"]
        # IP地址条件
        - type: ip
          operator: regex
          values: ["192\\.168\\.1\\..*"]
      version: v2
    
    # 订单服务灰度规则（禁用状态）
    - service: vibe-order-service
      enabled: false
      weight: 0
      conditions: []
      version: v1
```

1. 点击 **发布** 按钮

## 3. 启动服务

### 3.1 启动 Gateway 服务

```bash
cd /Users/linzx/idea_projects/lzx/vibeCoding001/vibe-gateway
mvn spring-boot:run
```

### 3.2 启动支付服务 v1 版本（默认版本）

```bash
# 终端1
cd /Users/linzx/idea_projects/lzx/vibeCoding001/vibe-payment-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=10991"
```

### 3.3 启动支付服务 v2 版本（灰度版本）

```bash
# 终端2
cd /Users/linzx/idea_projects/lzx/vibeCoding001/vibe-payment-service
SERVICE_VERSION=v2 mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=10993"
```

### 3.4 验证服务注册

1. 访问 Nacos 控制台：<http://localhost:8848/nacos>
2. 进入 **服务管理** → **服务列表**
3. 确认 `vibe-payment-service` 有两个实例：
   - 一个实例版本为 `v1`（端口 10991）
   - 一个实例版本为 `v2`（端口 10993）

## 4. 测试灰度功能

### 4.1 访问灰度观测页面

1. 打开浏览器访问：<http://localhost:8080/index.html>
2. 点击 **🎨 灰度发布观测** 按钮
3. 查看当前灰度配置和服务实例状态

### 4.2 测试场景1：灰度用户ID匹配

**测试目的**：验证指定用户ID的请求是否路由到灰度版本

**测试步骤**：

```bash
# 发送请求，携带灰度用户ID
curl -X POST "http://localhost:8081/api/payment/single" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1001" \
  -H "Authorization: Bearer <your_token>" \
  -d '{"amount": 100, "orderId": "gray-test-001"}'
```

**预期结果**：

- 请求应该路由到 v2 版本（端口 10993）
- 灰度观测页面显示路由日志，标记为 `[GRAY ROUTE]`
- 统计数据中灰度请求数增加

### 4.3 测试场景2：非灰度用户ID

**测试目的**：验证非灰度用户ID的请求是否路由到正常版本

**测试步骤**：

```bash
# 发送请求，携带非灰度用户ID
curl -X POST "http://localhost:8081/api/payment/single" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 9999" \
  -H "Authorization: Bearer <your_token>" \
  -d '{"amount": 200, "orderId": "gray-test-002"}'
```

**预期结果**：

- 请求应该路由到 v1 版本（端口 10991）
- 灰度观测页面显示路由日志，标记为 `[NORMAL ROUTE]`
- 统计数据中正常请求数增加

### 4.4 测试场景3：权重分配

**测试目的**：验证基于权重的流量分配

**测试步骤**：

1. 修改 Nacos 配置，移除用户ID条件：

```yaml
gray:
  rules:
    - service: vibe-payment-service
      enabled: true
      weight: 30
      conditions: []  # 清空条件，仅基于权重
      version: v2
```

1. 连续发送多个请求：

```bash
for i in {1..10}; do
  curl -X POST "http://localhost:8081/api/payment/single" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <your_token>" \
    -d "{\"amount\": 100, \"orderId\": \"weight-test-$i\"}"
  echo ""
done
```

**预期结果**：

- 大约 30% 的请求路由到 v2 版本
- 大约 70% 的请求路由到 v1 版本
- 灰度观测页面显示灰度比例约为 30%

### 4.5 测试场景4：禁用灰度规则

**测试目的**：验证禁用灰度规则后所有请求路由到正常版本

**测试步骤**：

1. 修改 Nacos 配置：

```yaml
gray:
  rules:
    - service: vibe-payment-service
      enabled: false  # 禁用灰度
      weight: 30
      conditions:
        - type: user_id
          operator: in
          values: ["1001", "1002", "1003"]
      version: v2
```

1. 发送请求：

```bash
curl -X POST "http://localhost:8081/api/payment/single" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1001" \
  -H "Authorization: Bearer <your_token>" \
  -d '{"amount": 100, "orderId": "disabled-test"}'
```

**预期结果**：

- 即使携带灰度用户ID，请求也路由到 v1 版本
- 灰度观测页面显示服务状态为"未启用"

## 5. 观测灰度效果

### 5.1 查看统计数据

在灰度观测页面查看：

- **总请求数**：所有请求的总量
- **灰度请求数**：路由到灰度版本的请求数
- **正常请求数**：路由到正常版本的请求数
- **灰度比例**：灰度请求占总请求的百分比

### 5.2 查看路由日志

在灰度观测页面的路由日志区域查看：

- 每条日志显示请求的服务、版本、条件和URI
- 灰色日志表示灰度路由
- 绿色日志表示正常路由

### 5.3 查看服务实例状态

在灰度观测页面查看：

- 每个服务的实例数量
- 每个实例的版本信息
- 当前的灰度规则配置

## 6. 动态调整灰度规则

### 6.1 调整灰度比例

1. 在 Nacos 控制台修改配置：

```yaml
gray:
  rules:
    - service: vibe-payment-service
      enabled: true
      weight: 50  # 从30%调整到50%
      conditions: []
      version: v2
```

1. 发布配置后，Gateway 会自动刷新（无需重启）
2. 继续发送请求验证新的灰度比例

### 6.2 添加新的灰度条件

1. 在 Nacos 控制台修改配置：

```yaml
gray:
  rules:
    - service: vibe-payment-service
      enabled: true
      weight: 30
      conditions:
        # 用户ID条件
        - type: user_id
          operator: in
          values: ["1001", "1002", "1003"]
        # IP地址条件
        - type: ip
          operator: regex
          values: ["192\\.168\\.1\\..*"]
      version: v2
```

1. 发布配置后测试新的条件

## 7. 回滚操作

### 7.1 快速回滚

1. 在 Nacos 控制台修改配置：

```yaml
gray:
  rules:
    - service: vibe-payment-service
      enabled: false  # 禁用灰度
      weight: 0
      conditions: []
      version: v1
```

1. 发布配置，所有请求立即路由到 v1 版本

### 7.2 停止灰度实例

```bash
# 停止 v2 版本的支付服务实例
# 找到对应的进程并终止
ps aux | grep "SERVICE_VERSION=v2"
kill -9 <pid>
```

## 8. 清理和重置

### 8.1 清空路由日志

在灰度观测页面点击 **🗑️ 清空** 按钮，或调用 API：

```bash
curl -X DELETE "http://localhost:8081/api/gray/logs"
```

### 8.2 重置统计数据

调用 API 重置统计数据：

```bash
curl -X POST "http://localhost:8081/api/gray/stats/reset"
```

## 9. 常见问题排查

### 9.1 灰度路由不生效

**检查项**：

1. 确认 Nacos 配置已正确发布
2. 确认 Gateway 配置中 `spring.cloud.nacos.config.enabled` 未被禁用
3. 确认服务实例已正确注册版本元数据
4. 查看 Gateway 日志是否有错误信息

### 9.2 服务实例版本不正确

**检查项**：

1. 确认启动时设置了正确的 `SERVICE_VERSION` 环境变量
2. 在 Nacos 服务列表查看实例的元数据
3. 重启服务实例

### 9.3 配置变更不生效

**检查项**：

1. 确认 Nacos 配置已发布
2. 确认 Gateway 的配置刷新功能已启用
3. 查看 Gateway 日志中的配置刷新记录

## 10. 最佳实践建议

### 10.1 灰度发布流程

1. **准备阶段**：部署新版本实例，但不启用灰度规则
2. **小规模测试**：启用灰度规则，设置较小权重（5-10%）
3. **逐步扩大**：观察无问题后，逐步增加权重（20% → 50% → 80%）
4. **全量发布**：将所有流量切换到新版本
5. **清理阶段**：下线旧版本实例

### 10.2 监控建议

- 持续监控灰度观测页面的统计数据
- 关注错误率和响应时间
- 设置告警阈值，异常时自动回滚

### 10.3 安全建议

- 灰度规则变更需要审批
- 保留配置变更历史
- 定期备份灰度规则配置

## 11. API 接口参考

### 11.1 获取灰度规则

```bash
GET http://localhost:8081/api/gray/rules
```

### 11.2 获取服务实例

```bash
GET http://localhost:8081/api/gray/services
```

### 11.3 获取路由日志

```bash
GET http://localhost:8081/api/gray/logs
```

### 11.4 获取统计数据

```bash
GET http://localhost:8081/api/gray/stats
```

### 11.5 清空路由日志

```bash
DELETE http://localhost:8081/api/gray/logs
```

### 11.6 重置统计数据

```bash
POST http://localhost:8081/api/gray/stats/reset
```

## 12. 测试检查清单

- [ ] Nacos 服务正常运行
- [ ] Redis 服务正常运行
- [ ] Gateway 服务正常启动
- [ ] 支付服务 v1 版本正常启动
- [ ] 支付服务 v2 版本正常启动
- [ ] 灰度规则配置已创建
- [ ] 灰度用户ID匹配测试通过
- [ ] 非灰度用户ID测试通过
- [ ] 权重分配测试通过
- [ ] 灰度禁用测试通过
- [ ] 灰度观测页面正常显示
- [ ] 路由日志正常记录
- [ ] 统计数据正常更新
- [ ] 动态配置刷新正常
- [ ] 回滚操作正常

***

**文档版本**: v1.0\
**更新日期**: 2026-03-28\
**作者**: Vibe Coding Team
