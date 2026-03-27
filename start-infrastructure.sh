#!/bin/bash

# 启动基础设施 (仅 Nacos 和 Sentinel)

echo "======================================"
echo "  启动基础设施 (Docker)"
echo "======================================"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose 未安装"
    exit 1
fi

echo "正在启动 Nacos 和 Sentinel..."
docker-compose up -d

echo ""
echo "等待服务启动..."
sleep 10

echo ""
echo "======================================"
echo "基础设施启动完成！"
echo "======================================"
echo ""
echo "服务访问地址:"
echo "  Nacos:        http://localhost:8848/nacos"
echo "                账号: nacos"
echo "                密码: nacos"
echo ""
echo "  Sentinel:     http://localhost:8858"
echo ""
echo "注意: MySQL 和 Redis 使用本地已安装的服务"
echo "  MySQL:        localhost:3306"
echo "  Redis:        localhost:6379"
echo ""
echo "查看日志:"
echo "  docker-compose logs -f"
echo ""
echo "停止基础设施:"
echo "  docker-compose down"
echo ""
