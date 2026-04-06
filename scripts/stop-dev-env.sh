#!/bin/bash

# 停止本地开发环境

set -e

echo "🛑 Stopping Trading System Development Environment..."

# 停止所有服务
docker-compose down

echo "✅ All services stopped"
echo ""
echo "💡 To remove all data volumes, run: docker-compose down -v"
