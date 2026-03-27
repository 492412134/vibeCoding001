#!/bin/bash

# Vibe 项目灰度发布功能测试脚本
# 使用方式：./test-gray-release.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 基础配置
GATEWAY_URL="http://localhost:8081"
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInR5cGUiOiJhY2Nlc3MiLCJ1c2VySWQiOjQsInVzZXJuYW1lIjoidGVzdHVzZXIxIiwiaWF0IjoxNzc0NjE1MTU0LCJleHAiOjE3NzQ3MDE1NTR9.-UeQg1i9ypy37TIysRfdPjfAMzLxaQPIy5vIeOC_GOE"

# 打印带颜色的信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查服务是否运行
check_service() {
    local service_name=$1
    local port=$2
    
    if lsof -ti:$port > /dev/null 2>&1; then
        print_success "$service_name 服务正在运行 (端口: $port)"
        return 0
    else
        print_error "$service_name 服务未运行 (端口: $port)"
        return 1
    fi
}

# 前置检查
pre_check() {
    print_info "开始前置检查..."
    
    # 检查 Gateway
    if ! check_service "Gateway" 8081; then
        print_error "请先启动 Gateway 服务"
        exit 1
    fi
    
    # 检查 Nacos
    if ! check_service "Nacos" 8848; then
        print_error "请先启动 Nacos 服务"
        exit 1
    fi
    
    print_success "前置检查通过"
    echo ""
}

# 测试灰度用户ID匹配
test_gray_user_id_match() {
    print_info "测试场景1：灰度用户ID匹配测试"
    
    response=$(curl -s -X POST "${GATEWAY_URL}/api/payment/single" \
        -H "Content-Type: application/json" \
        -H "X-User-Id: 1001" \
        -d '{"amount": 100, "orderId": "gray-test-001"}')
    
    if [ $? -eq 0 ]; then
        print_success "请求发送成功"
        print_info "响应: $response"
    else
        print_error "请求发送失败"
    fi
    echo ""
}

# 测试非灰度用户ID
test_non_gray_user_id() {
    print_info "测试场景2：非灰度用户ID测试"
    
    response=$(curl -s -X POST "${GATEWAY_URL}/api/payment/single" \
        -H "Content-Type: application/json" \
        -H "X-User-Id: 9999" \
        -d '{"amount": 200, "orderId": "gray-test-002"}')
    
    if [ $? -eq 0 ]; then
        print_success "请求发送成功"
        print_info "响应: $response"
    else
        print_error "请求发送失败"
    fi
    echo ""
}

# 测试基于URL参数的灰度
test_gray_param() {
    print_info "测试场景3：基于URL参数的灰度测试"
    
    response=$(curl -s -X POST "${GATEWAY_URL}/api/payment/single?version=v2" \
        -H "Content-Type: application/json" \
        -d '{"amount": 300, "orderId": "gray-test-003"}')
    
    if [ $? -eq 0 ]; then
        print_success "请求发送成功"
        print_info "响应: $response"
    else
        print_error "请求发送失败"
    fi
    echo ""
}

# 测试基于请求头的灰度
test_gray_header() {
    print_info "测试场景4：基于请求头的灰度测试"
    
    response=$(curl -s -X POST "${GATEWAY_URL}/api/payment/single" \
        -H "Content-Type: application/json" \
        -H "X-Gray-Version: v2" \
        -d '{"amount": 400, "orderId": "gray-test-004"}')
    
    if [ $? -eq 0 ]; then
        print_success "请求发送成功"
        print_info "响应: $response"
    else
        print_error "请求发送失败"
    fi
    echo ""
}

# 测试权重分配
test_gray_weight() {
    print_info "测试场景5：权重分配测试 - 连续发送10个请求"
    
    for i in {1..10}; do
        response=$(curl -s -X POST "${GATEWAY_URL}/api/payment/single" \
            -H "Content-Type: application/json" \
            -d "{\"amount\": 500, \"orderId\": \"gray-weight-$(printf "%03d" $i)\"}")
        
        if [ $? -eq 0 ]; then
            print_success "请求 $i 发送成功"
        else
            print_error "请求 $i 发送失败"
        fi
        
        # 添加短暂延迟，避免请求过快
        sleep 0.1
    done
    echo ""
}

# 测试订单服务灰度
test_order_service_gray() {
    print_info "测试场景6：订单服务灰度测试"
    
    response=$(curl -s -X GET "${GATEWAY_URL}/api/order/list" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "X-User-Id: 1001")
    
    if [ $? -eq 0 ]; then
        print_success "请求发送成功"
        print_info "响应: $response"
    else
        print_error "请求发送失败"
    fi
    echo ""
}

# 测试认证服务灰度
test_auth_service_gray() {
    print_info "测试场景7：认证服务灰度测试"
    
    response=$(curl -s -X POST "${GATEWAY_URL}/auth/login" \
        -H "Content-Type: application/json" \
        -H "X-User-Id: 1001" \
        -d '{"username": "testuser1", "password": "Test12341"}')
    
    if [ $? -eq 0 ]; then
        print_success "请求发送成功"
        print_info "响应: $response"
    else
        print_error "请求发送失败"
    fi
    echo ""
}

# 打印测试说明
print_test_instructions() {
    echo "========================================"
    echo "  灰度发布功能测试"
    echo "========================================"
    echo ""
    echo "前置条件："
    echo "1. Nacos 服务已启动 (端口 8848)"
    echo "2. Gateway 服务已启动 (端口 8081)"
    echo "3. 支付服务 v1 版本已启动 (端口 10991)"
    echo "4. 支付服务 v2 版本已启动 (端口 10993)"
    echo "5. 在 Nacos 中已配置灰度规则"
    echo ""
    echo "验证方法："
    echo "1. 查看 Gateway 日志，确认灰度路由日志"
    echo "2. 查看支付服务 v1 和 v2 版本的日志"
    echo "3. 检查 Nacos 服务列表中的实例版本信息"
    echo ""
    echo "========================================"
    echo ""
}

# 主函数
main() {
    print_test_instructions
    
    # 前置检查
    pre_check
    
    # 执行测试
    test_gray_user_id_match
    test_non_gray_user_id
    test_gray_param
    test_gray_header
    test_gray_weight
    test_order_service_gray
    test_auth_service_gray
    
    print_success "所有测试场景执行完成"
    print_info "请查看 Gateway 和各服务实例的日志，验证灰度路由效果"
}

# 执行主函数
main
