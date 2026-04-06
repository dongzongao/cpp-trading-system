#!/bin/bash

# 启动本地开发环境
# 包括 PostgreSQL, TimescaleDB, Redis, Kafka, Prometheus, Grafana

set -e

echo "🚀 Starting Trading System Development Environment..."

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# 启动所有服务
echo "📦 Starting all services..."
docker-compose up -d

# 等待服务启动
echo "⏳ Waiting for services to be ready..."
sleep 10

# 检查服务状态
echo "🔍 Checking service status..."

# PostgreSQL
if docker-compose exec -T postgres pg_isready -U trading > /dev/null 2>&1; then
    echo "✅ PostgreSQL is ready"
else
    echo "❌ PostgreSQL is not ready"
fi

# Redis
if docker-compose exec -T redis redis-cli -a trading123 ping > /dev/null 2>&1; then
    echo "✅ Redis is ready"
else
    echo "❌ Redis is not ready"
fi

# Kafka
if docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo "✅ Kafka is ready"
else
    echo "❌ Kafka is not ready (may take a few more seconds)"
fi

echo ""
echo "🎉 Development environment is ready!"
echo ""
echo "📊 Service URLs:"
echo "  - PostgreSQL:    localhost:5432 (user: trading, password: trading123)"
echo "  - TimescaleDB:   localhost:5433 (user: trading, password: trading123)"
echo "  - Redis:         localhost:6379 (password: trading123)"
echo "  - Kafka:         localhost:9092"
echo "  - Prometheus:    http://localhost:9090"
echo "  - Grafana:       http://localhost:3000 (user: admin, password: admin123)"
echo ""
echo "📝 To view logs: docker-compose logs -f [service-name]"
echo "🛑 To stop: ./scripts/stop-dev-env.sh"
