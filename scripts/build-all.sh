#!/bin/bash

# 构建所有服务

set -e

echo "🔨 Building Trading System..."

# 构建 Java 服务
echo ""
echo "📦 Building Java Business Services..."
cd trading-business
mvn clean install -DskipTests
cd ..
echo "✅ Java services built successfully"

# 构建 C++ 引擎
echo ""
echo "⚙️  Building C++ Trading Engine..."
cd trading-engine
mkdir -p build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make -j$(nproc)
cd ../..
echo "✅ C++ engine built successfully"

# 构建 Golang 查询服务
echo ""
echo "🐹 Building Golang Query Service..."
cd trading-query
go build -o bin/query-service ./cmd/query-service
cd ..
echo "✅ Golang service built successfully"

echo ""
echo "🎉 All services built successfully!"
