# VibeCoding001 - 高性能支付服务与规则引擎

> 一个支持百万级并发支付的微服务系统，集成规则引擎实现灵活的业务逻辑处理。

## 📋 项目概述

### 核心功能
- **高性能支付服务**：支持30分钟内完成100万笔支付，峰值TPS达600
- **双队列优先级处理**：VIP订单优先处理，普通订单排队等待
- **实时监控系统**：队列状态、线程池状态、性能指标实时监控
- **规则引擎**：支持复杂条件组合、版本控制、执行监控

### 技术栈
| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.3 | 应用框架 |
| MyBatis | 4.0.1 | ORM框架 |
| MySQL | 8.0+ | 数据存储 |
| JDK | 17 | 运行环境 |
| Maven | 3.8+ | 构建工具 |

## 🚀 快速开始

### 1. 环境准备
```bash
# 克隆项目
git clone <repository-url>
cd vibeCoding001

# 配置数据库
# 修改 src/main/resources/application.properties 中的数据库连接信息
```

### 2. 数据库初始化
```bash
# 创建数据库
create database rule_engine charset=utf8mb4;
create database payment_db charset=utf8mb4;

# 执行DDL脚本
source src/main/resources/sql/rule.sql
source src/main/resources/sql/rule_execution_log.sql
source src/main/resources/sql/external_api_config.sql
source src/main/resources/db/payment_ddl.sql
```

### 3. 启动应用
```bash
# 使用Maven启动
./mvnw spring-boot:run

# 或使用打包后的jar
./mvnw clean package
java -jar target/vibeCoding001-0.0.1-SNAPSHOT.jar
```

### 4. 访问服务
- 支付服务测试页面：http://localhost:8080/payment-demo.html
- 规则引擎管理页面：http://localhost:8080/rule-engine.html
- 外部API配置页面：http://localhost:8080/external-api.html

## 📚 文档目录

### 核心文档
- [项目架构文档](docs/architecture.md) - 系统整体架构设计
- [支付服务文档](docs/payment-service.md) - 支付服务详细说明
- [规则引擎文档](docs/rule-engine.md) - 规则引擎使用指南
- [API接口文档](docs/api-reference.md) - RESTful API参考
- [数据库设计文档](docs/database-design.md) - 数据库表结构设计
- [部署配置文档](docs/deployment.md) - 部署和运维指南

### 需求文档
- [100万笔支付需求](requirements/100W-payment-requirements.md) - 支付系统需求规格
- [规则引擎功能规划](rule-engine-features.md) - 规则引擎功能列表

### 其他文档
- [版本升级说明](VERSION_UPGRADE.md) - 数据库升级和版本变更
- [项目模板](project-template.md) - Spring Boot项目脚手架

## 🏗️ 项目结构

```
vibeCoding001/
├── docs/                          # 项目文档
├── requirements/                  # 需求文档
├── src/
│   └── main/
│       ├── java/com/example/vibecoding001/
│       │   ├── payment/           # 支付服务模块
│       │   │   ├── controller/    # REST API控制器
│       │   │   ├── service/       # 业务逻辑服务
│       │   │   ├── model/         # 数据模型
│       │   │   ├── repository/    # 数据访问层
│       │   │   └── enums/         # 枚举定义
│       │   ├── rule/              # 规则引擎模块
│       │   │   ├── condition/     # 条件表达式
│       │   │   ├── external/      # 外部API调用
│       │   │   ├── RuleEngine.java
│       │   │   └── RuleController.java
│       │   ├── entity/            # 实体类
│       │   ├── mapper/            # MyBatis Mapper
│       │   └── controller/        # 通用控制器
│       └── resources/
│           ├── mapper/            # MyBatis XML配置
│           ├── static/            # 静态资源(HTML页面)
│           ├── sql/               # 数据库脚本
│           └── application.properties
├── logs/                          # 应用日志
├── pom.xml                        # Maven配置
└── README.md                      # 本文件
```

## 🔧 配置说明

### 应用配置 (application.properties)
```properties
# 服务端口
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/rule_engine?useUnicode=true&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=sr123456

# 线程池配置
payment.threadPool.coreSize=20
payment.threadPool.maxSize=40
payment.threadPool.queueCapacity=1000

# 日志配置
logging.level.com.example.vibecoding001=info
```

## 📊 性能指标

### 支付服务性能
- 峰值处理能力：600 TPS
- 平均响应时间：< 100ms
- 队列聚合策略：10条/1秒
- 线程池配置：20核心线程，最大40线程

### 监控指标
- 实时QPS
- 队列大小（VIP/普通队列）
- 线程池状态（处理中/空闲线程）
- 第三方接口TPS
- 处理耗时统计

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

[MIT](LICENSE) © 2026 VibeCoding Team
