# 交易上下文 (Trading Context) - DDD 详细设计

## 1. 概述

交易上下文负责管理订单的生命周期，包括订单创建、提交、撤销、部分成交和完全成交。该上下文与 C++ 撮合引擎通过 gRPC 通信，与账户上下文协作处理资金和持仓。

## 2. 领域模型

### 2.1 聚合根 (Aggregate Root)

#### Order（订单聚合根）

```java
package com.trading.order.domain.model.aggregate;

@Entity
@Table(name = "orders")
public class Order extends AggregateRoot<OrderId> {
    
    private OrderId orderId;
    private UserId userId;
    private AccountId accountId;
    private Symbol symbol;
    private OrderType orderType;
    private OrderSide side;
    private Price price;
    private Quantity quantity;
    private Quantity filledQuantity;
    private Quantity remainingQuantity;
    private OrderStatus status;
    private TimeInForce timeInForce;
    private Money frozenAmount;        // 冻结金额（买单）或冻结数量对应金额（卖单）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiredAt;
    
    // 工厂方法：创建新订单
    public static Order create(
            UserId userId,
            AccountId accountId,
            Symbol symbol,
            OrderType orderType,
            OrderSide side,
            Price price,
            Quantity quantity,
            TimeInForce timeInForce) {
        
        Order order = new Order();
        order.orderId = OrderId.generate();
        order.userId = userId;
        order.accountId = accountId;
        order.symbol = symbol;
        order.orderType = orderType;
        order.side = side;
        order.price = price;
        order.quantity = quantity;
        order.filledQuantity = Quantity.zero();
        order.remainingQuantity = quantity;
        order.status = OrderStatus.PENDING;
        order.timeInForce = timeInForce;
        order.createdAt = LocalDateTime.now();
        
        // 计算需要冻结的金额
        if (side == OrderSide.BUY) {
            order.frozenAmount = price.multiply(quantity);
        }
        
        order.registerEvent(new OrderCreatedEvent(
            order.orderId, userId, symbol, orderType, side, price, quantity
        ));
        
        return order;
    }
    
    // 领域行为：提交订单到撮合引擎
    public void submit() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be submitted");
        }
        
        this.status = OrderStatus.SUBMITTED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new OrderSubmittedEvent(this.orderId));
    }
    
    // 领域行为：订单被撮合引擎接受
    public void accept() {
        if (this.status != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted orders can be accepted");
        }
        
        this.status = OrderStatus.ACCEPTED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new OrderAcceptedEvent(this.orderId));
    }
    
    // 领域行为：订单被撮合引擎拒绝
    public void reject(String reason) {
        if (this.status != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted orders can be rejected");
        }
        
        this.status = OrderStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new OrderRejectedEvent(this.orderId, reason));
    }
    
    // 领域行为：部分成交
    public void partiallyFill(Quantity filledQty, Price fillPrice, TradeId tradeId) {
        if (this.status != OrderStatus.ACCEPTED && this.status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("Order is not in a fillable state");
        }
        
        if (filledQty.isGreaterThan(this.remainingQuantity)) {
            throw new IllegalArgumentException("Filled quantity exceeds remaining quantity");
        }
        
        this.filledQuantity = this.filledQuantity.add(filledQty);
        this.remainingQuantity = this.remainingQuantity.subtract(filledQty);
        this.status = OrderStatus.PARTIALLY_FILLED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new OrderPartiallyFilledEvent(
            this.orderId, tradeId, filledQty, fillPrice, this.remainingQuantity
        ));
    }
    
    // 领域行为：完全成交
    public void fullyFill(Quantity filledQty, Price fillPrice, TradeId tradeId) {
        if (this.status != OrderStatus.ACCEPTED && this.status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("Order is not in a fillable state");
        }
        
        if (!filledQty.equals(this.remainingQuantity)) {
            throw new IllegalArgumentException("Filled quantity must equal remaining quantity");
        }
        
        this.filledQuantity = this.filledQuantity.add(filledQty);
        this.remainingQuantity = Quantity.zero();
        this.status = OrderStatus.FILLED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new OrderFullyFilledEvent(
            this.orderId, tradeId, filledQty, fillPrice
        ));
    }
    
    // 领域行为：撤销订单
    public void cancel(String reason) {
        if (this.status == OrderStatus.FILLED || 
            this.status == OrderStatus.CANCELLED || 
            this.status == OrderStatus.REJECTED) {
            throw new IllegalStateException("Order cannot be cancelled in current state");
        }
        
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new OrderCancelledEvent(this.orderId, reason, this.remainingQuantity));
    }
    
    // 领域行为：订单过期
    public void expire() {
        if (this.status != OrderStatus.ACCEPTED && this.status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("Only active orders can expire");
        }
        
        this.status = OrderStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new OrderExpiredEvent(this.orderId, this.remainingQuantity));
    }
    
    // 查询方法
    public boolean isActive() {
        return this.status == OrderStatus.ACCEPTED || 
               this.status == OrderStatus.PARTIALLY_FILLED;
    }
    
    public boolean isFinal() {
        return this.status == OrderStatus.FILLED || 
               this.status == OrderStatus.CANCELLED || 
               this.status == OrderStatus.REJECTED ||
               this.status == OrderStatus.EXPIRED;
    }
    
    public Money calculateRequiredFunds() {
        if (this.side == OrderSide.BUY) {
            return this.price.multiply(this.quantity);
        }
        return Money.zero(this.price.getCurrency());
    }
}
```

### 2.2 实体 (Entity)

#### Trade（成交实体）

```java
package com.trading.order.domain.model.entity;

@Entity
@Table(name = "trades")
public class Trade extends BaseEntity<TradeId> {
    
    private TradeId tradeId;
    private OrderId buyOrderId;
    private OrderId sellOrderId;
    private UserId buyUserId;
    private UserId sellUserId;
    private Symbol symbol;
    private Price price;
    private Quantity quantity;
    private Money amount;
    private Money buyerFee;
    private Money sellerFee;
    private LocalDateTime tradeTime;
    
    public static Trade create(
            OrderId buyOrderId,
            OrderId sellOrderId,
            UserId buyUserId,
            UserId sellUserId,
            Symbol symbol,
            Price price,
            Quantity quantity,
            Money buyerFee,
            Money sellerFee) {
        
        Trade trade = new Trade();
        trade.tradeId = TradeId.generate();
        trade.buyOrderId = buyOrderId;
        trade.sellOrderId = sellOrderId;
        trade.buyUserId = buyUserId;
        trade.sellUserId = sellUserId;
        trade.symbol = symbol;
        trade.price = price;
        trade.quantity = quantity;
        trade.amount = price.multiply(quantity);
        trade.buyerFee = buyerFee;
        trade.sellerFee = sellerFee;
        trade.tradeTime = LocalDateTime.now();
        
        return trade;
    }
}
```

### 2.3 值对象 (Value Object)

```java
package com.trading.order.domain.model.valueobject;

@Embeddable
public class OrderId implements ValueObject {
    @Column(name = "order_id")
    private Long value;
    
    public static OrderId of(Long value) {
        return new OrderId(value);
    }
    
    public static OrderId generate() {
        return new OrderId(IdGenerator.nextId());
    }
}

@Embeddable
public class TradeId implements ValueObject {
    @Column(name = "trade_id")
    private Long value;
    
    public static TradeId generate() {
        return new TradeId(IdGenerator.nextId());
    }
}

@Embeddable
public class Symbol implements ValueObject {
    @Column(name = "symbol")
    private String value;
    
    private Symbol(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
        this.value = value.toUpperCase();
    }
    
    public static Symbol of(String value) {
        return new Symbol(value);
    }
}

@Embeddable
public class Price implements ValueObject {
    private BigDecimal value;
    private Currency currency;
    
    public Money multiply(Quantity quantity) {
        return Money.of(this.value.multiply(quantity.getValue()), this.currency);
    }
    
    public boolean isPositive() {
        return this.value.compareTo(BigDecimal.ZERO) > 0;
    }
}

public enum OrderType {
    LIMIT,      // 限价单
    MARKET,     // 市价单
    STOP,       // 止损单
    STOP_LIMIT  // 止损限价单
}

public enum OrderSide {
    BUY,   // 买入
    SELL   // 卖出
}

public enum OrderStatus {
    PENDING,           // 待提交
    SUBMITTED,         // 已提交
    ACCEPTED,          // 已接受
    REJECTED,          // 已拒绝
    PARTIALLY_FILLED,  // 部分成交
    FILLED,            // 完全成交
    CANCELLED,         // 已撤销
    EXPIRED            // 已过期
}

public enum TimeInForce {
    GTC,  // Good Till Cancel - 一直有效直到撤销
    IOC,  // Immediate Or Cancel - 立即成交否则撤销
    FOK,  // Fill Or Kill - 全部成交否则撤销
    DAY   // Day - 当日有效
}
```

## 3. 领域服务 (Domain Service)

### OrderValidationService（订单验证服务）

```java
package com.trading.order.domain.service;

@Service
public class OrderValidationService {
    
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final RiskControlService riskControlService;
    
    // 验证订单
    public void validate(Order order) {
        // 验证账户状态
        validateAccount(order.getAccountId());
        
        // 验证订单参数
        validateOrderParameters(order);
        
        // 验证资金或持仓
        if (order.getSide() == OrderSide.BUY) {
            validateBuyOrderFunds(order);
        } else {
            validateSellOrderPosition(order);
        }
        
        // 风控检查
        riskControlService.checkOrderRisk(order);
    }
    
    private void validateAccount(AccountId accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        if (!account.isActive()) {
            throw new AccountNotActiveException("Account is not active");
        }
    }
    
    private void validateOrderParameters(Order order) {
        if (!order.getPrice().isPositive()) {
            throw new InvalidOrderException("Price must be positive");
        }
        
        if (!order.getQuantity().isPositive()) {
            throw new InvalidOrderException("Quantity must be positive");
        }
    }
    
    private void validateBuyOrderFunds(Order order) {
        Money requiredFunds = order.calculateRequiredFunds();
        
        Account account = accountRepository.findById(order.getAccountId())
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        if (!account.hasAvailableBalance(requiredFunds)) {
            throw new InsufficientBalanceException("Insufficient balance for buy order");
        }
    }
    
    private void validateSellOrderPosition(Order order) {
        Position position = positionRepository
            .findByAccountIdAndSymbol(order.getAccountId(), order.getSymbol())
            .orElseThrow(() -> new InsufficientPositionException("No position found"));
        
        if (!position.hasAvailableQuantity(order.getQuantity())) {
            throw new InsufficientPositionException("Insufficient position for sell order");
        }
    }
}
```

### OrderMatchingService（订单撮合服务 - 与 C++ 引擎交互）

```java
package com.trading.order.domain.service;

@Service
public class OrderMatchingService {
    
    private final MatchingEngineGrpcClient matchingEngineClient;
    
    // 提交订单到撮合引擎
    public void submitToMatchingEngine(Order order) {
        MatchingEngineRequest request = MatchingEngineRequest.newBuilder()
            .setOrderId(order.getOrderId().getValue())
            .setSymbol(order.getSymbol().getValue())
            .setSide(order.getSide().name())
            .setPrice(order.getPrice().getValue().doubleValue())
            .setQuantity(order.getQuantity().getValue().doubleValue())
            .setOrderType(order.getOrderType().name())
            .build();
        
        try {
            MatchingEngineResponse response = matchingEngineClient.submitOrder(request);
            
            if (!response.getSuccess()) {
                throw new OrderSubmissionException(response.getErrorMessage());
            }
        } catch (Exception e) {
            throw new MatchingEngineException("Failed to submit order to matching engine", e);
        }
    }
    
    // 撤销订单
    public void cancelOrder(OrderId orderId) {
        CancelOrderRequest request = CancelOrderRequest.newBuilder()
            .setOrderId(orderId.getValue())
            .build();
        
        try {
            CancelOrderResponse response = matchingEngineClient.cancelOrder(request);
            
            if (!response.getSuccess()) {
                throw new OrderCancellationException(response.getErrorMessage());
            }
        } catch (Exception e) {
            throw new MatchingEngineException("Failed to cancel order", e);
        }
    }
}
```

## 4. 仓储接口 (Repository)

```java
package com.trading.order.domain.repository;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId orderId);
    List<Order> findByUserId(UserId userId);
    List<Order> findByAccountId(AccountId accountId);
    List<Order> findActiveOrders(UserId userId);
    List<Order> findByStatus(OrderStatus status);
    Page<Order> findByUserIdAndDateRange(
        UserId userId, LocalDateTime start, LocalDateTime end, Pageable pageable
    );
}

public interface TradeRepository {
    Trade save(Trade trade);
    Optional<Trade> findById(TradeId tradeId);
    List<Trade> findByOrderId(OrderId orderId);
    List<Trade> findByUserId(UserId userId);
    Page<Trade> findByUserIdAndDateRange(
        UserId userId, LocalDateTime start, LocalDateTime end, Pageable pageable
    );
}
```

## 5. 领域事件 (Domain Event)

```java
package com.trading.order.domain.event;

public class OrderCreatedEvent extends DomainEvent {
    private final OrderId orderId;
    private final UserId userId;
    private final Symbol symbol;
    private final OrderType orderType;
    private final OrderSide side;
    private final Price price;
    private final Quantity quantity;
}

public class OrderSubmittedEvent extends DomainEvent {
    private final OrderId orderId;
}

public class OrderAcceptedEvent extends DomainEvent {
    private final OrderId orderId;
}

public class OrderRejectedEvent extends DomainEvent {
    private final OrderId orderId;
    private final String reason;
}

public class OrderPartiallyFilledEvent extends DomainEvent {
    private final OrderId orderId;
    private final TradeId tradeId;
    private final Quantity filledQuantity;
    private final Price fillPrice;
    private final Quantity remainingQuantity;
}

public class OrderFullyFilledEvent extends DomainEvent {
    private final OrderId orderId;
    private final TradeId tradeId;
    private final Quantity filledQuantity;
    private final Price fillPrice;
}

public class OrderCancelledEvent extends DomainEvent {
    private final OrderId orderId;
    private final String reason;
    private final Quantity remainingQuantity;
}

public class TradeExecutedEvent extends DomainEvent {
    private final TradeId tradeId;
    private final OrderId buyOrderId;
    private final OrderId sellOrderId;
    private final Symbol symbol;
    private final Price price;
    private final Quantity quantity;
}
```

## 6. 应用层 (Application Layer)

```java
package com.trading.order.application.service;

@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final OrderValidationService validationService;
    private final OrderMatchingService matchingService;
    private final AccountApplicationService accountService;
    private final DomainEventPublisher eventPublisher;
    
    // 创建并提交订单
    public OrderDTO placeOrder(PlaceOrderCommand command) {
        // 创建订单
        Order order = Order.create(
            UserId.of(command.getUserId()),
            AccountId.of(command.getAccountId()),
            Symbol.of(command.getSymbol()),
            OrderType.valueOf(command.getOrderType()),
            OrderSide.valueOf(command.getSide()),
            Price.of(command.getPrice(), Currency.valueOf(command.getCurrency())),
            Quantity.of(command.getQuantity()),
            TimeInForce.valueOf(command.getTimeInForce())
        );
        
        // 验证订单
        validationService.validate(order);
        
        // 冻结资金或持仓
        if (order.getSide() == OrderSide.BUY) {
            accountService.freezeFunds(
                order.getAccountId(),
                order.calculateRequiredFunds(),
                order.getOrderId(),
                FreezeReason.ORDER_PLACEMENT
            );
        } else {
            accountService.freezePosition(
                order.getAccountId(),
                order.getSymbol(),
                order.getQuantity(),
                order.getOrderId()
            );
        }
        
        // 保存订单
        order = orderRepository.save(order);
        
        // 提交到撮合引擎
        try {
            order.submit();
            matchingService.submitToMatchingEngine(order);
            order.accept();
        } catch (Exception e) {
            order.reject(e.getMessage());
            // 解冻资金或持仓
            rollbackFreeze(order);
        }
        
        orderRepository.save(order);
        eventPublisher.publish(order.getDomainEvents());
        
        return OrderAssembler.toDTO(order);
    }
    
    // 撤销订单
    public void cancelOrder(CancelOrderCommand command) {
        Order order = orderRepository.findById(OrderId.of(command.getOrderId()))
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        // 从撮合引擎撤销
        matchingService.cancelOrder(order.getOrderId());
        
        // 更新订单状态
        order.cancel(command.getReason());
        orderRepository.save(order);
        
        // 解冻剩余资金或持仓
        if (order.getSide() == OrderSide.BUY) {
            Money unfreezeAmount = order.getPrice().multiply(order.getRemainingQuantity());
            accountService.unfreezeFunds(order.getAccountId(), unfreezeAmount, order.getOrderId());
        } else {
            accountService.unfreezePosition(
                order.getAccountId(),
                order.getSymbol(),
                order.getRemainingQuantity(),
                order.getOrderId()
            );
        }
        
        eventPublisher.publish(order.getDomainEvents());
    }
    
    // 处理成交回报（从 C++ 引擎接收）
    @EventListener
    public void handleTradeExecution(TradeExecutionMessage message) {
        Order order = orderRepository.findById(OrderId.of(message.getOrderId()))
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        Quantity filledQty = Quantity.of(message.getFilledQuantity());
        Price fillPrice = Price.of(message.getFillPrice(), order.getPrice().getCurrency());
        TradeId tradeId = TradeId.of(message.getTradeId());
        
        // 更新订单状态
        if (filledQty.equals(order.getRemainingQuantity())) {
            order.fullyFill(filledQty, fillPrice, tradeId);
        } else {
            order.partiallyFill(filledQty, fillPrice, tradeId);
        }
        
        orderRepository.save(order);
        
        // 更新账户和持仓
        updateAccountAndPosition(order, filledQty, fillPrice);
        
        eventPublisher.publish(order.getDomainEvents());
    }
    
    private void updateAccountAndPosition(Order order, Quantity filledQty, Price fillPrice) {
        Money tradeAmount = fillPrice.multiply(filledQty);
        
        if (order.getSide() == OrderSide.BUY) {
            // 扣减冻结资金
            accountService.deductFrozenFunds(order.getAccountId(), tradeAmount, order.getOrderId());
            // 增加持仓
            accountService.increasePosition(order.getAccountId(), order.getSymbol(), filledQty, tradeAmount);
        } else {
            // 扣减冻结持仓
            accountService.deductFrozenPosition(order.getAccountId(), order.getSymbol(), filledQty);
            // 增加可用资金
            accountService.deposit(order.getAccountId(), tradeAmount, order.getOrderId().toString());
        }
    }
}
```

## 7. 接口层 - gRPC 服务（接收 C++ 引擎回报）

```java
package com.trading.order.interfaces.grpc;

@GrpcService
public class TradeCallbackGrpcService extends TradeCallbackServiceGrpc.TradeCallbackServiceImplBase {
    
    private final OrderApplicationService orderApplicationService;
    
    @Override
    public void onTradeExecuted(TradeExecutionRequest request, 
                               StreamObserver<TradeExecutionResponse> responseObserver) {
        try {
            TradeExecutionMessage message = new TradeExecutionMessage(
                request.getOrderId(),
                request.getTradeId(),
                request.getFilledQuantity(),
                request.getFillPrice()
            );
            
            orderApplicationService.handleTradeExecution(message);
            
            TradeExecutionResponse response = TradeExecutionResponse.newBuilder()
                .setSuccess(true)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
```

## 8. 项目结构

```
order-service/
├── order-interfaces/
│   ├── rest/
│   │   └── OrderController.java
│   ├── grpc/
│   │   ├── OrderGrpcService.java
│   │   └── TradeCallbackGrpcService.java
│   └── event/
│       └── TradeEventListener.java
├── order-application/
│   ├── service/
│   │   └── OrderApplicationService.java
│   ├── command/
│   │   ├── PlaceOrderCommand.java
│   │   └── CancelOrderCommand.java
│   └── assembler/
│       └── OrderAssembler.java
├── order-domain/
│   ├── model/
│   │   ├── aggregate/
│   │   │   └── Order.java
│   │   ├── entity/
│   │   │   └── Trade.java
│   │   └── valueobject/
│   │       ├── OrderId.java
│   │       ├── Symbol.java
│   │       ├── Price.java
│   │       └── OrderStatus.java
│   ├── service/
│   │   ├── OrderValidationService.java
│   │   └── OrderMatchingService.java
│   ├── repository/
│   │   ├── OrderRepository.java
│   │   └── TradeRepository.java
│   └── event/
│       └── OrderEvents.java
└── order-infrastructure/
    ├── persistence/
    │   ├── OrderRepositoryImpl.java
    │   └── TradeRepositoryImpl.java
    ├── grpc/
    │   └── MatchingEngineGrpcClient.java
    └── messaging/
        └── OrderEventProducer.java
```

## 9. 与 C++ 撮合引擎的交互流程

```
Java Order Service          C++ Matching Engine
       |                            |
       |  1. Submit Order (gRPC)    |
       |--------------------------->|
       |                            |
       |  2. Order Accepted         |
       |<---------------------------|
       |                            |
       |                            | 3. Matching...
       |                            |
       |  4. Trade Executed (gRPC)  |
       |<---------------------------|
       |                            |
       |  5. Ack                    |
       |--------------------------->|
```

## 10. 总结

交易上下文的核心职责：
1. 管理订单生命周期
2. 与 C++ 撮合引擎通信
3. 协调账户上下文处理资金和持仓
4. 记录成交信息
5. 通过领域事件通知其他上下文
