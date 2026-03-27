#!/bin/bash
# 一键清理所有服务端口

echo "🔍 检查并清理服务端口..."

# 定义端口
PORTS=(8081 10990 10991 10992 10993 10994)

for PORT in "${PORTS[@]}"; do
    PID=$(lsof -ti :$PORT)
    if [ -n "$PID" ]; then
        echo "  🛑 端口 $PORT 被进程 $PID 占用，正在停止..."
        kill -9 $PID 2>/dev/null
        echo "  ✅ 端口 $PORT 已释放"
    else
        echo "  ✓ 端口 $PORT 空闲"
    fi
done

echo ""
echo "🎉 所有端口清理完成！"
