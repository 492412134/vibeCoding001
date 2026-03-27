# 分布式微服务架构配置说明

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端 (浏览器)                          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │ 所有请求都访问 Gateway
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Cloud Gateway                          │
│                         端口: 8081                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  路由规则:                                              │   │
│  │  - /, /*.html → 支付服务静态页面                        │   │
│  │  - /static/** → 支付服务静态资源                        │   │
│  │  - /api/payment/** → 支付服务API                        │   │
│  │  - /api/rules/** → 规则引擎API                          │   │
│  │  - /api/snowflake/** → 雪花算法API                      │   │
│  │  - /api/external/** → 外部API配置                       │   │
│  │  - /api/order/** → 订单服务API                          │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
                    ▼           ▼           ▼
        ┌───────────────┐ ┌───────────┐ ┌───────────────┐
        │ 支付服务实例1  │ │ 支付服务   │ │ 支付服务实例N  │
        │ 端口: 10991    │ │ 实例2      │ │ 端口: 1099X    │
        └───────────────┘ │ 端口:10993  │ └───────────────┘
                          └───────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │      Nacos 注册中心    │
                    │      端口: 8848        │
                    └───────────────────────┘
```

## 服务端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8081 | 统一入口，负责路由和负载均衡 |
| Nacos | 8848 | 服务注册与发现中心 |
| 支付服务 | 10991 | 默认实例，可启动多个实例 |
| 订单服务 | 10992 | 订单微服务 |

## 启动顺序

1. **启动基础设施**
   ```bash
   # 启动 Nacos (Docker)
   docker start vibe-nacos
   ```

2. **启动 Gateway**
   ```bash
   # 端口: 8081
   # 启动后会自动从 Nacos 发现其他服务
   ```

3. **启动支付服务**
   ```bash
   # 默认实例端口: 10991
   # 可以启动多个实例验证负载均衡
   ```

## 访问方式

### 通过 Gateway 访问 (推荐)

所有前端页面和 API 都通过 Gateway 地址访问：

```
http://localhost:8081/
```

### 页面访问

| 页面 | Gateway URL |
|------|-------------|
| 首页 | http://localhost:8081/index.html |
| 支付压测 | http://localhost:8081/payment-demo.html |
| 规则引擎 | http://localhost:8081/rule-engine.html |
| 雪花算法 | http://localhost:8081/snowflake-demo.html |
| 订单查询 | http://localhost:8081/orders-demo.html |
| 外部API | http://localhost:8081/external-api.html |

### API 访问

| 功能 | Gateway URL |
|------|-------------|
| 支付下单 | POST http://localhost:8081/api/payment/single |
| 查询状态 | GET http://localhost:8081/api/payment/status |
| 规则列表 | GET http://localhost:8081/api/rules/list |
| 生成ID | POST http://localhost:8081/api/snowflake/generate |
| 服务实例 | GET http://localhost:8081/api/payment/instance |

## 负载均衡验证

### 方法1: 使用浏览器控制台

```javascript
// 连续调用多次，观察返回的端口号变化
fetch('/api/payment/instance')
  .then(r => r.json())
  .then(console.log)
```

### 方法2: 使用 curl

```bash
# 连续请求多次，观察 port 字段变化
for i in {1..5}; do
  curl -s http://localhost:8081/api/payment/instance | jq '.port'
done
```

### 预期结果

当启动多个支付服务实例时，请求会轮询分发到不同实例：

```json
// 第一次请求
{ "port": "10991", "instanceId": "vibe-payment-service:10991" }

// 第二次请求
{ "port": "10993", "instanceId": "vibe-payment-service:10993" }

// 第三次请求
{ "port": "10991", "instanceId": "vibe-payment-service:10991" }
```

## 启动多个支付服务实例

### 方法1: 使用不同端口启动

```bash
# 实例1 (默认端口 10991)
cd vibe-payment-service
mvn spring-boot:run

# 实例2 (端口 10993)
cd vibe-payment-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=10993"
```

### 方法2: 修改 application.yml

复制 `application.yml` 为 `application-instance2.yml`，修改端口：

```yaml
server:
  port: 10993
```

然后启动：

```bash
mvn spring-boot:run -Dspring.profiles.active=instance2
```

## 前端配置

所有前端页面已配置通过 Gateway 访问 API，配置文件：

**static/js/gateway-config.js**

```javascript
const GatewayConfig = {
    baseUrl: '',  // 空表示使用相对路径，自动通过当前域名访问
    
    getApiUrl(service, path) {
        const prefix = this.apiPrefix[service];
        return `${this.baseUrl}${prefix}${path}`;
    }
};
```

## 注意事项

1. **必须通过 Gateway 访问**: 所有页面和 API 都应该通过 `http://localhost:8081` 访问，而不是直接访问支付服务的 10991 端口

2. **负载均衡策略**: Gateway 默认使用轮询(RoundRobin)策略，可以在配置中修改为随机等其他策略

3. **服务发现**: 支付服务启动后会自动注册到 Nacos，Gateway 会自动发现并路由请求

4. **跨域问题**: Gateway 已配置 CORS，前端页面可以直接访问

## 故障排查

### 页面无法访问

1. 检查 Gateway 是否启动：`curl http://localhost:8081/actuator/health`
2. 检查支付服务是否注册到 Nacos
3. 查看 Gateway 日志确认路由配置是否生效

### 负载均衡不生效

1. 确认启动了多个支付服务实例
2. 检查 Nacos 服务列表是否显示多个实例
3. 查看 Gateway 日志确认负载均衡器工作正常

### API 请求失败

1. 检查浏览器控制台网络请求
2. 确认请求 URL 是通过 Gateway (8081) 而不是直接访问支付服务
3. 查看支付服务日志确认请求是否到达
