# Spring Cloud 微服务改造任务清单

## Phase 1: 基础架构搭建

### 1.1 创建父 POM 项目
- [ ] 修改根目录 pom.xml 为父 POM
- [ ] 定义 Spring Boot 和 Spring Cloud 版本
- [ ] 添加子模块管理

### 1.2 创建 Gateway 服务 (vibe-gateway)
- [ ] 创建 vibe-gateway 子模块
- [ ] 添加 Gateway 依赖
- [ ] 配置路由规则
- [ ] 配置 Nacos 注册

### 1.3 创建公共模块 (vibe-common)
- [ ] 创建 vibe-common 子模块
- [ ] 抽取公共实体类
- [ ] 抽取公共工具类

## Phase 2: 支付服务改造 (vibe-payment-service)

### 2.1 代码迁移
- [ ] 创建 vibe-payment-service 子模块
- [ ] 迁移现有支付相关代码
- [ ] 迁移雪花算法实现
- [ ] 迁移规则引擎代码

### 2.2 添加微服务能力
- [ ] 添加 Nacos Discovery 依赖
- [ ] 配置 Nacos 注册中心地址
- [ ] 添加 Sentinel 依赖
- [ ] 配置 Sentinel 控制台

### 2.3 服务接口优化
- [ ] 创建 Feign 客户端接口
- [ ] 添加服务健康检查端点
- [ ] 配置 Actuator 监控

## Phase 3: 订单服务开发 (vibe-order-service)

### 3.1 基础搭建
- [ ] 创建 vibe-order-service 子模块
- [ ] 配置数据库连接
- [ ] 添加 MyBatis 依赖

### 3.2 订单功能实现
- [ ] 创建订单表结构
- [ ] 实现订单实体类
- [ ] 实现订单创建接口
- [ ] 实现订单查询接口
- [ ] 实现订单支付接口

### 3.3 服务调用集成
- [ ] 添加 OpenFeign 依赖
- [ ] 创建 PaymentFeignClient
- [ ] 实现熔断降级 Fallback
- [ ] 配置 Sentinel 流控规则

### 3.4 订单支付流程
- [ ] 创建订单（状态：待支付）
- [ ] 调用支付服务
- [ ] 处理支付结果
- [ ] 更新订单状态

## Phase 4: 联调测试

### 4.1 环境准备
- [ ] 启动 Nacos Server
- [ ] 启动 Sentinel Dashboard
- [ ] 准备测试数据

### 4.2 服务启动
- [ ] 启动 Gateway
- [ ] 启动 Payment Service
- [ ] 启动 Order Service
- [ ] 验证服务注册

### 4.3 功能测试
- [ ] 测试 Gateway 路由
- [ ] 测试订单创建
- [ ] 测试订单支付
- [ ] 测试熔断降级
- [ ] 测试限流功能

### 4.4 端到端测试
- [ ] 完整业务流程测试
- [ ] 异常情况测试
- [ ] 性能测试

## Phase 5: 文档与优化

### 5.1 文档编写
- [ ] 更新 README.md
- [ ] 编写 API 文档
- [ ] 编写部署文档

### 5.2 代码优化
- [ ] 代码审查
- [ ] 性能优化
- [ ] 日志完善
