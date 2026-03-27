# Vibe 项目部署体系建设路线图

> 本文档记录从本地开发到生产部署的完整体系建设过程，为后续升级 K8s 和实现灰度发布做准备。

---

## 📌 项目目标

1. **本地开发**：IDEA 调试后端 + Docker 运行前端
2. **一键部署**：脚本化构建并部署到 Docker
3. **文档沉淀**：每个阶段都有可复用的操作手册
4. **K8s 准备**：目录结构和配置预留，方便后续迁移
5. **发布策略**：支持灰度、蓝绿、金丝雀等多种发布模式

---

## 🗓️ 第一阶段：本地开发环境标准化

**目标**：让开发者在本地能快速调试前后端分离项目

### 1.1 配置本地开发调试方案
- [ ] 停止 Docker 中的后端服务（只保留 Nginx）
- [ ] IDEA 中配置 Gateway 启动（端口 8081）
- [ ] IDEA 中配置 Payment 启动（端口 10991）
- [ ] IDEA 中配置 Order 启动（端口 10992）
- [ ] 验证：http://localhost 能访问前端，API 能调到 IDEA 启动的后端

### 1.2 统一环境配置管理
- [ ] 创建 `config/` 目录存放各环境配置
- [ ] 区分 application-dev.yml、application-test.yml、application-prod.yml
- [ ] 配置 Nacos 不同命名空间（dev/test/prod）

### 1.3 本地一键启动脚本
- [ ] 编写 `scripts/local-start.sh`：启动 Nginx Docker
- [ ] 编写 `scripts/local-stop.sh`：停止所有服务
- [ ] 编写 `scripts/local-logs.sh`：查看日志

**完成标准**：新成员能在 5 分钟内搭建好本地开发环境

---

## 🗓️ 第二阶段：Docker 部署标准化

**目标**：实现一键构建并部署到 Docker

### 2.1 完善多服务 Dockerfile
- [ ] 优化 Gateway Dockerfile（多阶段构建减小体积）
- [ ] 优化 Payment Dockerfile（多阶段构建）
- [ ] 创建 Order Dockerfile
- [ ] 统一基础镜像版本

### 2.2 构建 docker-compose 完整编排
- [ ] 完善 docker-compose.yml（包含所有服务）
- [ ] 创建 docker-compose.override.yml（本地开发覆盖配置）
- [ ] 创建 docker-compose.prod.yml（生产环境配置）
- [ ] 可选：添加 Nacos、Sentinel、Redis、MySQL 服务

### 2.3 制作一键部署脚本
- [ ] 编写 `scripts/build.sh`：编译所有服务并打包镜像
- [ ] 编写 `scripts/deploy.sh`：部署到 Docker
- [ ] 编写 `scripts/rollback.sh`：回滚到上一版本

### 2.4 配置 Docker 环境变量和配置文件分离
- [ ] 使用 `.env` 文件管理环境变量
- [ ] 配置文件外置挂载（不打包进镜像）
- [ ] 敏感信息使用 Docker Secrets 或环境变量注入

**完成标准**：执行 `./scripts/deploy.sh` 能完整部署所有服务

---

## 🗓️ 第三阶段：文档和流程固化

**目标**：沉淀文档，方便团队协作和后续维护

### 3.1 编写 Docker 部署文档
- [ ] 目录结构说明
- [ ] Dockerfile 编写规范
- [ ] docker-compose 使用指南
- [ ] 常见问题排查

### 3.2 编写本地开发调试指南
- [ ] 环境准备（JDK、Maven、Docker）
- [ ] IDEA 配置步骤
- [ ] 调试技巧（断点、日志）
- [ ] 端口占用排查

### 3.3 编写一键部署操作手册
- [ ] 部署前检查清单
- [ ] 部署步骤详解
- [ ] 验证方法
- [ ] 回滚操作

**完成标准**：新运维人员能根据文档独立完成部署

---

## 🗓️ 第四阶段：K8s 准备（为后续升级铺垫）

**目标**：预留 K8s 迁移能力，不强制现在使用

### 4.1 设计 K8s 目录结构
```
k8s/
├── base/                    # 基础资源
│   ├── namespace.yaml
│   ├── configmap.yaml
│   └── secret.yaml
├── overlays/
│   ├── dev/                # 开发环境
│   ├── test/               # 测试环境
│   └── prod/               # 生产环境
└── helm/                   # Helm Chart（可选）
```

### 4.2 编写基础 K8s 资源模板
- [ ] Deployment 模板（多副本、滚动更新策略）
- [ ] Service 模板（ClusterIP、NodePort）
- [ ] Ingress 模板（Nginx Ingress Controller）
- [ ] ConfigMap 和 Secret 模板

### 4.3 配置健康检查和优雅停机
- [ ] 配置 Liveness Probe（存活检查）
- [ ] 配置 Readiness Probe（就绪检查）
- [ ] 配置 Graceful Shutdown（优雅停机）
- [ ] 配置 PreStop Hook

**完成标准**：能在本地 Minikube 或测试 K8s 集群运行

---

## 🗓️ 第五阶段：发布策略准备

**目标**：支持多种发布模式，降低发布风险

### 5.1 设计灰度发布方案
- [ ] 基于 Nginx 权重分流
- [ ] 基于 Gateway 路由规则分流
- [ ] 基于用户标识（如用户ID哈希）分流
- [ ] 监控和回滚机制

### 5.2 设计蓝绿发布方案
- [ ] 双环境部署（Blue/Green）
- [ ] 流量切换机制
- [ ] 快速回滚能力

### 5.3 设计金丝雀发布方案
- [ ] 小流量验证（如 5% 流量）
- [ ] 渐进式放量（5% → 25% → 50% → 100%）
- [ ] 自动回滚条件（错误率、延迟阈值）

### 5.4 配置 CI/CD 流水线基础
- [ ] Jenkins/GitLab CI/GitHub Actions 选型
- [ ] 自动化测试阶段
- [ ] 镜像构建和推送
- [ ] 自动化部署触发

**完成标准**：能根据业务场景选择合适的发布策略

---

## 📊 当前进度

| 阶段 | 进度 | 状态 |
|------|------|------|
| 第一阶段 | 0% | 🟡 未开始 |
| 第二阶段 | 30% | 🟡 进行中（基础 Docker 配置完成） |
| 第三阶段 | 0% | 🟡 未开始 |
| 第四阶段 | 0% | ⚪ 待规划 |
| 第五阶段 | 0% | ⚪ 待规划 |

---

## 🚀 下一步行动

**当前建议**：从 **第一阶段 1.1** 开始

原因：
1. 你现在已经有 Docker Nginx 在运行
2. 先搞定本地开发调试，方便后续开发和测试
3. 等本地调通后，再完善一键部署脚本

---

## 📝 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|---------|------|
| 2026-03-24 | v1.0 | 初始版本，规划五个阶段 | Trae |

---

## 📚 参考文档

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Spring Cloud 官方文档](https://spring.io/projects/spring-cloud)
