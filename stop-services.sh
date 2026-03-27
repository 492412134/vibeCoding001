#!/bin/bash

# Spring Cloud 微服务停止脚本

echo "======================================"
echo "  Vibe Microservices 停止脚本"
echo "======================================"
echo ""

echo "正在停止服务..."

# 停止 Gateway (端口 8080)
if lsof -ti:8080 > /dev/null 2>&1; then
    echo "  停止 Gateway (端口 8080)..."
    lsof -ti:8080 | xargs kill -9 2>/dev/null
fi

# 停止支付服务 (端口 10991)
if lsof -ti:10991 > /dev/null 2>&1; then
    echo "  停止支付服务 (端口 10991)..."
    lsof -ti:10991 | xargs kill -9 2>/dev/null
fi

# 停止订单服务 (端口 10992)
if lsof -ti:10992 > /dev/null 2>&1; then
    echo "  停止订单服务 (端口 10992)..."
    lsof -ti:10992 | xargs kill -9 2>/dev/null
fi

echo ""
echo "所有服务已停止"
echo ""
