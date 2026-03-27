#!/bin/bash

# Nacos Docker 启动脚本

echo "======================================"
echo "  启动 Nacos (Docker)"
echo "======================================"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    echo "请先安装 Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose 未安装"
    echo "请先安装 Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
fi

echo "正在启动 Nacos..."
docker-compose up -d nacos

echo ""
echo "等待 Nacos 启动..."
sleep 10

echo ""
echo "======================================"
echo "Nacos 启动完成！"
echo "======================================"
echo ""
echo "访问地址:"
echo "  Nacos 控制台: http://localhost:8848/nacos"
echo "  默认账号: nacos"
echo "  默认密码: nacos"
echo ""
echo "查看日志:"
echo "  docker logs -f vibe-nacos"
echo ""
echo "停止 Nacos:"
echo "  docker-compose stop nacos"
echo ""
