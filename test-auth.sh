#!/bin/bash

# 认证服务测试脚本
# 使用: ./test-auth.sh

BASE_URL="http://localhost:8081"

echo "========== 认证服务测试 =========="
echo ""

# 1. 测试登录
echo "1. 测试登录"
echo "请求: POST $BASE_URL/auth/login"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}')
echo "响应: $LOGIN_RESPONSE"
echo ""

# 提取 Token
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
echo "提取的 AccessToken: ${ACCESS_TOKEN:0:50}..."
echo ""

# 2. 测试无Token访问支付接口（应该失败）
echo "2. 测试无Token访问支付接口（应该返回401）"
curl -s -X POST "$BASE_URL/api/payment/single" \
  -H "Content-Type: application/json" \
  -d '{"amount":100,"policyCode":123}'
echo ""
echo ""

# 3. 测试带Token访问支付接口（应该成功）
echo "3. 测试带Token访问支付接口（应该成功）"
if [ -n "$ACCESS_TOKEN" ]; then
    curl -s -X POST "$BASE_URL/api/payment/single" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"amount":100,"policyCode":123}'
else
    echo "未获取到Token，跳过此测试"
fi
echo ""
echo ""

# 4. 测试访问规则引擎（公开接口，不需要Token）
echo "4. 测试访问规则引擎（公开接口，不需要Token）"
curl -s "$BASE_URL/api/rule/list"
echo ""
echo ""

# 5. 测试注册新用户
echo "5. 测试注册新用户"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"Test1234","email":"test@example.com"}')
echo "响应: $REGISTER_RESPONSE"
echo ""

echo "========== 测试完成 =========="
