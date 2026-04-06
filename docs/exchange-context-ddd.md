# Exchange Context - DDD 设计

## 领域概述

Exchange Context（交换机上下文）负责消息路由、转发和协议转换，是系统内部和外部通信的核心枢纽。

## 核心职责

- 消息路由和转发
- 协议转换（REST/gRPC/WebSocket/FIX）
- 消息队列管理
- 消息持久化和重试
- 流量控制和限流

## 聚合根（Aggregate Roots）

### 1. MessageRoute（消息路由）
```java
@Entity
@Table(name = "message_routes")
public class MessageRoute {
    @Id
    private String routeId;
    private String routeName;
    private String sourceType;      // REST, GRPC, WEBSOCKET, FIX
    private String targetType;
    private String routingKey;
    private RouteStatus status;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. MessageQueue（消息队列）
```java
@Entity
@Table(name = "message_queues")
public class MessageQueue {
    @Id
    private String queueId;
    private String queueName;
    private String queueType;       // DIRECT, TOPIC, FANOUT
    private Integer maxSize;
    private Integer currentSize;
    private QueueStatus status;
    private LocalDateTime createdAt;
}
```

## 实体（Entities）

### 1. Message（消息）
```java
@Entity
@Table(name = "messages")
public class Message {
    @Id
    private String messageId;
    private String routeId;
    private String sourceService;
    private String targetService;
    private String messageType;
    private String payload;
    private MessageStatus status;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
```

### 2. ProtocolConverter（协议转换器）
```java
@Entity
@Table(name = "protocol_converters")
public class ProtocolConverter {
    @Id
    private String converterId;
    private String converterName;
    private String sourceProtocol;
    private String targetProtocol;
    private String conversionRules;
    private ConverterStatus status;
}
```

## 值对象（Value Objects）

### 1. RouteConfig（路由配置）
```java
public class RouteConfig {
    private String pattern;
    private String method;
    private Map<String, String> headers;
    private Integer timeout;
    private Integer maxRetries;
}
```

### 2. MessageMetadata（消息元数据）
```java
public class MessageMetadata {
    private String correlationId;
    private String traceId;
    private Map<String, String> headers;
    private LocalDateTime timestamp;
}
```

## 领域服务（Domain Services）

### 1. MessageRoutingService
- 根据路由规则转发消息
- 动态路由选择
- 负载均衡

### 2. ProtocolConversionService
- REST 到 gRPC 转换
- WebSocket 到 Kafka 转换
- FIX 协议处理

### 3. MessageRetryService
- 失败消息重试
- 死信队列处理
- 消息补偿

## 仓储接口（Repository Interfaces）

```java
public interface MessageRouteRepository {
    MessageRoute save(MessageRoute route);
    Optional<MessageRoute> findById(String routeId);
    List<MessageRoute> findBySourceType(String sourceType);
    List<MessageRoute> findActiveRoutes();
}

public interface MessageQueueRepository {
    MessageQueue save(MessageQueue queue);
    Optional<MessageQueue> findById(String queueId);
    List<MessageQueue> findByStatus(QueueStatus status);
}

public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(String messageId);
    List<Message> findPendingMessages();
    List<Message> findFailedMessages();
}
```

## 领域事件（Domain Events）

```java
public class MessageRoutedEvent extends DomainEvent {
    private String messageId;
    private String routeId;
    private String sourceService;
    private String targetService;
}

public class MessageFailedEvent extends DomainEvent {
    private String messageId;
    private String errorMessage;
    private Integer retryCount;
}

public class RouteCreatedEvent extends DomainEvent {
    private String routeId;
    private String routeName;
}
```

## 应用服务（Application Services）

### 1. MessageRoutingApplicationService
- 创建路由规则
- 更新路由配置
- 删除路由规则
- 查询路由信息

### 2. MessageProcessingApplicationService
- 接收消息
- 路由消息
- 转换协议
- 发送消息

### 3. QueueManagementApplicationService
- 创建队列
- 管理队列
- 监控队列状态

## REST API 端点

```
POST   /api/v1/routes              - 创建路由
GET    /api/v1/routes              - 查询路由列表
GET    /api/v1/routes/{id}         - 查询路由详情
PUT    /api/v1/routes/{id}         - 更新路由
DELETE /api/v1/routes/{id}         - 删除路由

POST   /api/v1/messages            - 发送消息
GET    /api/v1/messages/{id}       - 查询消息状态
POST   /api/v1/messages/{id}/retry - 重试消息

GET    /api/v1/queues              - 查询队列列表
GET    /api/v1/queues/{id}         - 查询队列详情
POST   /api/v1/queues              - 创建队列
```

## 集成事件（Integration Events）

### 发布的事件
- MessageRoutedEvent
- MessageFailedEvent
- RouteStatusChangedEvent

### 订阅的事件
- OrderCreatedEvent（来自 Order Service）
- TradeExecutedEvent（来自 Trading Engine）
- AccountUpdatedEvent（来自 Account Service）

## 技术实现

### 消息路由
- Spring Integration
- Apache Camel
- 自定义路由引擎

### 协议转换
- gRPC Transcoding
- WebSocket Gateway
- FIX Protocol Handler

### 消息持久化
- PostgreSQL（路由配置）
- Redis（消息缓存）
- Kafka（消息队列）

## 性能指标

- 消息路由延迟: < 10ms (P99)
- 消息吞吐量: > 50,000 TPS
- 协议转换延迟: < 5ms
- 消息可靠性: 99.99%
