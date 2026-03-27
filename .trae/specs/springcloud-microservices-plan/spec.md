# Spring Cloud 微服务架构改造计划

## 1. 项目目标

将现有单体应用改造为 Spring Cloud 微服务架构，包含：
- **Spring Cloud Gateway**: API 网关，统一入口、路由、限流
- **OpenFeign**: 服务间声明式 HTTP 调用
- **Sentinel**: 流量控制、熔断降级
- **Nacos**: 服务注册发现与配置中心

## 2. 微服务划分

### 2.1 服务清单

| 服务名 | 端口 | 职责 | 说明 |
|--------|------|------|------|
| `vibe-gateway` | 8080 | API 网关 | 统一入口，路由转发，鉴权 |
| `vibe-payment-service` | 10991 | 支付服务 | 现有项目改造，提供支付能力 |
| `vibe-order-service` | 10992 | 订单服务 | 新建服务，调用支付服务完成订单支付 |

### 2.2 项目结构调整

```
vibeCoding001/                    # 父项目（POM聚合）
├── vibe-gateway/                 # 网关服务
├── vibe-payment-service/         # 支付服务（现有代码迁移）
├── vibe-order-service/           # 订单服务（新建）
└── vibe-common/                  # 公共模块（可选）
```

## 3. 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.x | 基础框架 |
| Spring Cloud | 2023.0.x (Kelvin) | 微服务套件 |
| Spring Cloud Alibaba | 2023.0.x | 阿里微服务组件 |
| Nacos | 2.3.x | 注册中心/配置中心 |
| Sentinel | 1.8.x | 流量控制 |
| OpenFeign | 4.x | 服务调用 |

## 4. 各服务详细设计

### 4.1 Gateway 网关服务 (vibe-gateway)

**功能**：
- 路由转发：/payment/** → 支付服务, /order/** → 订单服务
- 统一鉴权（JWT）
- 限流（Sentinel）
- 跨域处理

**路由规则**：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: payment-service
          uri: lb://vibe-payment-service
          predicates:
            - Path=/api/payment/**
        - id: order-service
          uri: lb://vibe-order-service
          predicates:
            - Path=/api/order/**
```

### 4.2 支付服务 (vibe-payment-service)

**功能**：
- 现有代码完整迁移
- 添加 Nacos 服务注册
- 添加 Sentinel 监控
- 提供 Feign 客户端接口

**核心 API**：
- POST `/api/payment/single` - 单笔支付
- POST `/api/payment/batch` - 批量支付
- GET `/api/payment/status/{requestId}` - 查询支付状态

### 4.3 订单服务 (vibe-order-service)

**功能**：
- 订单创建与管理
- 通过 OpenFeign 调用支付服务
- 订单状态流转

**核心 API**：
- POST `/api/order/create` - 创建订单
- POST `/api/order/{orderId}/pay` - 订单支付
- GET `/api/order/{orderId}` - 查询订单

**订单支付流程**：
```
1. 用户调用 POST /api/order/create
2. 订单服务创建订单（状态：待支付）
3. 用户调用 POST /api/order/{orderId}/pay
4. 订单服务通过 OpenFeign 调用支付服务
5. 支付服务返回支付结果
6. 订单服务更新订单状态
7. 返回支付结果给用户
```

## 5. 服务间调用设计

### 5.1 OpenFeign 接口定义

**订单服务调用支付服务**：
```java
@FeignClient(name = "vibe-payment-service", fallback = PaymentFeignClientFallback.class)
public interface PaymentFeignClient {
    
    @PostMapping("/api/payment/single")
    PaymentResult processPayment(@RequestBody PaymentRequest request);
    
    @GetMapping("/api/payment/status/{requestId}")
    PaymentStatus getPaymentStatus(@PathVariable String requestId);
}
```

### 5.2 Sentinel 熔断降级

```java
@Component
public class PaymentFeignClientFallback implements PaymentFeignClient {
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        return PaymentResult.fail("支付服务暂时不可用，请稍后重试");
    }
}
```

## 6. 数据流图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   用户请求   │────▶│   Gateway   │────▶│  订单服务:10992  │
└─────────────┘     │   :8080     │     └────────┬────────┘
                    └─────────────┘              │
                           │                     │ OpenFeign
                           │                     ▼
                           │            ┌─────────────────┐
                           │            │  支付服务:10991  │
                           │            │  (现有代码)      │
                           │            └─────────────────┘
                           │                     │
                           │                     ▼
                           │            ┌─────────────────┐
                           └───────────▶│   MySQL/Redis   │
                                        └─────────────────┘
```

## 7. 实施步骤

### Phase 1: 基础架构搭建
1. 创建父 POM 项目
2. 创建 Gateway 服务
3. 创建 Common 公共模块

### Phase 2: 支付服务改造
1. 迁移现有代码到 vibe-payment-service
2. 添加 Nacos 注册
3. 添加 Sentinel 监控

### Phase 3: 订单服务开发
1. 创建 vibe-order-service
2. 实现订单 CRUD
3. 集成 OpenFeign 调用支付服务
4. 添加 Sentinel 熔断

### Phase 4: 联调测试
1. 启动 Nacos Server
2. 启动所有服务
3. 端到端测试

## 8. 端口规划

| 服务 | 端口 | 说明 |
|------|------|------|
| Nacos Server | 8848 | 注册中心/配置中心 |
| Gateway | 8080 | API 网关入口 |
| Payment Service | 10991 | 支付服务 |
| Order Service | 10992 | 订单服务 |
| Sentinel Dashboard | 8858 | 流量控制面板 |

## 9. 验证清单

- [ ] Nacos 服务注册正常
- [ ] Gateway 路由转发正常
- [ ] 订单服务能正常调用支付服务
- [ ] Sentinel 限流生效
- [ ] 熔断降级功能正常
- [ ] 端到端业务流程跑通
