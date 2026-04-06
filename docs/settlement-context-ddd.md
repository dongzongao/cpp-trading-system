# 清算上下文 (Settlement Context) - DDD 详细设计

## 1. 概述

清算上下文负责日终清算、资金结算、持仓结算和对账。该上下文确保交易数据的一致性和准确性，生成清算报告。

## 2. 领域模型

### 2.1 聚合根 (Aggregate Root)

#### SettlementBatch（清算批次聚合根）

```java
package com.trading.settlement.domain.model.aggregate;

@Entity
@Table(name = "settlement_batches")
public class SettlementBatch extends AggregateRoot<SettlementBatchId> {
    
    private SettlementBatchId batchId;
    private LocalDate settlementDate;
    private SettlementType type;
    private SettlementStatus status;
    private Integer totalAccounts;
    private Integer processedAccounts;
    private Integer failedAccounts;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    
    // 工厂方法：创建清算批次
    public static SettlementBatch create(LocalDate settlementDate, SettlementType type) {
        SettlementBatch batch = new SettlementBatch();
        batch.batchId = SettlementBatchId.generate();
        batch.settlementDate = settlementDate;
        batch.type = type;
        batch.status = SettlementStatus.PENDING;
        batch.totalAccounts = 0;
        batch.processedAccounts = 0;
        batch.failedAccounts = 0;
        
        batch.registerEvent(new SettlementBatchCreatedEvent(batch.batchId, settlementDate, type));
        
        return batch;
    }
    
    // 领域行为：开始清算
    public void start(Integer totalAccounts) {
        if (this.status != SettlementStatus.PENDING) {
            throw new IllegalStateException("Settlement batch is not in pending status");
        }
        
        this.status = SettlementStatus.PROCESSING;
        this.totalAccounts = totalAccounts;
        this.startTime = LocalDateTime.now();
        
        registerEvent(new SettlementBatchStartedEvent(this.batchId, totalAccounts));
    }
    
    // 领域行为：记录账户处理成功
    public void recordAccountProcessed() {
        this.processedAccounts++;
        
        if (this.processedAccounts + this.failedAccounts >= this.totalAccounts) {
            this.complete();
        }
    }
    
    // 领域行为：记录账户处理失败
    public void recordAccountFailed() {
        this.failedAccounts++;
        
        if (this.processedAccounts + this.failedAccounts >= this.totalAccounts) {
            this.complete();
        }
    }
    
    // 领域行为：完成清算
    public void complete() {
        if (this.status != SettlementStatus.PROCESSING) {
            throw new IllegalStateException("Settlement batch is not in processing status");
        }
        
        this.status = this.failedAccounts > 0 ? 
            SettlementStatus.COMPLETED_WITH_ERRORS : 
            SettlementStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        
        registerEvent(new SettlementBatchCompletedEvent(
            this.batchId, 
            this.processedAccounts, 
            this.failedAccounts
        ));
    }
    
    // 领域行为：失败
    public void fail(String errorMessage) {
        this.status = SettlementStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        
        registerEvent(new SettlementBatchFailedEvent(this.batchId, errorMessage));
    }
    
    // 查询方法
    public BigDecimal getProgressPercentage() {
        if (this.totalAccounts == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(this.processedAccounts + this.failedAccounts)
            .divide(BigDecimal.valueOf(this.totalAccounts), 2, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
}
```

#### AccountSettlement（账户清算聚合根）

```java
package com.trading.settlement.domain.model.aggregate;

@Entity
@Table(name = "account_settlements")
public class AccountSettlement extends AggregateRoot<AccountSettlementId> {
    
    private AccountSettlementId settlementId;
    private SettlementBatchId batchId;
    private AccountId accountId;
    private UserId userId;
    private LocalDate settlementDate;
    
    // 资金数据
    private Money openingBalance;      // 期初余额
    private Money closingBalance;      // 期末余额
    private Money totalDeposit;        // 总存款
    private Money totalWithdrawal;     // 总取款
    private Money totalTradingProfit;  // 交易盈亏
    private Money totalFee;            // 总手续费
    
    // 持仓数据
    private Integer openingPositions;  // 期初持仓数
    private Integer closingPositions;  // 期末持仓数
    private Money totalBuyAmount;      // 总买入金额
    private Money totalSellAmount;     // 总卖出金额
    
    // 交易统计
    private Integer totalOrders;       // 总订单数
    private Integer totalTrades;       // 总成交数
    
    private SettlementStatus status;
    private String errorMessage;
    private LocalDateTime processedAt;
    
    // 工厂方法：创建账户清算
    public static AccountSettlement create(
            SettlementBatchId batchId,
            AccountId accountId,
            UserId userId,
            LocalDate settlementDate) {
        
        AccountSettlement settlement = new AccountSettlement();
        settlement.settlementId = AccountSettlementId.generate();
        settlement.batchId = batchId;
        settlement.accountId = accountId;
        settlement.userId = userId;
        settlement.settlementDate = settlementDate;
        settlement.status = SettlementStatus.PENDING;
        
        return settlement;
    }
    
    // 领域行为：执行清算
    public void settle(
            Money openingBalance,
            Money closingBalance,
            Money totalDeposit,
            Money totalWithdrawal,
            Money totalTradingProfit,
            Money totalFee,
            Integer openingPositions,
            Integer closingPositions,
            Money totalBuyAmount,
            Money totalSellAmount,
            Integer totalOrders,
            Integer totalTrades) {
        
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.totalDeposit = totalDeposit;
        this.totalWithdrawal = totalWithdrawal;
        this.totalTradingProfit = totalTradingProfit;
        this.totalFee = totalFee;
        this.openingPositions = openingPositions;
        this.closingPositions = closingPositions;
        this.totalBuyAmount = totalBuyAmount;
        this.totalSellAmount = totalSellAmount;
        this.totalOrders = totalOrders;
        this.totalTrades = totalTrades;
        
        // 验证余额一致性
        Money calculatedBalance = openingBalance
            .add(totalDeposit)
            .subtract(totalWithdrawal)
            .add(totalTradingProfit)
            .subtract(totalFee);
        
        if (!calculatedBalance.equals(closingBalance)) {
            this.fail("Balance mismatch: calculated=" + calculatedBalance + 
                     ", actual=" + closingBalance);
            return;
        }
        
        this.status = SettlementStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
        
        registerEvent(new AccountSettlementCompletedEvent(
            this.settlementId, 
            this.accountId, 
            this.settlementDate
        ));
    }
    
    // 领域行为：失败
    public void fail(String errorMessage) {
        this.status = SettlementStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
        
        registerEvent(new AccountSettlementFailedEvent(
            this.settlementId, 
            this.accountId, 
            errorMessage
        ));
    }
}
```

### 2.2 值对象 (Value Object)

```java
package com.trading.settlement.domain.model.valueobject;

public enum SettlementType {
    DAILY,      // 日终清算
    MONTHLY,    // 月度清算
    YEARLY      // 年度清算
}

public enum SettlementStatus {
    PENDING,                  // 待处理
    PROCESSING,               // 处理中
    COMPLETED,                // 已完成
    COMPLETED_WITH_ERRORS,    // 完成但有错误
    FAILED                    // 失败
}

@Embeddable
public class SettlementBatchId implements ValueObject {
    @Column(name = "batch_id")
    private Long value;
    
    public static SettlementBatchId generate() {
        return new SettlementBatchId(IdGenerator.nextId());
    }
}

@Embeddable
public class AccountSettlementId implements ValueObject {
    @Column(name = "settlement_id")
    private Long value;
    
    public static AccountSettlementId generate() {
        return new AccountSettlementId(IdGenerator.nextId());
    }
}
```

## 3. 领域服务 (Domain Service)

### SettlementCalculationService（清算计算服务）

```java
package com.trading.settlement.domain.service;

@Service
public class SettlementCalculationService {
    
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;
    
    // 计算账户清算数据
    public AccountSettlementData calculateAccountSettlement(
            AccountId accountId, 
            LocalDate settlementDate) {
        
        LocalDateTime startOfDay = settlementDate.atStartOfDay();
        LocalDateTime endOfDay = settlementDate.atTime(23, 59, 59);
        
        // 获取期初余额（前一天的期末余额）
        Money openingBalance = getOpeningBalance(accountId, settlementDate);
        
        // 获取期末余额（当前余额）
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        Money closingBalance = account.getTotalBalance();
        
        // 计算当日存取款
        List<AccountTransaction> transactions = transactionRepository
            .findByAccountIdAndDateRange(accountId, startOfDay, endOfDay);
        
        Money totalDeposit = Money.zero(account.getCurrency());
        Money totalWithdrawal = Money.zero(account.getCurrency());
        Money totalFee = Money.zero(account.getCurrency());
        
        for (AccountTransaction tx : transactions) {
            switch (tx.getType()) {
                case DEPOSIT:
                    totalDeposit = totalDeposit.add(tx.getAmount());
                    break;
                case WITHDRAWAL:
                    totalWithdrawal = totalWithdrawal.add(tx.getAmount());
                    break;
                case FEE:
                    totalFee = totalFee.add(tx.getAmount());
                    break;
            }
        }
        
        // 计算交易盈亏
        List<Trade> trades = tradeRepository
            .findByAccountIdAndDateRange(accountId, startOfDay, endOfDay);
        
        Money totalBuyAmount = Money.zero(account.getCurrency());
        Money totalSellAmount = Money.zero(account.getCurrency());
        
        for (Trade trade : trades) {
            Money tradeAmount = trade.getPrice().multiply(trade.getQuantity());
            if (trade.getBuyUserId().equals(account.getUserId())) {
                totalBuyAmount = totalBuyAmount.add(tradeAmount);
            } else {
                totalSellAmount = totalSellAmount.add(tradeAmount);
            }
        }
        
        Money totalTradingProfit = totalSellAmount.subtract(totalBuyAmount);
        
        // 统计订单和成交数
        List<Order> orders = orderRepository
            .findByAccountIdAndDateRange(accountId, startOfDay, endOfDay);
        Integer totalOrders = orders.size();
        Integer totalTrades = trades.size();
        
        // 统计持仓
        Integer openingPositions = getOpeningPositions(accountId, settlementDate);
        Integer closingPositions = getClosingPositions(accountId, settlementDate);
        
        return new AccountSettlementData(
            openingBalance, closingBalance,
            totalDeposit, totalWithdrawal,
            totalTradingProfit, totalFee,
            openingPositions, closingPositions,
            totalBuyAmount, totalSellAmount,
            totalOrders, totalTrades
        );
    }
    
    private Money getOpeningBalance(AccountId accountId, LocalDate date) {
        // 查询前一天的清算记录
        LocalDate previousDate = date.minusDays(1);
        Optional<AccountSettlement> previousSettlement = 
            accountSettlementRepository.findByAccountIdAndDate(accountId, previousDate);
        
        if (previousSettlement.isPresent()) {
            return previousSettlement.get().getClosingBalance();
        }
        
        // 如果没有前一天的记录，返回账户初始余额
        return Money.zero(Currency.USD);
    }
    
    private Integer getOpeningPositions(AccountId accountId, LocalDate date) {
        // 实现逻辑
        return 0;
    }
    
    private Integer getClosingPositions(AccountId accountId, LocalDate date) {
        // 实现逻辑
        return 0;
    }
}
```

## 4. 仓储接口 (Repository)

```java
package com.trading.settlement.domain.repository;

public interface SettlementBatchRepository {
    SettlementBatch save(SettlementBatch batch);
    Optional<SettlementBatch> findById(SettlementBatchId batchId);
    Optional<SettlementBatch> findByDateAndType(LocalDate date, SettlementType type);
    List<SettlementBatch> findByStatus(SettlementStatus status);
}

public interface AccountSettlementRepository {
    AccountSettlement save(AccountSettlement settlement);
    Optional<AccountSettlement> findById(AccountSettlementId settlementId);
    Optional<AccountSettlement> findByAccountIdAndDate(AccountId accountId, LocalDate date);
    List<AccountSettlement> findByBatchId(SettlementBatchId batchId);
    List<AccountSettlement> findByUserId(UserId userId);
}
```

## 5. 领域事件 (Domain Event)

```java
package com.trading.settlement.domain.event;

public class SettlementBatchCreatedEvent extends DomainEvent {
    private final SettlementBatchId batchId;
    private final LocalDate settlementDate;
    private final SettlementType type;
}

public class SettlementBatchStartedEvent extends DomainEvent {
    private final SettlementBatchId batchId;
    private final Integer totalAccounts;
}

public class SettlementBatchCompletedEvent extends DomainEvent {
    private final SettlementBatchId batchId;
    private final Integer processedAccounts;
    private final Integer failedAccounts;
}

public class AccountSettlementCompletedEvent extends DomainEvent {
    private final AccountSettlementId settlementId;
    private final AccountId accountId;
    private final LocalDate settlementDate;
}
```

## 6. 应用层 (Application Layer)

```java
package com.trading.settlement.application.service;

@Service
@Transactional
public class SettlementApplicationService {
    
    private final SettlementBatchRepository batchRepository;
    private final AccountSettlementRepository settlementRepository;
    private final SettlementCalculationService calculationService;
    private final AccountRepository accountRepository;
    private final DomainEventPublisher eventPublisher;
    
    // 执行日终清算
    @Scheduled(cron = "0 0 1 * * *")  // 每天凌晨1点执行
    public void performDailySettlement() {
        LocalDate settlementDate = LocalDate.now().minusDays(1);
        
        // 检查是否已经清算过
        Optional<SettlementBatch> existing = batchRepository
            .findByDateAndType(settlementDate, SettlementType.DAILY);
        
        if (existing.isPresent()) {
            log.info("Settlement already performed for date: {}", settlementDate);
            return;
        }
        
        // 创建清算批次
        SettlementBatch batch = SettlementBatch.create(settlementDate, SettlementType.DAILY);
        batch = batchRepository.save(batch);
        
        // 获取所有活跃账户
        List<Account> accounts = accountRepository.findActiveAccounts();
        
        // 开始清算
        batch.start(accounts.size());
        batchRepository.save(batch);
        
        // 处理每个账户
        for (Account account : accounts) {
            try {
                settleAccount(batch.getBatchId(), account.getAccountId(), settlementDate);
                batch.recordAccountProcessed();
            } catch (Exception e) {
                log.error("Failed to settle account: " + account.getAccountId(), e);
                batch.recordAccountFailed();
            }
            
            batchRepository.save(batch);
        }
        
        eventPublisher.publish(batch.getDomainEvents());
    }
    
    // 清算单个账户
    private void settleAccount(
            SettlementBatchId batchId, 
            AccountId accountId, 
            LocalDate settlementDate) {
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        // 创建账户清算记录
        AccountSettlement settlement = AccountSettlement.create(
            batchId,
            accountId,
            account.getUserId(),
            settlementDate
        );
        
        // 计算清算数据
        AccountSettlementData data = calculationService
            .calculateAccountSettlement(accountId, settlementDate);
        
        // 执行清算
        settlement.settle(
            data.getOpeningBalance(),
            data.getClosingBalance(),
            data.getTotalDeposit(),
            data.getTotalWithdrawal(),
            data.getTotalTradingProfit(),
            data.getTotalFee(),
            data.getOpeningPositions(),
            data.getClosingPositions(),
            data.getTotalBuyAmount(),
            data.getTotalSellAmount(),
            data.getTotalOrders(),
            data.getTotalTrades()
        );
        
        settlementRepository.save(settlement);
        eventPublisher.publish(settlement.getDomainEvents());
    }
    
    // 查询清算报告
    public SettlementReportDTO getSettlementReport(LocalDate date) {
        SettlementBatch batch = batchRepository
            .findByDateAndType(date, SettlementType.DAILY)
            .orElseThrow(() -> new SettlementNotFoundException("Settlement not found"));
        
        List<AccountSettlement> settlements = settlementRepository
            .findByBatchId(batch.getBatchId());
        
        return SettlementReportAssembler.toDTO(batch, settlements);
    }
}
```

## 7. 项目结构

```
settlement-service/
├── settlement-interfaces/
│   ├── rest/
│   │   └── SettlementController.java
│   └── scheduler/
│       └── SettlementScheduler.java
├── settlement-application/
│   ├── service/
│   │   └── SettlementApplicationService.java
│   └── assembler/
│       └── SettlementReportAssembler.java
├── settlement-domain/
│   ├── model/
│   │   ├── aggregate/
│   │   │   ├── SettlementBatch.java
│   │   │   └── AccountSettlement.java
│   │   └── valueobject/
│   │       ├── SettlementType.java
│   │       └── SettlementStatus.java
│   ├── service/
│   │   └── SettlementCalculationService.java
│   ├── repository/
│   │   ├── SettlementBatchRepository.java
│   │   └── AccountSettlementRepository.java
│   └── event/
│       └── SettlementEvents.java
└── settlement-infrastructure/
    ├── persistence/
    │   ├── SettlementBatchRepositoryImpl.java
    │   └── AccountSettlementRepositoryImpl.java
    └── messaging/
        └── SettlementEventProducer.java
```

## 8. 总结

清算上下文的核心职责：
1. 执行日终清算和定期清算
2. 计算账户资金和持仓变动
3. 验证数据一致性
4. 生成清算报告
5. 支持对账和审计
