# 多语言分布式交易系统

一个高性能、可扩展的分布式交易系统，采用多语言微服务架构，充分发挥各语言优势。

## 🎯 项目概述

### 系统架构

- **Java 业务系统**: 处理复杂业务逻辑、用户管理、风控和清算
- **C++ 交易引擎**: 提供微秒级延迟的订单撮合和实时清算
- **Golang 查询系统**: 支持高并发查询和报表生成

### 核心特性

- ⚡ **高性能**: C++ 撮合引擎提供 < 100 微秒的撮合延迟
- 🔒 **高可用**: 系统可用性 99.99%，支持故障自动切换
- 📈 **可扩展**: 各子系统支持独立水平扩展
- 🛡️ **安全可靠**: JWT 认证、数据加密、完整审计日志
- 📊 **可观测**: Prometheus 监控、ELK 日志、Jaeger 链路追踪

## 📋 文档导航

### 规范文档

- [需求文档](.kiro/specs/multi-lang-trading-system/requirements.md) - 详细的功能需求和非功能需求
- [设计文档](.kiro/specs/multi-lang-trading-system/design.md) - 完整的技术架构设计
- [业务流程](docs/business-flows.md) - 核心业务流程定义

### 开发指南

- [多语言开发规范](.kiro/skills/multi-language-development.md) - Java、C++、Golang 开发最佳实践

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│         Web 前端 | 移动应用 | 交易终端                       │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                        接入层                                │
│              API 网关 | WebSocket 网关                       │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────┬──────────────────┬──────────────────────┐
│  Java 业务系统    │  C++ 交易引擎     │  Golang 查询系统      │
├──────────────────┼──────────────────┼──────────────────────┤
│ • 用户服务        │ • 订单簿管理      │ • 查询服务            │
│ • 账户服务        │ • 撮合引擎        │ • 报表服务            │
│ • 订单服务        │ • 实时清算        │ • 缓存管理            │
│ • 风控服务        │ • 行情推送        │                      │
│ • 清算服务        │                  │                      │
│ • 通知服务        │                  │                      │
└──────────────────┴──────────────────┴──────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                        数据层                                │
│    PostgreSQL | TimescaleDB | Redis | Kafka                 │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 前置要求

- Docker 20.10+
- Kubernetes 1.28+
- Java 17+
- C++ 编译器（GCC 13+ 或 Clang 15+）
- Go 1.21+

### 本地开发环境

```bash
# 1. 克隆仓库
git clone https://github.com/your-org/trading-system.git
cd trading-system

# 2. 启动基础设施（PostgreSQL、Kafka、Redis）
docker-compose up -d

# 3. 构建 Java 服务
cd trading-business/order-service
mvn clean package
java -jar target/order-service.jar

# 4. 构建 C++ 引擎
cd trading-engine
mkdir build && cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make -j$(nproc)
./trading_engine_server

# 5. 构建 Golang 服务
cd trading-query
go build -o query-service ./cmd/query-service
./query-service
```

### Kubernetes 部署

```bash
# 1. 创建命名空间
kubectl apply -f deployment/kubernetes/namespaces.yaml

# 2. 部署数据层
kubectl apply -f deployment/kubernetes/data/

# 3. 部署业务服务
kubectl apply -f deployment/kubernetes/business/

# 4. 部署交易引擎
kubectl apply -f deployment/kubernetes/engine/

# 5. 部署查询服务
kubectl apply -f deployment/kubernetes/query/

# 6. 验证部署
kubectl get pods -n trading-business
kubectl get pods -n trading-engine
kubectl get pods -n trading-query
```

## 📊 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 撮合延迟 | < 100 微秒 (P99) | C++ 引擎撮合延迟 |
| 订单吞吐 | > 100,000 TPS | 每秒处理订单数 |
| API 响应 | < 100 毫秒 (P95) | Java 服务 API 响应时间 |
| 查询响应 | < 50 毫秒 (P95) | Golang 查询服务响应时间 |
| 查询并发 | > 50,000 QPS | 每秒查询请求数 |
| 系统可用性 | 99.99% | 年度可用性目标 |

## 🔧 技术栈

### 后端服务

| 组件 | 技术 | 版本 |
|------|------|------|
| 业务系统 | Java + Spring Boot | JDK 17.0.9, Spring Boot 3.2.1 |
| 交易引擎 | C++ | C++20, CMake 3.28.1, GCC 13.2.0 |
| 查询系统 | Golang | Go 1.21.6, Gin 1.9.1, GORM 1.25.5 |

### 基础设施

| 组件 | 技术 | 版本 |
|------|------|------|
| 消息队列 | Apache Kafka | 3.6.1 |
| RPC 框架 | gRPC | 1.60.0, Protobuf 25.1 |
| 关系数据库 | PostgreSQL | 16.1 |
| 时序数据库 | TimescaleDB | 2.13.1 |
| 缓存 | Redis | 7.2.4 (Sentinel) |
| 容器 | Docker | 25.0.0 |
| 容器编排 | Kubernetes | 1.29.1, Helm 3.14.0 |
| 服务网格 | Istio | 1.20.2 |

### 监控运维

| 组件 | 技术 | 版本 |
|------|------|------|
| 监控 | Prometheus | 2.48.1 |
| 可视化 | Grafana | 10.2.3 |
| 日志 | Elasticsearch | 8.11.4 |
| 日志处理 | Logstash | 8.11.4 |
| 日志可视化 | Kibana | 8.11.4 |
| 日志采集 | Filebeat | 8.11.4 |
| 链路追踪 | Jaeger | 1.53.0 |
| 告警 | Alertmanager | 0.26.0 |

## 📖 API 文档

### REST API

- 用户管理: `POST /api/v1/users/register`, `POST /api/v1/users/login`
- 账户管理: `GET /api/v1/accounts`, `POST /api/v1/accounts/{id}/deposit`
- 订单管理: `POST /api/v1/orders`, `DELETE /api/v1/orders/{id}`
- 查询服务: `GET /api/v1/orders`, `GET /api/v1/trades`, `GET /api/v1/positions`

### WebSocket API

- 实时行情: `wss://api.trading.com/ws` (订阅 `trades`, `depth`)
- 订单通知: `wss://api.trading.com/ws` (订阅 `orders`)

### gRPC API

详见 `api/proto/` 目录下的 protobuf 定义文件。

## 🧪 测试

### 单元测试

```bash
# Java
cd trading-business/order-service
mvn test

# C++
cd trading-engine/build
ctest --output-on-failure

# Golang
cd trading-query
go test -v ./...
```

### 集成测试

```bash
# 启动测试环境
docker-compose -f docker-compose.test.yml up -d

# 运行集成测试
./scripts/run-integration-tests.sh
```

### 性能测试

```bash
# 使用 JMeter 进行压力测试
jmeter -n -t tests/performance/order-submission.jmx -l results.jtl

# 使用 wrk 测试 API 性能
wrk -t12 -c400 -d30s http://localhost:8080/api/v1/orders
```

## 📈 监控

### Grafana 仪表板

访问 `http://grafana.trading.com` 查看监控仪表板：

- 系统概览：订单量、成交量、延迟、错误率
- Java 服务：JVM 指标、API 性能、数据库连接池
- C++ 引擎：撮合延迟、订单簿深度、内存使用
- Golang 服务：查询性能、缓存命中率、goroutine 数量

### 日志查询

访问 `http://kibana.trading.com` 查询日志：

- 按服务筛选：`service: order-service`
- 按日志级别筛选：`level: ERROR`
- 按用户筛选：`userId: 1001`
- 按时间范围筛选

### 链路追踪

访问 `http://jaeger.trading.com` 查看链路追踪：

- 查看完整请求链路
- 分析性能瓶颈
- 定位错误来源

## 🔐 安全

### 认证授权

- JWT Token 认证
- 多因素认证（MFA）
- 基于角色的访问控制（RBAC）

### 数据安全

- 敏感数据加密存储（AES-256）
- 传输层 TLS 加密
- 数据脱敏
- 完整审计日志

### 风控

- 前置风控规则引擎
- 实时异常交易检测
- 频率限制
- IP 白名单

## 🤝 贡献指南

### 开发流程

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 代码规范

- Java: 遵循 Google Java Style Guide
- C++: 遵循 Google C++ Style Guide
- Golang: 遵循 Effective Go

### 提交规范

使用 Conventional Commits 规范：

- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

## 📝 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 📞 联系方式

- 项目主页: https://github.com/your-org/trading-system
- 问题反馈: https://github.com/your-org/trading-system/issues
- 邮件: support@trading.com

## 🙏 致谢

感谢所有贡献者对本项目的支持！

---

**注意**: 本项目仅供学习和研究使用，不构成任何投资建议。
