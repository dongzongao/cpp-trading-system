# Channel Context - DDD 设计

## 领域概述

Channel Context（通道上下文）负责管理与外部交易所、券商、银行等机构的连接通道，处理外部系统对接和数据同步。

## 核心职责

- 交易通道管理
- 外部系统对接
- 行情数据接入
- 订单路由到外部系统
- 通道状态监控
- 故障切换和容错

## 聚合根（Aggregate Roots）

### 1. TradingChannel（交易通道）
```java
@Entity
@Table(name = "trading_channels")
public class TradingChannel {
    @Id
    private String channelId;
    private String channelName;
    private String channelType;     // EXCHANGE, BROKER, BANK
    private String provider;        // 交易所/券商名称
    private ChannelStatus status;
    private ConnectionConfig config;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime lastConnectedAt;
}
```

### 2. ChannelConnection（通道连接）
```java
@Entity
@Table(name = "channel_connections")
public class ChannelConnection {
    @Id
    private String connectionId;
    private String channelId;
    private String connectionType;  // REST, WEBSOCKET, FIX, TCP
    private String endpoint;
    private ConnectionStatus status;
    private Integer maxRetries;
    private Integer currentRetries;
    private LocalDateTime connectedAt;
    private LocalDateTime lastHeartbeatAt;
}
```

## 实体（Entities）

### 1. ExternalOrder（外部订单）
```java
@Entity
@Table(name = "external_orders")
public class ExternalOrder {
    @Id
    private String externalOrderId;
    private String internalOrderId;
    private String channelId;
    private String externalSystemId;
    private String symbol;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal price;
    private ExternalOrderStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime acknowledgedAt;
}
```

### 2. MarketDataFeed（行情数据源）
```java
@Entity
@Table(name = "market_data_feeds")
public class MarketDataFeed {
    @Id
    private String feedId;
    private String channelId;
    private String symbol;
    private FeedType feedType;      // REALTIME, SNAPSHOT, HISTORICAL
    private FeedStatus status;
    private Integer updateFrequency;
    private LocalDateTime lastUpdateAt;
}
```

### 3. ChannelCredential（通道凭证）
```java
@Entity
@Table(name = "channel_credentials")
public class ChannelCredential {
    @Id
    private String credentialId;
    private String channelId;
    private String apiKey;
    private String apiSecret;
    private String accessToken;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
```

## 值对象（Value Objects）

### 1. ConnectionConfig（连接配置）
```java
public class ConnectionConfig {
    private String host;
    private Integer port;
    private String protocol;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Boolean enableSsl;
    private Map<String, String> headers;
}
```

### 2. ChannelMetrics（通道指标）
```java
public class ChannelMetrics {
    private Long totalOrders;
    private Long successfulOrders;
    private Long failedOrders;
    private Double successRate;
    private Long avgResponseTime;
    private LocalDateTime lastUpdated;
}
```

## 领域服务（Domain Services）

### 1. ChannelConnectionService
- 建立通道连接
- 维持心跳
- 断线重连
- 连接池管理

### 2. OrderRoutingService
- 选择最优通道
- 订单路由
- 负载均衡
- 故障转移

### 3. MarketDataService
- 订阅行情数据
- 数据标准化
- 数据分发
- 延迟监控

## 仓储接口（Repository Interfaces）

```java
public interface TradingChannelRepository {
    TradingChannel save(TradingChannel channel);
    Optional<TradingChannel> findById(String channelId);
    List<TradingChannel> findByStatus(ChannelStatus status);
    List<TradingChannel> findByProvider(String provider);
}

public interface ChannelConnectionRepository {
    ChannelConnection save(ChannelConnection connection);
    Optional<ChannelConnection> findById(String connectionId);
    List<ChannelConnection> findByChannelId(String channelId);
    List<ChannelConnection> findActiveConnections();
}

public interface ExternalOrderRepository {
    ExternalOrder save(ExternalOrder order);
    Optional<ExternalOrder> findById(String externalOrderId);
    Optional<ExternalOrder> findByInternalOrderId(String internalOrderId);
    List<ExternalOrder> findByChannelId(String channelId);
}
```

## 领域事件（Domain Events）

```java
public class ChannelConnectedEvent extends DomainEvent {
    private String channelId;
    private String channelName;
    private LocalDateTime connectedAt;
}

public class ChannelDisconnectedEvent extends DomainEvent {
    private String channelId;
    private String reason;
    private LocalDateTime disconnectedAt;
}

public class OrderRoutedToChannelEvent extends DomainEvent {
    private String orderId;
    private String channelId;
    private String externalOrderId;
}

public class MarketDataReceivedEvent extends DomainEvent {
    private String symbol;
    private String channelId;
    private MarketData data;
}
```

## 应用服务（Application Services）

### 1. ChannelManagementApplicationService
- 创建通道
- 配置通道
- 启用/禁用通道
- 查询通道状态

### 2. OrderRoutingApplicationService
- 路由订单到外部系统
- 查询外部订单状态
- 同步订单状态
- 处理订单回报

### 3. MarketDataApplicationService
- 订阅行情
- 取消订阅
- 查询行情数据
- 管理数据源

## REST API 端点

```
POST   /api/v1/channels                    - 创建通道
GET    /api/v1/channels                    - 查询通道列表
GET    /api/v1/channels/{id}               - 查询通道详情
PUT    /api/v1/channels/{id}               - 更新通道
DELETE /api/v1/channels/{id}               - 删除通道
POST   /api/v1/channels/{id}/connect       - 连接通道
POST   /api/v1/channels/{id}/disconnect    - 断开通道

POST   /api/v1/channels/{id}/orders        - 通过通道下单
GET    /api/v1/channels/{id}/orders        - 查询通道订单
GET    /api/v1/channels/{id}/orders/{orderId} - 查询订单详情

POST   /api/v1/channels/{id}/market-data/subscribe   - 订阅行情
POST   /api/v1/channels/{id}/market-data/unsubscribe - 取消订阅
GET    /api/v1/channels/{id}/market-data/{symbol}    - 查询行情

GET    /api/v1/channels/{id}/metrics       - 查询通道指标
GET    /api/v1/channels/{id}/health        - 健康检查
```

## 集成事件（Integration Events）

### 发布的事件
- ChannelConnectedEvent
- ChannelDisconnectedEvent
- OrderRoutedToChannelEvent
- MarketDataReceivedEvent
- ChannelHealthChangedEvent

### 订阅的事件
- OrderCreatedEvent（来自 Order Service）
- OrderCancelledEvent（来自 Order Service）
- MarketDataSubscriptionEvent（来自其他服务）

## 外部系统集成

### 交易所对接
- Binance API
- Huobi API
- OKEx API
- 上交所/深交所接口

### 券商对接
- 券商柜台系统
- PB 系统
- 托管银行系统

### 协议支持
- REST API
- WebSocket
- FIX Protocol
- TCP/Binary Protocol

## 技术实现

### 连接管理
- Netty（TCP/WebSocket）
- Apache HttpClient（REST）
- QuickFIX/J（FIX Protocol）

### 数据处理
- 消息解析和序列化
- 数据标准化
- 协议适配器模式

### 容错机制
- 连接池
- 断线重连
- 故障转移
- 限流和熔断

## 性能指标

- 连接建立时间: < 1s
- 订单路由延迟: < 50ms
- 行情数据延迟: < 100ms
- 通道可用性: 99.9%
- 并发连接数: > 100
