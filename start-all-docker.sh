#!/bin/bash

# 启动所有基础设施 (Docker)

echo "======================================"
echo "  启动所有基础设施 (Docker)"
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

echo "正在启动所有服务..."
docker-compose up -d

echo ""
echo "等待服务启动..."
sleep 15

echo ""
echo "======================================"
echo "所有基础设施启动完成！"
echo "======================================"
echo ""
echo "服务访问地址:"
echo "  Nacos:        http://localhost:8848/nacos (nacos/nacos)"
echo "  MySQL:        localhost:3306 (root/sr123456)"
echo "  Redis:        localhost:6379"
echo "  Sentinel:     http://localhost:8858"
echo ""
echo "查看服务状态:"
echo "  docker-compose ps"
echo ""
echo "查看日志:"
echo "  docker-compose logs -f"
echo ""
echo "停止所有服务:"
echo "  docker-compose down"
echo ""
