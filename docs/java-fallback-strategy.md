# Java 业务系统兜底方案设计

## 1. 概述

本文档定义了 Java 业务系统的全面兜底方案，包括容错机制、服务降级、熔断保护、事务补偿、数据一致性保障和灾难恢复策略。

## 2. 核心兜底策略

### 2.1 服务间通信兜底

#### 2.1.1 与 C++ 撮合引擎通信兜底

**场景**: C++ 撮合引擎不可用或响应超时

**兜底策略**:

```java
package com.trading.order.infrastructure.fallback;

@Component
public class MatchingEngineFallback {
    
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, OrderMessage> kafkaTemplate;
    
    // 策略 1: 订单缓存到 Redis，等待引擎恢复
    public void cacheOrderForRetry(Order order) {
        String key = "pending_orders:" + order.getOrderId().getValue();
        
        OrderCache cache = OrderCache.builder()
            .orderId(order.getOrderId().getValue())
            .userId(order.getUserId().getValue())
            .symbol(order.getSymbol().getValue())
            .side(order.getSide().name())
            .price(order.getPrice().getValue())
            .quantity(order.getQuantity().getValue())
            .timestamp(System.currentTimeMillis())
            .retryCount(0)
            .build();
        
        // 缓存到 Redis，TTL 1小时
        redisTemplate.opsForValue().set(key, cache, 1, TimeUnit.HOURS);
        
        // 更新订单状态为待重试
        order.markAsPendingRetry("Matching engine unavailable");
        orderRepository.save(order);
        
        log.warn("Order {} cached for retry due to matching engine unavailable", 
                 order.getOrderId());
    }
    
    // 策略 2: 发送到 Kafka 死信队列
    public void sendToDeadLetterQueue(Order order, Exception exception) {
        OrderMessage message = OrderMessage.builder()
            .orderId(order.getOrderId().getValue())
            .payload(order)
            .errorMessage(exception.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();
        
        kafkaTemplate.send("order-dlq", message);
        
        log.error("Order {} sent to DLQ: {}", order.getOrderId(), exception.getMessage());
    }
    
    // 策略 3: 自动重试机制（指数退避）
    @Retryable(
        value = {MatchingEngineException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 30000)
    )
    public void submitOrderWithRetry(Order order) {
        try {
            matchingEngineClient.submitOrder(order);
        } catch (MatchingEngineException e) {
            log.warn("Retry attempt failed for order {}: {}", 
                     order.getOrderId(), e.getMessage());
            throw e;
        }
    }
    
    // 策略 4: 熔断器保护
    @CircuitBreaker(
        name = "matchingEngine",
        fallbackMethod = "submitOrderFallback"
    )
    public void submitOrderWithCircuitBreaker(Order order) {
        matchingEngineClient.submitOrder(order);
    }
    
    public void submitOrderFallback(Order order, Exception e) {
        log.error("Circuit breaker activated for order {}", order.getOrderId());
        cacheOrderForRetry(order);
    }
    
    // 定时任务: 重试缓存的订单
    @Scheduled(fixedRate = 30000)  // 每30秒执行
    public void retryPendingOrders() {
        Set<String> keys = redisTemplate.keys("pending_orders:*");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        for (String key : keys) {
            OrderCache cache = (OrderCache) redisTemplate.opsForValue().get(key);
            
            if (cache == null || cache.getRetryCount() >= 10) {
                redisTemplate.delete(key);
                continue;
            }
            
            try {
                Order order = orderRepository.findById(OrderId.of(cache.getOrderId()))
                    .orElse(null);
                
                if (order != null && order.getStatus() == OrderStatus.PENDING_RETRY) {
                    matchingEngineClient.submitOrder(order);
                    order.accept();
                    orderRepository.save(order);
                    redisTemplate.delete(key);
                    
                    log.info("Successfully retried order {}", order.getOrderId());
                }
            } catch (Exception e) {
                cache.setRetryCount(cache.getRetryCount() + 1);
                redisTemplate.opsForValue().set(key, cache, 1, TimeUnit.HOURS);
                
                log.warn("Retry failed for order {}, attempt {}", 
                         cache.getOrderId(), cache.getRetryCount());
            }
        }
    }
}
```

#### 2.1.2 与 Golang 查询服务通信兜底

**场景**: Golang 查询服务不可用

**兜底策略**:

```java
package com.trading.query.infrastructure.fallback;

@Component
public class QueryServiceFallback {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;
    
    // 策略 1: 使用 Redis 缓存
    @Cacheable(value = "orders", key = "#orderId", unless = "#result == null")
    public OrderDTO getOrder(Long orderId) {
        // 先尝试从 Golang 查询服务获取
        try {
            return queryServiceClient.getOrder(orderId);
        } catch (Exception e) {
            log.warn("Query service unavailable, falling back to database");
            return getOrderFromDatabase(orderId);
        }
    }
    
    // 策略 2: 降级到直接查询数据库
    private OrderDTO getOrderFromDatabase(Long orderId) {
        Order order = orderRepository.findById(OrderId.of(orderId))
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        return OrderAssembler.toDTO(order);
    }
    
    // 策略 3: 返回降级数据
    @CircuitBreaker(
        name = "queryService",
        fallbackMethod = "getOrdersFallback"
    )
    public List<OrderDTO> getOrders(Long userId) {
        return queryServiceClient.getOrders(userId);
    }
    
    public List<OrderDTO> getOrdersFallback(Long userId, Exception e) {
        log.warn("Query service circuit breaker activated, returning limited data");
        
        // 返回最近的订单（限制数量）
        List<Order> orders = orderRepository.findByUserId(UserId.of(userId))
            .stream()
            .limit(10)
            .collect(Collectors.toList());
        
        return orders.stream()
            .map(OrderAssembler::toDTO)
            .collect(Collectors.toList());
    }
}
```

### 2.2 数据库兜底策略

#### 2.2.1 主从切换

```java
package com.trading.infrastructure.datasource;

@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        return new RoutingDataSource();
    }
}

public class RoutingDataSource extends AbstractRoutingDataSource {
    
    private final AtomicBoolean useMaster = new AtomicBoolean(true);
    
    @Override
    protected Object determineCurrentLookupKey() {
        return useMaster.get() ? "master" : "slave";
    }
    
    // 健康检查
    @Scheduled(fixedRate = 5000)
    public void checkMasterHealth() {
        try {
            // 检查主库连接
            Connection conn = getResolvedDataSources().get("master").getConnection();
            conn.close();
            
            if (!useMaster.get()) {
                log.info("Master database recovered, switching back");
                useMaster.set(true);
            }
        } catch (Exception e) {
            if (useMaster.get()) {
                log.error("Master database failed, switching to slave");
                useMaster.set(false);
            }
        }
    }
}
```

#### 2.2.2 数据库连接池兜底

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      # 连接测试
      connection-test-query: SELECT 1
      # 连接泄漏检测
      leak-detection-threshold: 60000
```

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            
            if (rs.next()) {
                return Health.up()
                    .withDetail("database", "Available")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "Unavailable")
                .withException(e)
                .build();
        }
        
        return Health.down().build();
    }
}
```

### 2.3 消息队列兜底策略

#### 2.3.1 Kafka 消息发送兜底

```java
package com.trading.infrastructure.messaging;

@Component
public class KafkaFallbackProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 策略 1: 本地消息表
    @Transactional
    public void sendWithLocalMessage(String topic, Object message) {
        // 1. 保存到本地消息表
        OutboxMessage outboxMessage = OutboxMessage.builder()
            .id(UUID.randomUUID().toString())
            .topic(topic)
            .payload(JsonUtil.toJson(message))
            .status(MessageStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .retryCount(0)
            .build();
        
        outboxMessageRepository.save(outboxMessage);
        
        // 2. 尝试发送
        try {
            kafkaTemplate.send(topic, message).get(5, TimeUnit.SECONDS);
            outboxMessage.setStatus(MessageStatus.SENT);
            outboxMessageRepository.save(outboxMessage);
        } catch (Exception e) {
            log.warn("Failed to send message to Kafka, will retry later: {}", e.getMessage());
        }
    }
    
    // 定时任务: 重试失败的消息
    @Scheduled(fixedRate = 10000)
    public void retryFailedMessages() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository
            .findByStatusAndRetryCountLessThan(MessageStatus.PENDING, 10);
        
        for (OutboxMessage message : pendingMessages) {
            try {
                Object payload = JsonUtil.fromJson(message.getPayload(), Object.class);
                kafkaTemplate.send(message.getTopic(), payload).get(5, TimeUnit.SECONDS);
                
                message.setStatus(MessageStatus.SENT);
                message.setSentAt(LocalDateTime.now());
                outboxMessageRepository.save(message);
                
                log.info("Successfully retried message {}", message.getId());
            } catch (Exception e) {
                message.setRetryCount(message.getRetryCount() + 1);
                message.setLastError(e.getMessage());
                outboxMessageRepository.save(message);
                
                log.warn("Retry failed for message {}, attempt {}", 
                         message.getId(), message.getRetryCount());
            }
        }
    }
    
    // 策略 2: 缓存到 Redis
    public void cacheMessageToRedis(String topic, Object message) {
        String key = "failed_messages:" + topic + ":" + UUID.randomUUID();
        
        MessageCache cache = MessageCache.builder()
            .topic(topic)
            .payload(message)
            .timestamp(System.currentTimeMillis())
            .build();
        
        redisTemplate.opsForValue().set(key, cache, 24, TimeUnit.HOURS);
    }
}
```

#### 2.3.2 Kafka 消费兜底

```java
@Component
public class KafkaFallbackConsumer {
    
    // 策略 1: 死信队列
    @KafkaListener(topics = "order-events")
    public void consumeOrderEvents(ConsumerRecord<String, OrderEvent> record) {
        try {
            processOrderEvent(record.value());
        } catch (Exception e) {
            log.error("Failed to process order event, sending to DLQ", e);
            sendToDeadLetterQueue(record);
        }
    }
    
    private void sendToDeadLetterQueue(ConsumerRecord<String, OrderEvent> record) {
        kafkaTemplate.send("order-events-dlq", record.value());
    }
    
    // 策略 2: 手动提交偏移量
    @KafkaListener(
        topics = "trade-events",
        containerFactory = "manualKafkaListenerContainerFactory"
    )
    public void consumeTradeEvents(
            ConsumerRecord<String, TradeEvent> record,
            Acknowledgment acknowledgment) {
        
        try {
            processTradeEvent(record.value());
            acknowledgment.acknowledge();  // 手动提交
        } catch (Exception e) {
            log.error("Failed to process trade event, will retry", e);
            // 不提交偏移量，下次重新消费
        }
    }
}
```

### 2.4 分布式事务兜底策略

#### 2.4.1 Saga 模式补偿

```java
package com.trading.saga;

@Component
public class OrderSaga {
    
    // 正向操作
    public void placeOrder(PlaceOrderCommand command) {
        SagaContext context = new SagaContext();
        
        try {
            // Step 1: 冻结资金
            Money frozenAmount = freezeFunds(command, context);
            context.addCompensation(() -> unfreezeFunds(frozenAmount, command));
            
            // Step 2: 创建订单
            Order order = createOrder(command, context);
            context.addCompensation(() -> cancelOrder(order));
            
            // Step 3: 提交到撮合引擎
            submitToMatchingEngine(order, context);
            context.addCompensation(() -> cancelFromMatchingEngine(order));
            
            // Step 4: 更新风控
            updateRiskProfile(order, context);
            
            context.complete();
            
        } catch (Exception e) {
            log.error("Saga failed, executing compensations", e);
            context.compensate();
            throw e;
        }
    }
    
    // 补偿操作
    private void unfreezeFunds(Money amount, PlaceOrderCommand command) {
        try {
            accountService.unfreezeFunds(
                AccountId.of(command.getAccountId()),
                amount,
                TransactionId.generate()
            );
            log.info("Compensated: unfroze funds {}", amount);
        } catch (Exception e) {
            log.error("Compensation failed: unfreeze funds", e);
            // 记录到补偿失败表，人工介入
            recordCompensationFailure("unfreeze_funds", command, e);
        }
    }
    
    private void cancelOrder(Order order) {
        try {
            order.cancel("Saga compensation");
            orderRepository.save(order);
            log.info("Compensated: cancelled order {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Compensation failed: cancel order", e);
            recordCompensationFailure("cancel_order", order, e);
        }
    }
}

// Saga 上下文
public class SagaContext {
    private final List<Runnable> compensations = new ArrayList<>();
    private boolean completed = false;
    
    public void addCompensation(Runnable compensation) {
        compensations.add(0, compensation);  // 逆序添加
    }
    
    public void complete() {
        this.completed = true;
    }
    
    public void compensate() {
        if (completed) {
            return;
        }
        
        for (Runnable compensation : compensations) {
            try {
                compensation.run();
            } catch (Exception e) {
                log.error("Compensation execution failed", e);
            }
        }
    }
}
```

#### 2.4.2 TCC 模式

```java
@Component
public class OrderTccService {
    
    // Try 阶段
    @Transactional
    public void tryPlaceOrder(PlaceOrderCommand command) {
        // 1. 预留资金
        accountService.reserveFunds(
            AccountId.of(command.getAccountId()),
            Money.of(command.getAmount(), Currency.USD),
            command.getTransactionId()
        );
        
        // 2. 创建订单（预提交状态）
        Order order = Order.createPending(command);
        orderRepository.save(order);
        
        // 3. 记录 TCC 事务
        TccTransaction tcc = TccTransaction.builder()
            .transactionId(command.getTransactionId())
            .status(TccStatus.TRYING)
            .context(JsonUtil.toJson(command))
            .createdAt(LocalDateTime.now())
            .build();
        
        tccRepository.save(tcc);
    }
    
    // Confirm 阶段
    @Transactional
    public void confirmPlaceOrder(String transactionId) {
        TccTransaction tcc = tccRepository.findById(transactionId)
            .orElseThrow(() -> new TccException("TCC transaction not found"));
        
        if (tcc.getStatus() == TccStatus.CONFIRMED) {
            return;  // 幂等性
        }
        
        PlaceOrderCommand command = JsonUtil.fromJson(tcc.getContext(), PlaceOrderCommand.class);
        
        // 1. 确认冻结资金
        accountService.confirmFreeze(
            AccountId.of(command.getAccountId()),
            transactionId
        );
        
        // 2. 确认订单
        Order order = orderRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        order.confirm();
        orderRepository.save(order);
        
        // 3. 更新 TCC 状态
        tcc.setStatus(TccStatus.CONFIRMED);
        tcc.setConfirmedAt(LocalDateTime.now());
        tccRepository.save(tcc);
    }
    
    // Cancel 阶段
    @Transactional
    public void cancelPlaceOrder(String transactionId) {
        TccTransaction tcc = tccRepository.findById(transactionId)
            .orElseThrow(() -> new TccException("TCC transaction not found"));
        
        if (tcc.getStatus() == TccStatus.CANCELLED) {
            return;  // 幂等性
        }
        
        PlaceOrderCommand command = JsonUtil.fromJson(tcc.getContext(), PlaceOrderCommand.class);
        
        // 1. 释放预留资金
        accountService.releaseReservedFunds(
            AccountId.of(command.getAccountId()),
            transactionId
        );
        
        // 2. 取消订单
        Order order = orderRepository.findByTransactionId(transactionId)
            .orElse(null);
        if (order != null) {
            order.cancel("TCC cancelled");
            orderRepository.save(order);
        }
        
        // 3. 更新 TCC 状态
        tcc.setStatus(TccStatus.CANCELLED);
        tcc.setCancelledAt(LocalDateTime.now());
        tccRepository.save(tcc);
    }
    
    // 定时任务: 处理超时的 TCC 事务
    @Scheduled(fixedRate = 30000)
    public void handleTimeoutTransactions() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(5);
        
        List<TccTransaction> timeoutTxs = tccRepository
            .findByStatusAndCreatedAtBefore(TccStatus.TRYING, timeout);
        
        for (TccTransaction tcc : timeoutTxs) {
            try {
                cancelPlaceOrder(tcc.getTransactionId());
                log.info("Cancelled timeout TCC transaction: {}", tcc.getTransactionId());
            } catch (Exception e) {
                log.error("Failed to cancel timeout TCC transaction", e);
            }
        }
    }
}
```

### 2.5 缓存兜底策略

```java
@Component
public class CacheFallbackService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CaffeineCache localCache;
    
    // 多级缓存
    public <T> T getWithMultiLevelCache(
            String key,
            Class<T> type,
            Supplier<T> dataLoader) {
        
        // Level 1: 本地缓存
        T value = localCache.get(key, type);
        if (value != null) {
            return value;
        }
        
        // Level 2: Redis 缓存
        try {
            value = (T) redisTemplate.opsForValue().get(key);
            if (value != null) {
                localCache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to database", e);
        }
        
        // Level 3: 数据库
        value = dataLoader.get();
        if (value != null) {
            // 异步更新缓存
            CompletableFuture.runAsync(() -> {
                try {
                    redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
                    localCache.put(key, value);
                } catch (Exception e) {
                    log.error("Failed to update cache", e);
                }
            });
        }
        
        return value;
    }
    
    // Redis 降级到本地缓存
    @CircuitBreaker(name = "redis", fallbackMethod = "getFromLocalCache")
    public Object getFromRedis(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    public Object getFromLocalCache(String key, Exception e) {
        log.warn("Redis circuit breaker activated, using local cache");
        return localCache.get(key, Object.class);
    }
}
```

### 2.6 限流降级策略

```java
@Component
public class RateLimiterFallback {
    
    // 基于 Resilience4j 的限流
    @RateLimiter(name = "orderService", fallbackMethod = "placeOrderFallback")
    public OrderDTO placeOrder(PlaceOrderCommand command) {
        return orderService.placeOrder(command);
    }
    
    public OrderDTO placeOrderFallback(PlaceOrderCommand command, Exception e) {
        log.warn("Rate limit exceeded for user {}", command.getUserId());
        throw new RateLimitExceededException("Too many requests, please try again later");
    }
    
    // 基于令牌桶的限流
    private final Map<Long, RateLimiter> userLimiters = new ConcurrentHashMap<>();
    
    public boolean checkUserRateLimit(Long userId) {
        RateLimiter limiter = userLimiters.computeIfAbsent(
            userId,
            k -> RateLimiter.create(10.0)  // 每秒10个请求
        );
        
        return limiter.tryAcquire(100, TimeUnit.MILLISECONDS);
    }
}
```

## 3. 监控和告警

```java
@Component
public class FallbackMonitor {
    
    private final MeterRegistry meterRegistry;
    
    // 记录兜底触发次数
    public void recordFallback(String service, String operation) {
        meterRegistry.counter(
            "fallback.triggered",
            "service", service,
            "operation", operation
        ).increment();
    }
    
    // 记录补偿执行
    public void recordCompensation(String saga, boolean success) {
        meterRegistry.counter(
            "compensation.executed",
            "saga", saga,
            "success", String.valueOf(success)
        ).increment();
    }
    
    // 告警
    @Scheduled(fixedRate = 60000)
    public void checkFallbackRate() {
        double fallbackRate = getFallbackRate();
        
        if (fallbackRate > 0.1) {  // 超过10%
            alertService.sendAlert(
                AlertLevel.CRITICAL,
                "High fallback rate detected: " + fallbackRate
            );
        }
    }
}
```

## 4. 灾难恢复

### 4.1 数据备份策略

```yaml
# 备份配置
backup:
  schedule: "0 0 2 * * ?"  # 每天凌晨2点
  retention-days: 30
  locations:
    - s3://backup-bucket/trading-system/
    - /mnt/backup/trading-system/
```

### 4.2 故障切换

```java
@Component
public class DisasterRecoveryService {
    
    // 主站点故障，切换到备用站点
    public void switchToBackupSite() {
        log.warn("Switching to backup site");
        
        // 1. 更新 DNS
        dnsService.updateRecord("trading.example.com", backupSiteIp);
        
        // 2. 激活备用数据库
        dataSourceManager.activateBackupDatabase();
        
        // 3. 通知所有服务
        notificationService.notifyAllServices("SITE_SWITCHED");
        
        // 4. 发送告警
        alertService.sendAlert(
            AlertLevel.CRITICAL,
            "System switched to backup site"
        );
    }
}
```

## 5. 总结

Java 业务系统的兜底方案涵盖：

1. **服务通信兜底**: 重试、熔断、降级、缓存
2. **数据库兜底**: 主从切换、连接池管理
3. **消息队列兜底**: 本地消息表、死信队列
4. **分布式事务兜底**: Saga 补偿、TCC 模式
5. **缓存兜底**: 多级缓存、降级策略
6. **限流降级**: 令牌桶、熔断器
7. **监控告警**: 指标收集、实时告警
8. **灾难恢复**: 数据备份、故障切换

这些策略确保系统在各种异常情况下都能保持稳定运行。
