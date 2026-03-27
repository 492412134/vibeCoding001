# Nacos 部署文档

## 环境信息

- **Nacos 版本**: 3.2.0 (latest)
- **部署方式**: Docker
- **运行模式**: Standalone (单机模式)

## Docker 启动命令

```bash
docker run -d \
  --name vibe-nacos \
  -p 8080:8080 \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e NACOS_AUTH_TOKEN=TXlTZWNyZXRLZXlGb3JOYWNvc1Rva2VuNDkyNDEyMTM0 \
  -e NACOS_AUTH_IDENTITY_KEY=123456 \
  -e NACOS_AUTH_IDENTITY_VALUE=123456 \
  nacos/nacos-server
```

## 端口说明

| 端口 | 用途 | 说明 |
|------|------|------|
| 8080 | 控制台端口 | Nacos 3.x 控制台默认端口 |
| 8848 | HTTP API 端口 | 兼容 Nacos 2.x 的 API 端口 |
| 9848 | gRPC 端口 | 客户端长连接通信端口 |

## 环境变量说明

| 变量名 | 值 | 说明 |
|--------|-----|------|
| MODE | standalone | 单机模式运行 |
| NACOS_AUTH_TOKEN | TXlTZWNyZXRLZXlGb3JOYWNvc1Rva2VuNDkyNDEyMTM0 | JWT Token 密钥 |
| NACOS_AUTH_IDENTITY_KEY | 123456 | 身份验证 Key |
| NACOS_AUTH_IDENTITY_VALUE | 123456 | 身份验证 Value |

## 访问地址

- **控制台**: http://localhost:8080/nacos
- **HTTP API**: http://localhost:8848/nacos
- **默认账号**: nacos
- **默认密码**: nacos

## 常用命令

```bash
# 查看容器状态
docker ps | grep vibe-nacos

# 查看日志
docker logs -f vibe-nacos

# 停止容器
docker stop vibe-nacos

# 启动容器
docker start vibe-nacos

# 重启容器
docker restart vibe-nacos

# 进入容器
docker exec -it vibe-nacos bash

# 删除容器（数据会丢失）
docker rm -f vibe-nacos
```

## 数据持久化（可选）

如需数据持久化，添加卷映射：

```bash
docker run -d \
  --name vibe-nacos \
  -p 8080:8080 \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e NACOS_AUTH_TOKEN=TXlTZWNyZXRLZXlGb3JOYWNvc1Rva2VuNDkyNDEyMTM0 \
  -e NACOS_AUTH_IDENTITY_KEY=123456 \
  -e NACOS_AUTH_IDENTITY_VALUE=123456 \
  -v /path/to/nacos/data:/home/nacos/data \
  -v /path/to/nacos/logs:/home/nacos/logs \
  nacos/nacos-server
```

## 版本差异说明

| 版本 | 控制台端口 | gRPC 端口 | 备注 |
|------|-----------|-----------|------|
| Nacos 2.x | 8848 | 9848 | 控制台和 API 共用 8848 |
| Nacos 3.x | 8080 | 9848 | 控制台独立为 8080，8848 保留为 API 端口 |
