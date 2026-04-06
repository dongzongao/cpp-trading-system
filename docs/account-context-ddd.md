# 账户上下文 (Account Context) - DDD 详细设计

## 1. 概述

账户上下文负责管理用户的资金账户、资产持仓、资金流水和账户冻结/解冻等操作。这是交易系统的核心上下文之一，与交易上下文和清算上下文紧密协作。

## 2. 领域模型

### 2.1 聚合根 (Aggregate Root)

#### Account（账户聚合根）

```java
package com.trading.account.domain.model.aggregate;

@Entity
@Table(name = "accounts")
public class Account extends AggregateRoot<AccountId> {
    
    private AccountId accountId;
    private UserId userId;
    private AccountType accountType;
    private Currency currency;
    private Money availableBalance;    // 可用余额
    private Money frozenBalance;       // 冻结余额
    private Money totalBalance;        // 总余额 = 可用 + 冻结
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 工厂方法：创建新账户
    public static Account create(UserId userId, AccountType accountType, Currency currency) {
        Account account = new Account();
        account.accountId = AccountId.generate();
        account.userId = userId;
        account.accountType = accountType;
        account.currency = currency;
        account.availableBalance = Money.zero(currency);
        account.frozenBalance = Money.zero(currency);
        account.totalBalance = Money.zero(currency);
        account.status = AccountStatus.ACTIVE;
        account.createdAt = LocalDateTime.now();
        
        account.registerEvent(new AccountCreatedEvent(account.accountId, userId, accountType));
        
        return account;
    }
    
    // 领域行为：存入资金
    public void deposit(Money amount, TransactionId transactionId, String description) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        this.availableBalance = this.availableBalance.add(amount);
        this.totalBalance = this.totalBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new FundsDepositedEvent(
            this.accountId, transactionId, amount, description
        ));
    }
    
    // 领域行为：提取资金
    public void withdraw(Money amount, TransactionId transactionId, String description) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        if (this.availableBalance.isLessThan(amount)) {
            throw new InsufficientBalanceException(
                "Insufficient balance: available=" + availableBalance + ", required=" + amount
            );
        }
        
        this.availableBalance = this.availableBalance.subtract(amount);
        this.totalBalance = this.totalBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new FundsWithdrawnEvent(
            this.accountId, transactionId, amount, description
        ));
    }
    
    // 领域行为：冻结资金
    public void freeze(Money amount, TransactionId transactionId, FreezeReason reason) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        if (this.availableBalance.isLessThan(amount)) {
            throw new InsufficientBalanceException(
                "Insufficient available balance to freeze"
            );
        }
        
        this.availableBalance = this.availableBalance.subtract(amount);
        this.frozenBalance = this.frozenBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new FundsFrozenEvent(
            this.accountId, transactionId, amount, reason
        ));
    }
    
    // 领域行为：解冻资金
    public void unfreeze(Money amount, TransactionId transactionId) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        if (this.frozenBalance.isLessThan(amount)) {
            throw new InsufficientFrozenBalanceException(
                "Insufficient frozen balance to unfreeze"
            );
        }
        
        this.frozenBalance = this.frozenBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new FundsUnfrozenEvent(
            this.accountId, transactionId, amount
        ));
    }
    
    // 领域行为：扣减冻结资金（用于订单成交）
    public void deductFrozen(Money amount, TransactionId transactionId) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        if (this.frozenBalance.isLessThan(amount)) {
            throw new InsufficientFrozenBalanceException(
                "Insufficient frozen balance to deduct"
            );
        }
        
        this.frozenBalance = this.frozenBalance.subtract(amount);
        this.totalBalance = this.totalBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new FrozenFundsDeductedEvent(
            this.accountId, transactionId, amount
        ));
    }
    
    // 领域行为：冻结账户
    public void suspend(String reason) {
        if (this.status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot suspend a closed account");
        }
        
        this.status = AccountStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new AccountSuspendedEvent(this.accountId, reason));
    }
    
    // 领域行为：激活账户
    public void activate() {
        if (this.status != AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended accounts can be activated");
        }
        
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new AccountActivatedEvent(this.accountId));
    }
    
    // 领域行为：关闭账户
    public void close() {
        if (!this.totalBalance.isZero()) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }
        
        this.status = AccountStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new AccountClosedEvent(this.accountId));
    }
    
    // 验证方法
    private void validateActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active");
        }
    }
    
    private void validatePositiveAmount(Money amount) {
        if (amount.isNegativeOrZero()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
    
    private void validateCurrency(Money amount) {
        if (!amount.getCurrency().equals(this.currency)) {
            throw new CurrencyMismatchException(
                "Currency mismatch: account=" + this.currency + ", amount=" + amount.getCurrency()
            );
        }
    }
    
    // 查询方法
    public boolean hasAvailableBalance(Money amount) {
        return this.availableBalance.isGreaterThanOrEqual(amount);
    }
    
    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }
}
```

### 2.2 实体 (Entity)

#### Position（持仓实体）

```java
package com.trading.account.domain.model.entity;

@Entity
@Table(name = "positions")
public class Position extends BaseEntity<PositionId> {
    
    private PositionId positionId;
    private AccountId accountId;
    private Symbol symbol;
    private Quantity totalQuantity;      // 总持仓
    private Quantity availableQuantity;  // 可用持仓
    private Quantity frozenQuantity;     // 冻结持仓
    private Money averageCost;           // 平均成本
    private LocalDateTime updatedAt;
    
    // 增加持仓
    public void increase(Quantity quantity, Money cost) {
        this.totalQuantity = this.totalQuantity.add(quantity);
        this.availableQuantity = this.availableQuantity.add(quantity);
        
        // 更新平均成本
        Money totalCost = this.averageCost.multiply(this.totalQuantity.getValue())
                                          .add(cost.multiply(quantity.getValue()));
        this.averageCost = totalCost.divide(this.totalQuantity.getValue());
        
        this.updatedAt = LocalDateTime.now();
    }
    
    // 减少持仓
    public void decrease(Quantity quantity) {
        if (this.availableQuantity.isLessThan(quantity)) {
            throw new InsufficientPositionException("Insufficient available position");
        }
        
        this.totalQuantity = this.totalQuantity.subtract(quantity);
        this.availableQuantity = this.availableQuantity.subtract(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    // 冻结持仓
    public void freeze(Quantity quantity) {
        if (this.availableQuantity.isLessThan(quantity)) {
            throw new InsufficientPositionException("Insufficient available position to freeze");
        }
        
        this.availableQuantity = this.availableQuantity.subtract(quantity);
        this.frozenQuantity = this.frozenQuantity.add(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    // 解冻持仓
    public void unfreeze(Quantity quantity) {
        if (this.frozenQuantity.isLessThan(quantity)) {
            throw new InsufficientFrozenPositionException("Insufficient frozen position");
        }
        
        this.frozenQuantity = this.frozenQuantity.subtract(quantity);
        this.availableQuantity = this.availableQuantity.add(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    // 扣减冻结持仓
    public void deductFrozen(Quantity quantity) {
        if (this.frozenQuantity.isLessThan(quantity)) {
            throw new InsufficientFrozenPositionException("Insufficient frozen position to deduct");
        }
        
        this.frozenQuantity = this.frozenQuantity.subtract(quantity);
        this.totalQuantity = this.totalQuantity.subtract(quantity);
        this.updatedAt = LocalDateTime.now();
    }
}
```

#### AccountTransaction（账户流水实体）

```java
package com.trading.account.domain.model.entity;

@Entity
@Table(name = "account_transactions")
public class AccountTransaction extends BaseEntity<TransactionId> {
    
    private TransactionId transactionId;
    private AccountId accountId;
    private TransactionType type;
    private Money amount;
    private Money balanceBefore;
    private Money balanceAfter;
    private String description;
    private String referenceId;
    private LocalDateTime createdAt;
    
    public static AccountTransaction create(
            AccountId accountId,
            TransactionType type,
            Money amount,
            Money balanceBefore,
            Money balanceAfter,
            String description,
            String referenceId) {
        
        AccountTransaction transaction = new AccountTransaction();
        transaction.transactionId = TransactionId.generate();
        transaction.accountId = accountId;
        transaction.type = type;
        transaction.amount = amount;
        transaction.balanceBefore = balanceBefore;
        transaction.balanceAfter = balanceAfter;
        transaction.description = description;
        transaction.referenceId = referenceId;
        transaction.createdAt = LocalDateTime.now();
        
        return transaction;
    }
}
```

### 2.3 值对象 (Value Object)

```java
package com.trading.account.domain.model.valueobject;

@Embeddable
public class AccountId implements ValueObject {
    @Column(name = "account_id")
    private Long value;
    
    public static AccountId of(Long value) {
        return new AccountId(value);
    }
    
    public static AccountId generate() {
        return new AccountId(IdGenerator.nextId());
    }
}

@Embeddable
public class Money implements ValueObject {
    private BigDecimal amount;
    private Currency currency;
    
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }
    
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
    
    public Money divide(BigDecimal divisor) {
        return new Money(this.amount.divide(divisor, RoundingMode.HALF_UP), this.currency);
    }
    
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isNegativeOrZero() {
        return this.amount.compareTo(BigDecimal.ZERO) <= 0;
    }
}

@Embeddable
public class Quantity implements ValueObject {
    private BigDecimal value;
    
    public Quantity add(Quantity other) {
        return new Quantity(this.value.add(other.value));
    }
    
    public Quantity subtract(Quantity other) {
        return new Quantity(this.value.subtract(other.value));
    }
    
    public boolean isLessThan(Quantity other) {
        return this.value.compareTo(other.value) < 0;
    }
}

public enum AccountType {
    CASH,      // 现金账户
    MARGIN     // 保证金账户
}

public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED
}

public enum TransactionType {
    DEPOSIT,           // 存款
    WITHDRAWAL,        // 取款
    FREEZE,            // 冻结
    UNFREEZE,          // 解冻
    DEDUCT_FROZEN,     // 扣减冻结
    TRADE_BUY,         // 买入交易
    TRADE_SELL,        // 卖出交易
    FEE,               // 手续费
    INTEREST,          // 利息
    DIVIDEND           // 分红
}

public enum FreezeReason {
    ORDER_PLACEMENT,   // 下单冻结
    RISK_CONTROL,      // 风控冻结
    WITHDRAWAL,        // 提现冻结
    MANUAL             // 手动冻结
}
```

## 3. 领域服务 (Domain Service)

### AccountBalanceService（账户余额服务）

```java
package com.trading.account.domain.service;

@Service
public class AccountBalanceService {
    
    private final AccountRepository accountRepository;
    
    // 检查账户余额是否充足
    public boolean hasEnoughBalance(AccountId accountId, Money requiredAmount) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        return account.hasAvailableBalance(requiredAmount);
    }
    
    // 计算账户总资产
    public Money calculateTotalAssets(AccountId accountId, Map<Symbol, Money> marketPrices) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        Money totalAssets = account.getTotalBalance();
        
        // 加上持仓市值
        List<Position> positions = positionRepository.findByAccountId(accountId);
        for (Position position : positions) {
            Money marketPrice = marketPrices.get(position.getSymbol());
            if (marketPrice != null) {
                Money positionValue = marketPrice.multiply(position.getTotalQuantity().getValue());
                totalAssets = totalAssets.add(positionValue);
            }
        }
        
        return totalAssets;
    }
}
```

## 4. 仓储接口 (Repository)

```java
package com.trading.account.domain.repository;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(AccountId accountId);
    Optional<Account> findByUserId(UserId userId);
    List<Account> findByUserIdAndCurrency(UserId userId, Currency currency);
    boolean existsByUserId(UserId userId);
}

public interface PositionRepository {
    Position save(Position position);
    Optional<Position> findById(PositionId positionId);
    Optional<Position> findByAccountIdAndSymbol(AccountId accountId, Symbol symbol);
    List<Position> findByAccountId(AccountId accountId);
}

public interface AccountTransactionRepository {
    AccountTransaction save(AccountTransaction transaction);
    List<AccountTransaction> findByAccountId(AccountId accountId, Pageable pageable);
    List<AccountTransaction> findByAccountIdAndDateRange(
        AccountId accountId, LocalDateTime start, LocalDateTime end
    );
}
```

## 5. 领域事件 (Domain Event)

```java
package com.trading.account.domain.event;

public class AccountCreatedEvent extends DomainEvent {
    private final AccountId accountId;
    private final UserId userId;
    private final AccountType accountType;
}

public class FundsDepositedEvent extends DomainEvent {
    private final AccountId accountId;
    private final TransactionId transactionId;
    private final Money amount;
    private final String description;
}

public class FundsWithdrawnEvent extends DomainEvent {
    private final AccountId accountId;
    private final TransactionId transactionId;
    private final Money amount;
    private final String description;
}

public class FundsFrozenEvent extends DomainEvent {
    private final AccountId accountId;
    private final TransactionId transactionId;
    private final Money amount;
    private final FreezeReason reason;
}

public class FundsUnfrozenEvent extends DomainEvent {
    private final AccountId accountId;
    private final TransactionId transactionId;
    private final Money amount;
}

public class PositionIncreasedEvent extends DomainEvent {
    private final AccountId accountId;
    private final Symbol symbol;
    private final Quantity quantity;
    private final Money cost;
}

public class PositionDecreasedEvent extends DomainEvent {
    private final AccountId accountId;
    private final Symbol symbol;
    private final Quantity quantity;
}
```

## 6. 应用层 (Application Layer)

```java
package com.trading.account.application.service;

@Service
@Transactional
public class AccountApplicationService {
    
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final DomainEventPublisher eventPublisher;
    
    // 创建账户
    public AccountDTO createAccount(CreateAccountCommand command) {
        Account account = Account.create(
            UserId.of(command.getUserId()),
            AccountType.valueOf(command.getAccountType()),
            Currency.valueOf(command.getCurrency())
        );
        
        account = accountRepository.save(account);
        eventPublisher.publish(account.getDomainEvents());
        
        return AccountAssembler.toDTO(account);
    }
    
    // 存款
    public void deposit(DepositCommand command) {
        Account account = accountRepository.findById(AccountId.of(command.getAccountId()))
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        Money amount = Money.of(command.getAmount(), Currency.valueOf(command.getCurrency()));
        TransactionId transactionId = TransactionId.generate();
        
        account.deposit(amount, transactionId, command.getDescription());
        accountRepository.save(account);
        
        // 记录流水
        recordTransaction(account, TransactionType.DEPOSIT, amount, transactionId, command.getDescription());
        
        eventPublisher.publish(account.getDomainEvents());
    }
    
    // 提款
    public void withdraw(WithdrawCommand command) {
        Account account = accountRepository.findById(AccountId.of(command.getAccountId()))
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        Money amount = Money.of(command.getAmount(), Currency.valueOf(command.getCurrency()));
        TransactionId transactionId = TransactionId.generate();
        
        account.withdraw(amount, transactionId, command.getDescription());
        accountRepository.save(account);
        
        recordTransaction(account, TransactionType.WITHDRAWAL, amount, transactionId, command.getDescription());
        
        eventPublisher.publish(account.getDomainEvents());
    }
    
    private void recordTransaction(Account account, TransactionType type, Money amount, 
                                   TransactionId transactionId, String description) {
        AccountTransaction transaction = AccountTransaction.create(
            account.getAccountId(),
            type,
            amount,
            account.getTotalBalance().subtract(amount),
            account.getTotalBalance(),
            description,
            transactionId.getValue()
        );
        
        transactionRepository.save(transaction);
    }
}
```

## 7. 项目结构

```
account-service/
├── account-interfaces/
│   ├── rest/
│   │   ├── AccountController.java
│   │   └── PositionController.java
│   └── grpc/
│       └── AccountGrpcService.java
├── account-application/
│   ├── service/
│   │   └── AccountApplicationService.java
│   └── command/
│       ├── CreateAccountCommand.java
│       ├── DepositCommand.java
│       └── WithdrawCommand.java
├── account-domain/
│   ├── model/
│   │   ├── aggregate/
│   │   │   └── Account.java
│   │   ├── entity/
│   │   │   ├── Position.java
│   │   │   └── AccountTransaction.java
│   │   └── valueobject/
│   │       ├── AccountId.java
│   │       ├── Money.java
│   │       └── Quantity.java
│   ├── service/
│   │   └── AccountBalanceService.java
│   ├── repository/
│   │   ├── AccountRepository.java
│   │   └── PositionRepository.java
│   └── event/
│       └── AccountEvents.java
└── account-infrastructure/
    ├── persistence/
    │   ├── AccountRepositoryImpl.java
    │   └── PositionRepositoryImpl.java
    └── messaging/
        └── AccountEventProducer.java
```

## 8. 总结

账户上下文的核心职责：
1. 管理用户资金账户和余额
2. 处理资金的存取、冻结、解冻操作
3. 管理用户的持仓信息
4. 记录账户流水
5. 通过领域事件与其他上下文协作
