# 灰度发布功能集成测试指南

## 测试环境准备

### 1. 启动基础设施
```bash
# 启动 Nacos 服务
./start-nacos-docker.sh

# 等待 Nacos 启动完成
sleep 10
```

### 2. 启动多个版本的支付服务实例

#### 启动 v1 版本实例（默认版本）
```bash
# 在支付服务目录下执行
cd vibe-payment-service

# 启动 v1 版本实例（默认）
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=10991"
```

#### 启动 v2 版本实例（灰度版本）
```bash
# 在另一个终端执行
cd vibe-payment-service

# 启动 v2 版本实例
SERVICE_VERSION=v2 mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=10993"
```

### 3. 启动 Gateway 服务
```bash
# 在 Gateway 目录下执行
cd vibe-gateway

# 启动 Gateway
mvn spring-boot:run
```

### 4. 配置灰度规则

1. 访问 Nacos 控制台：http://localhost:8848/nacos
2. 登录账号：nacos / nacos
3. 进入配置管理 → 配置列表
4. 点击 "+" 按钮添加新配置：
   - Data ID: gray-rules.yaml
   - Group: DEFAULT_GROUP
   - 配置格式: YAML
   - 配置内容：

```yaml
gray:
  rules:
    - service: vibe-payment-service
      enabled: true
      weight: 50  # 50%流量进入灰度版本
      conditions:
        - type: user_id
          operator: in
          values: ["1001", "1002", "1003"]
      version: v2
```

## 测试场景

### 场景1：用户ID匹配的请求

**测试步骤**：
1. 发送请求，携带用户ID 1001
```bash
curl -X POST "http://localhost:8081/api/payment/single" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1001" \
  -d '{"amount": 100, "orderId": "test123"}'
```

**预期结果**：
- 请求应该被路由到 v2 版本的支付服务实例（端口 10993）
- Gateway 日志中应该有 "Gray routing enabled for service: vibe-payment-service, version: v2" 的日志

### 场景2：用户ID不匹配的请求

**测试步骤**：
1. 发送请求，携带用户ID 9999
```bash
curl -X POST "http://localhost:8081/api/payment/single" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 9999" \
  -d '{"amount": 100, "orderId": "test123"}'
```

**预期结果**：
- 请求应该被路由到 v1 版本的支付服务实例（端口 10991）
- Gateway 日志中不应该有灰度路由的日志

### 场景3：基于权重的流量分配

**测试步骤**：
1. 修改 Nacos 中的灰度规则，设置 weight: 50
2. 连续发送多个请求，不携带用户ID
```bash
for i in {1..10}; do
  curl -X POST "http://localhost:8081/api/payment/single" \
    -H "Content-Type: application/json" \
    -d '{"amount": 100, "orderId": "test'$i'"}'
  echo ""
done
```

**预期结果**：
- 大约 50% 的请求应该被路由到 v2 版本
- 大约 50% 的请求应该被路由到 v1 版本
- Gateway 日志中应该有相应的灰度路由日志

### 场景4：禁用灰度规则

**测试步骤**：
1. 修改 Nacos 中的灰度规则，设置 enabled: false
2. 发送请求，携带用户ID 1001
```bash
curl -X POST "http://localhost:8081/api/payment/single" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1001" \
  -d '{"amount": 100, "orderId": "test123"}'
```

**预期结果**：
- 请求应该被路由到 v1 版本的支付服务实例
- Gateway 日志中不应该有灰度路由的日志

## 验证方法

### 1. 查看 Gateway 日志
```bash
# 在 Gateway 终端查看日志
# 应该能看到灰度路由的相关日志
```

### 2. 查看支付服务日志
```bash
# 在 v1 版本实例终端查看日志
# 在 v2 版本实例终端查看日志
# 确认请求是否正确路由到对应的实例
```

### 3. 检查 Nacos 服务列表
- 访问 Nacos 控制台：http://localhost:8848/nacos
- 进入服务管理 → 服务列表
- 查看 vibe-payment-service 的实例列表，应该能看到两个实例，分别标记为 v1 和 v2 版本

## 故障排查

### 1. 灰度路由不生效
- 检查 Nacos 配置是否正确
- 检查 Gateway 日志是否有错误信息
- 检查服务实例是否正确注册到 Nacos
- 检查服务实例的版本元数据是否正确设置

### 2. 服务实例无法注册
- 检查 Nacos 服务是否正常运行
- 检查服务的 Nacos 配置是否正确
- 检查网络连接是否正常

### 3. 配置变更不生效
- 检查 Nacos 配置是否正确发布
- 检查 Gateway 是否开启了配置刷新（refresh-enabled: true）
- 查看 Gateway 日志是否有配置更新的日志

## 测试完成后清理

```bash
# 停止所有服务
# 在各个服务终端按 Ctrl+C 停止服务

# 停止 Nacos 服务
docker stop vibe-nacos
```