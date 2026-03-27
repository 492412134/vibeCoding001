# Docker 环境启动指南

本项目支持两种 Docker 运行模式，根据开发需求选择使用。

***

## 环境说明

| 环境          | 配置文件                       | 容器名                | 用途                         |
| ----------- | -------------------------- | ------------------ | -------------------------- |
| 全 Docker 环境 | `docker-compose.yml`       | `vibe-nginx`       | 所有服务都在容器中运行，用于演示/测试        |
| 本地开发环境      | `docker-compose.local.yml` | `vibe-nginx-local` | Nginx Docker + 本地微服务 Debug |

***

## 快速启动

### 全 Docker 环境
```bash
docker-compose up -d
```

### 本地开发环境
```bash
docker-compose -f docker-compose.local.yml up -d nginx
```

***

## 一、全 Docker 环境

### 启动所有服务

```bash
docker-compose up -d
```

### 启动单个服务

```bash
# 启动 Nginx
docker-compose up -d nginx

# 启动 Nacos
docker-compose up -d nacos

# 启动全部微服务
docker-compose up -d vibe-gateway vibe-auth vibe-payment-service
```

### 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止单个服务
docker-compose stop nginx
```

### 查看日志

```bash
# 查看所有日志
docker-compose logs -f

# 查看单个服务日志
docker-compose logs -f nginx
docker-compose logs -f vibe-gateway
```

### 重启服务

```bash
docker-compose restart nginx
```

***

## 二、本地开发环境（Debug 模式）

### 使用场景

- 在 Trae/IDEA 中 Debug 微服务
- Nginx 在 Docker 中运行，转发请求到本地微服务

### 启动步骤

```bash
# 1. 启动基础设施（Nacos）
docker-compose up -d nacos

# 2. 启动本地开发版 Nginx
docker-compose -f docker-compose.local.yml up -d nginx

# 3. 在 Trae 中 Debug 启动微服务
#    - vibe-gateway (端口 8081)
#    - vibe-auth (端口 8082)
#    - vibe-payment-service (端口 8083)

# 4. 访问测试
curl http://localhost/api/auth/login
```

### 停止本地开发环境

```bash
docker-compose -f docker-compose.local.yml down
```

***

## 三、常用命令速查

### 查看运行状态

```bash
# 查看所有容器
docker ps -a

# 查看 Docker Compose 管理的服务
docker-compose ps
```

### 进入容器

```bash
# 进入 Nginx 容器
docker exec -it vibe-nginx sh
docker exec -it vibe-nginx-local sh
```

### 清理资源

```bash
# 停止并删除所有容器、网络
docker-compose down

# 同时删除 volumes
docker-compose down -v

# 删除所有未使用的镜像、容器、网络
docker system prune -a
```

### 端口占用检查

```bash
# 检查 80 端口占用
lsof -i :80

# 检查 8081 端口占用
lsof -i :8081
```

***

## 四、服务端口一览

| 服务       | 端口   | 说明        |
| -------- | ---- | --------- |
| Nginx    | 80   | 前端入口      |
| Gateway  | 8081 | API 网关    |
| Auth     | 8082 | 认证服务      |
| Payment  | 8083 | 支付服务      |
| Nacos    | 8848 | 注册中心/配置中心 |
| Sentinel | 8858 | 熔断限流控制台   |

***

## 五、请求链路

### 全 Docker 环境

```
浏览器 → Nginx(Docker:80) → Gateway(Docker:8081) → 微服务(Docker)
```

### 本地开发环境

```
浏览器 → Nginx(Docker:80) → Gateway(本地:8081) → 微服务(本地)
```

***

## 六、故障排查

### Nginx 无法连接 Gateway

```bash
# 检查 Gateway 是否启动
curl http://localhost:8081/actuator/health

# 检查 Nginx 配置
docker exec vibe-nginx-local cat /etc/nginx/nginx.conf

# 查看 Nginx 错误日志
docker logs vibe-nginx-local
```

### 端口冲突

```bash
# 如果 80 端口被占用，先停止其他 Nginx
docker stop vibe-nginx

# 再启动本地开发环境
docker-compose -f docker-compose.local.yml up -d nginx
```

### 容器网络问题

```bash
# 重建网络
docker-compose down
docker-compose -f docker-compose.local.yml up -d nginx
```

