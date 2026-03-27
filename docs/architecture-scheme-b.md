# 方案B：Gateway 统一入口架构

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户浏览器                               │
│                                                                 │
│  访问地址: http://localhost:8081/                                │
│  (所有请求都通过 Gateway)                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Cloud Gateway                          │
│                         端口: 8081                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  路由规则:                                              │   │
│  │  ├─ /, /*.html ───────► vibe-web-service (前端服务)     │   │
│  │  ├─ /static/** ───────► vibe-web-service               │   │
│  │  ├─ /api/payment/** ──► vibe-payment-service (支付API)  │   │
│  │  ├─ /api/rules/** ────► vibe-payment-service (规则API)  │   │
│  │  ├─ /api/snowflake/** ► vibe-payment-service (雪花算法) │   │
│  │  ├─ /api/external/** ─► vibe-payment-service (外部API)  │   │
│  │  └─ /api/order/** ────► vibe-order-service (订单API)    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
          │                      │                      │
          ▼                      ▼                      ▼
┌─────────────────┐  ┌─────────────────────┐  ┌─────────────────┐
│  vibe-web       │  │  vibe-payment       │  │  vibe-order     │
│  (前端服务)      │  │  (支付服务)          │  │  (订单服务)      │
│  端口: 10990     │  │  端口: 10991         │  │  端口: 10992     │
│                 │  │                     │  │                 │
│ • 静态页面       │  │ • 业务API           │  │ • 订单API       │
│ • 通过Gateway   │  │ • 无页面(可独立部署)  │  │ • 无页面        │
│   访问后端       │  │ • 本地页面保留       │  │                 │
│                 │  │   (独立部署时使用)    │  │                 │
└─────────────────┘  └─────────────────────┘  └─────────────────┘
```

## 模块职责

| 模块 | 端口 | 职责 | 部署方式 |
|------|------|------|----------|
| vibe-gateway | 8081 | 统一入口、路由、负载均衡 | 必须部署 |
| vibe-web | 10990 | 前端页面，通过 Gateway 调用 API | 推荐部署 |
| vibe-payment-service | 10991 | 支付业务 API | 必须部署 |
| vibe-order-service | 10992 | 订单业务 API | 可选部署 |

## 两种访问方式

### 方式1：通过 Gateway 访问（推荐）

```
http://localhost:8081/index.html          → 前端页面 (vibe-web)
http://localhost:8081/api/payment/single  → 支付API (vibe-payment)
http://localhost:8081/api/order/list      → 订单API (vibe-order)
```

**特点：**
- 所有请求都经过 Gateway
- 支持负载均衡
- 支持统一鉴权、限流
- 前端页面调用 API 走 Gateway

### 方式2：直接访问支付服务（独立部署）

```
http://localhost:10991/index.html         → 支付服务本地页面
http://localhost:10991/api/payment/single → 支付服务本地 API
```

**特点：**
- 不经过 Gateway
- 适合单独部署支付服务
- 页面直接调用本地 API

## 前端页面 API 调用方式对比

### vibe-web 模块（走 Gateway）

```javascript
// 使用 gateway-config.js
const response = await fetch(GatewayConfig.getApiUrl('payment', '/single'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
});

// 实际请求: POST http://localhost:8081/api/payment/single
// Gateway 转发到: vibe-payment-service
```

### payment-service 模块（本地调用）

```javascript
// 使用 payment-config.js
const response = await fetch(PaymentConfig.getApiUrl('payment', '/single'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
});

// 实际请求: POST http://localhost:10991/api/payment/single
// 直接访问本地服务
```

## 启动顺序

```bash
# 1. 启动 Nacos
docker start vibe-nacos

# 2. 启动 Gateway
# 端口: 8081

# 3. 启动 vibe-web (前端服务)
# 端口: 10990

# 4. 启动 vibe-payment-service (支付服务)
# 端口: 10991

# 5. 启动 vibe-order-service (订单服务，可选)
# 端口: 10992
```

## 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Gateway | http://localhost:8081 | 统一入口 |
| 前端页面 | http://localhost:8081/index.html | 通过 Gateway 访问 |
| 支付服务 | http://localhost:10991 | 直接访问（独立部署时使用） |

## 配置文件说明

### vibe-web/src/main/resources/static/js/gateway-config.js
前端服务使用，所有 API 调用通过 Gateway。

### vibe-payment-service/src/main/resources/static/js/payment-config.js
支付服务本地页面使用，直接调用本地 API。

## 优势

1. **灵活性**：支付服务可以独立部署，也可以作为微服务集群的一部分
2. **兼容性**：保留原有页面，不影响独立部署场景
3. **统一入口**：通过 Gateway 访问时，支持负载均衡、限流、鉴权等
4. **前后端分离**：vibe-web 只负责展示，业务逻辑在后端服务

## 缺点
1. **依赖 Gateway**：前端页面调用 API 时，必须通过 Gateway 访问
