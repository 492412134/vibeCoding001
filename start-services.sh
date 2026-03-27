#!/bin/bash

# Spring Cloud 微服务启动脚本

echo "======================================"
echo "  Vibe Microservices 启动脚本"
echo "======================================"
echo ""

# 检查 Nacos 是否运行
echo "检查 Nacos 服务..."
if ! lsof -ti:8848 > /dev/null 2>&1; then
    echo "警告: Nacos 服务未启动 (端口 8848)"
    echo "请先启动 Nacos:"
    echo "  1. 下载 Nacos: https://github.com/alibaba/nacos/releases"
    echo "  2. 启动: sh startup.sh -m standalone"
    echo ""
fi

# 创建日志目录
mkdir -p logs

# 函数：启动服务
start_service() {
    local service_name=$1
    local service_dir=$2
    local port=$3
    
    echo "启动 $service_name (端口: $port)..."
    
    # 检查端口是否被占用
    if lsof -ti:$port > /dev/null 2>&1; then
        echo "  端口 $port 已被占用，尝试停止旧服务..."
        lsof -ti:$port | xargs kill -9 2>/dev/null
        sleep 2
    fi
    
    # 启动服务
    cd $service_dir
    nohup mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=$port" > ../logs/$service_name.log 2>&1 &
    cd ..
    
    echo "  $service_name 启动中，日志: logs/$service_name.log"
}

# 启动 Gateway
echo ""
echo "正在启动服务..."
echo "------------------------------"
start_service "gateway" "vibe-gateway" "8080"

# 启动支付服务
start_service "payment-service" "vibe-payment-service" "10991"

# 启动订单服务
start_service "order-service" "vibe-order-service" "10992"

echo ""
echo "------------------------------"
echo "所有服务启动中..."
echo ""
echo "服务访问地址:"
echo "  Gateway:      http://localhost:8080"
echo "  支付服务:     http://localhost:10991"
echo "  订单服务:     http://localhost:10992"
echo ""
echo "API 接口:"
echo "  创建订单:     POST http://localhost:8080/api/order/create"
echo "  订单支付:     POST http://localhost:8080/api/order/{orderId}/pay"
echo "  查询订单:     GET  http://localhost:8080/api/order/{orderId}"
echo ""
echo "查看日志:"
echo "  tail -f logs/gateway.log"
echo "  tail -f logs/payment-service.log"
echo "  tail -f logs/order-service.log"
echo ""
echo "停止所有服务:"
echo "  ./stop-services.sh"
echo ""
