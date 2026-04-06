# 风控上下文 (Risk Management Context) - DDD 详细设计

## 1. 概述

风控上下文负责实时监控交易风险，包括用户风险等级评估、订单风险检查、持仓风险监控和风险预警。该上下文在订单提交前进行风控检查，并持续监控账户风险状态。

## 2. 领域模型

### 2.1 聚合根 (Aggregate Root)

#### RiskProfile（风险档案聚合根）

```java
package com.trading.risk.domain.model.aggregate;

@Entity
@Table(name = "risk_profiles")
public class RiskProfile extends AggregateRoot<RiskProfileId> {
    
    private RiskProfileId profileId;
    private UserId userId;
    private AccountId accountId;
    private RiskLevel riskLevel;
    private Money maxDailyLoss;           // 最大日损失限额
    private Money currentDailyLoss;       // 当前日损失
    private Money maxPositionValue;       // 最大持仓市值
    private Money currentPositionValue;   // 当前持仓市值
    private BigDecimal maxLeverage;       // 最大杠杆倍数
    private BigDecimal currentLeverage;   // 当前杠杆倍数
    private Integer maxOrdersPerDay;      // 每日最大订单数
    private Integer currentOrdersToday;   // 今日订单数
    private RiskStatus status;
    private LocalDateTime lastUpdated;
    
    // 工厂方法：创建风险档案
    public static RiskProfile create(UserId userId, AccountId accountId, RiskLevel riskLevel) {
        RiskProfile profile = new RiskProfile();
        profile.profileId = RiskProfileId.generate();
        profile.userId = userId;
        profile.accountId = accountId;
        profile.riskLevel = riskLevel;
        profile.status = RiskStatus.NORMAL;
        profile.currentDailyLoss = Money.zero(Currency.USD);
        profile.currentOrdersToday = 0;
        profile.lastUpdated = LocalDateTime.now();
        
        // 根据风险等级设置限额
        profile.setLimitsByRiskLevel(riskLevel);
        
        profile.registerEvent(new RiskProfileCreatedEvent(profile.profileId, userId, riskLevel));
        
        return profile;
    }
    
    // 领域行为：检查订单风险
    public RiskCheckResult checkOrderRisk(Order order) {
        List<RiskViolation> violations = new ArrayList<>();
        
        // 检查账户状态
        if (this.status == RiskStatus.SUSPENDED) {
            violations.add(new RiskViolation(
                RiskViolationType.ACCOUNT_SUSPENDED,
                "Account is suspended due to risk control"
            ));
        }
        
        // 检查每日订单数限制
        if (this.currentOrdersToday >= this.maxOrdersPerDay) {
            violations.add(new RiskViolation(
                RiskViolationType.MAX_ORDERS_EXCEEDED,
                "Daily order limit exceeded"
            ));
        }
        
        // 检查持仓市值限制
        Money orderValue = order.getPrice().multiply(order.getQuantity());
        if (order.getSide() == OrderSide.BUY) {
            Money projectedValue = this.currentPositionValue.add(orderValue);
            if (projectedValue.isGreaterThan(this.maxPositionValue)) {
                violations.add(new RiskViolation(
                    RiskViolationType.MAX_POSITION_EXCEEDED,
                    "Order would exceed maximum position value"
                ));
            }
        }
        
        // 检查杠杆限制
        BigDecimal projectedLeverage = calculateProjectedLeverage(order);
        if (projectedLeverage.compareTo(this.maxLeverage) > 0) {
            violations.add(new RiskViolation(
                RiskViolationType.MAX_LEVERAGE_EXCEEDED,
                "Order would exceed maximum leverage"
            ));
        }
        
        boolean passed = violations.isEmpty();
        
        if (!passed) {
            registerEvent(new RiskCheckFailedEvent(
                this.profileId, order.getOrderId(), violations
            ));
        }
        
        return new RiskCheckResult(passed, violations);
    }
    
    // 领域行为：更新日损失
    public void updateDailyLoss(Money loss) {
        this.currentDailyLoss = this.currentDailyLoss.add(loss);
        this.lastUpdated = LocalDateTime.now();
        
        // 检查是否超过限额
        if (this.currentDailyLoss.isGreaterThan(this.maxDailyLoss)) {
            this.suspend("Daily loss limit exceeded");
        }
        
        registerEvent(new DailyLossUpdatedEvent(this.profileId, this.currentDailyLoss));
    }
    
    // 领域行为：更新持仓市值
    public void updatePositionValue(Money value) {
        this.currentPositionValue = value;
        this.lastUpdated = LocalDateTime.now();
        
        if (value.isGreaterThan(this.maxPositionValue)) {
            this.warn("Position value exceeds limit");
        }
        
        registerEvent(new PositionValueUpdatedEvent(this.profileId, value));
    }
    
    // 领域行为：增加订单计数
    public void incrementOrderCount() {
        this.currentOrdersToday++;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // 领域行为：重置日计数器
    public void resetDailyCounters() {
        this.currentDailyLoss = Money.zero(this.currentDailyLoss.getCurrency());
        this.currentOrdersToday = 0;
        this.lastUpdated = LocalDateTime.now();
        
        // 如果因日限额被暂停，自动恢复
        if (this.status == RiskStatus.SUSPENDED) {
            this.activate();
        }
    }
    
    // 领域行为：暂停账户
    public void suspend(String reason) {
        if (this.status == RiskStatus.SUSPENDED) {
            return;
        }
        
        this.status = RiskStatus.SUSPENDED;
        this.lastUpdated = LocalDateTime.now();
        
        registerEvent(new RiskProfileSuspendedEvent(this.profileId, reason));
    }
    
    // 领域行为：发出警告
    public void warn(String reason) {
        this.status = RiskStatus.WARNING;
        this.lastUpdated = LocalDateTime.now();
        
        registerEvent(new RiskWarningEvent(this.profileId, reason));
    }
    
    // 领域行为：激活账户
    public void activate() {
        this.status = RiskStatus.NORMAL;
        this.lastUpdated = LocalDateTime.now();
        
        registerEvent(new RiskProfileActivatedEvent(this.profileId));
    }
    
    // 领域行为：调整风险等级
    public void adjustRiskLevel(RiskLevel newLevel) {
        RiskLevel oldLevel = this.riskLevel;
        this.riskLevel = newLevel;
        this.setLimitsByRiskLevel(newLevel);
        this.lastUpdated = LocalDateTime.now();
        
        registerEvent(new RiskLevelAdjustedEvent(this.profileId, oldLevel, newLevel));
    }
    
    private void setLimitsByRiskLevel(RiskLevel level) {
        switch (level) {
            case CONSERVATIVE:
                this.maxDailyLoss = Money.of(new BigDecimal("1000"), Currency.USD);
                this.maxPositionValue = Money.of(new BigDecimal("10000"), Currency.USD);
                this.maxLeverage = new BigDecimal("2");
                this.maxOrdersPerDay = 10;
                break;
            case MODERATE:
                this.maxDailyLoss = Money.of(new BigDecimal("5000"), Currency.USD);
                this.maxPositionValue = Money.of(new BigDecimal("50000"), Currency.USD);
                this.maxLeverage = new BigDecimal("5");
                this.maxOrdersPerDay = 50;
                break;
            case AGGRESSIVE:
                this.maxDailyLoss = Money.of(new BigDecimal("20000"), Currency.USD);
                this.maxPositionValue = Money.of(new BigDecimal("200000"), Currency.USD);
                this.maxLeverage = new BigDecimal("10");
                this.maxOrdersPerDay = 200;
                break;
        }
    }
    
    private BigDecimal calculateProjectedLeverage(Order order) {
        // 简化计算：杠杆 = 持仓市值 / 账户净值
        // 实际实现需要从账户服务获取净值
        return BigDecimal.ZERO; // 占位符
    }
}
```

### 2.2 实体 (Entity)

#### RiskAlert（风险预警实体）

```java
package com.trading.risk.domain.model.entity;

@Entity
@Table(name = "risk_alerts")
public class RiskAlert extends BaseEntity<AlertId> {
    
    private AlertId alertId;
    private RiskProfileId profileId;
    private UserId userId;
    private AlertType alertType;
    private AlertSeverity severity;
    private String message;
    private AlertStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    
    public static RiskAlert create(
            RiskProfileId profileId,
            UserId userId,
            AlertType alertType,
            AlertSeverity severity,
            String message) {
        
        RiskAlert alert = new RiskAlert();
        alert.alertId = AlertId.generate();
        alert.profileId = profileId;
        alert.userId = userId;
        alert.alertType = alertType;
        alert.severity = severity;
        alert.message = message;
        alert.status = AlertStatus.ACTIVE;
        alert.createdAt = LocalDateTime.now();
        
        return alert;
    }
    
    public void acknowledge(String acknowledgedBy) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = acknowledgedBy;
    }
    
    public void resolve() {
        this.status = AlertStatus.RESOLVED;
    }
}
```

### 2.3 值对象 (Value Object)

```java
package com.trading.risk.domain.model.valueobject;

public enum RiskLevel {
    CONSERVATIVE,  // 保守型
    MODERATE,      // 稳健型
    AGGRESSIVE     // 激进型
}

public enum RiskStatus {
    NORMAL,     // 正常
    WARNING,    // 警告
    SUSPENDED   // 暂停
}

public enum RiskViolationType {
    ACCOUNT_SUSPENDED,
    MAX_ORDERS_EXCEEDED,
    MAX_POSITION_EXCEEDED,
    MAX_LEVERAGE_EXCEEDED,
    MAX_DAILY_LOSS_EXCEEDED,
    INSUFFICIENT_MARGIN,
    CONCENTRATION_RISK
}

public enum AlertType {
    DAILY_LOSS_WARNING,
    POSITION_LIMIT_WARNING,
    LEVERAGE_WARNING,
    MARGIN_CALL,
    ACCOUNT_SUSPENDED
}

public enum AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

public enum AlertStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED
}

@Embeddable
public class RiskViolation implements ValueObject {
    private RiskViolationType type;
    private String message;
    
    public RiskViolation(RiskViolationType type, String message) {
        this.type = type;
        this.message = message;
    }
}

public class RiskCheckResult implements ValueObject {
    private boolean passed;
    private List<RiskViolation> violations;
    
    public RiskCheckResult(boolean passed, List<RiskViolation> violations) {
        this.passed = passed;
        this.violations = violations;
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public List<RiskViolation> getViolations() {
        return Collections.unmodifiableList(violations);
    }
}
```

## 3. 领域服务 (Domain Service)

### RiskCalculationService（风险计算服务）

```java
package com.trading.risk.domain.service;

@Service
public class RiskCalculationService {
    
    // 计算账户风险指标
    public RiskMetrics calculateRiskMetrics(
            Account account,
            List<Position> positions,
            Map<Symbol, Money> marketPrices) {
        
        // 计算持仓市值
        Money totalPositionValue = Money.zero(account.getCurrency());
        for (Position position : positions) {
            Money marketPrice = marketPrices.get(position.getSymbol());
            if (marketPrice != null) {
                Money positionValue = marketPrice.multiply(position.getTotalQuantity().getValue());
                totalPositionValue = totalPositionValue.add(positionValue);
            }
        }
        
        // 计算账户净值
        Money netValue = account.getTotalBalance().add(totalPositionValue);
        
        // 计算杠杆倍数
        BigDecimal leverage = BigDecimal.ZERO;
        if (netValue.isPositive()) {
            leverage = totalPositionValue.getAmount().divide(
                netValue.getAmount(), 2, RoundingMode.HALF_UP
            );
        }
        
        // 计算风险度
        BigDecimal riskRatio = BigDecimal.ZERO;
        if (netValue.isPositive()) {
            riskRatio = account.getTotalBalance().getAmount().divide(
                netValue.getAmount(), 4, RoundingMode.HALF_UP
            );
        }
        
        return new RiskMetrics(totalPositionValue, netValue, leverage, riskRatio);
    }
    
    // 计算持仓集中度风险
    public ConcentrationRisk calculateConcentrationRisk(
            List<Position> positions,
            Map<Symbol, Money> marketPrices) {
        
        Money totalValue = Money.zero(Currency.USD);
        Map<Symbol, Money> symbolValues = new HashMap<>();
        
        for (Position position : positions) {
            Money marketPrice = marketPrices.get(position.getSymbol());
            if (marketPrice != null) {
                Money value = marketPrice.multiply(position.getTotalQuantity().getValue());
                symbolValues.put(position.getSymbol(), value);
                totalValue = totalValue.add(value);
            }
        }
        
        // 找出最大持仓占比
        BigDecimal maxConcentration = BigDecimal.ZERO;
        Symbol maxSymbol = null;
        
        for (Map.Entry<Symbol, Money> entry : symbolValues.entrySet()) {
            BigDecimal concentration = entry.getValue().getAmount()
                .divide(totalValue.getAmount(), 4, RoundingMode.HALF_UP);
            
            if (concentration.compareTo(maxConcentration) > 0) {
                maxConcentration = concentration;
                maxSymbol = entry.getKey();
            }
        }
        
        return new ConcentrationRisk(maxSymbol, maxConcentration);
    }
}
```

### RiskMonitoringService（风险监控服务）

```java
package com.trading.risk.domain.service;

@Service
public class RiskMonitoringService {
    
    private final RiskProfileRepository riskProfileRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final RiskCalculationService calculationService;
    
    // 实时监控账户风险
    public void monitorAccountRisk(AccountId accountId) {
        RiskProfile profile = riskProfileRepository.findByAccountId(accountId)
            .orElseThrow(() -> new RiskProfileNotFoundException("Risk profile not found"));
        
        // 获取账户和持仓信息
        Account account = accountService.getAccount(accountId);
        List<Position> positions = accountService.getPositions(accountId);
        Map<Symbol, Money> marketPrices = marketDataService.getCurrentPrices();
        
        // 计算风险指标
        RiskMetrics metrics = calculationService.calculateRiskMetrics(
            account, positions, marketPrices
        );
        
        // 更新风险档案
        profile.updatePositionValue(metrics.getTotalPositionValue());
        
        // 检查风险阈值
        checkRiskThresholds(profile, metrics);
    }
    
    private void checkRiskThresholds(RiskProfile profile, RiskMetrics metrics) {
        // 检查杠杆率
        if (metrics.getLeverage().compareTo(profile.getMaxLeverage()) > 0) {
            createAlert(
                profile,
                AlertType.LEVERAGE_WARNING,
                AlertSeverity.WARNING,
                "Leverage exceeds limit: " + metrics.getLeverage()
            );
        }
        
        // 检查日损失
        if (profile.getCurrentDailyLoss().isGreaterThan(profile.getMaxDailyLoss())) {
            createAlert(
                profile,
                AlertType.DAILY_LOSS_WARNING,
                AlertSeverity.CRITICAL,
                "Daily loss exceeds limit"
            );
            profile.suspend("Daily loss limit exceeded");
        }
    }
    
    private void createAlert(
            RiskProfile profile,
            AlertType type,
            AlertSeverity severity,
            String message) {
        
        RiskAlert alert = RiskAlert.create(
            profile.getProfileId(),
            profile.getUserId(),
            type,
            severity,
            message
        );
        
        riskAlertRepository.save(alert);
    }
}
```

## 4. 仓储接口 (Repository)

```java
package com.trading.risk.domain.repository;

public interface RiskProfileRepository {
    RiskProfile save(RiskProfile profile);
    Optional<RiskProfile> findById(RiskProfileId profileId);
    Optional<RiskProfile> findByUserId(UserId userId);
    Optional<RiskProfile> findByAccountId(AccountId accountId);
    List<RiskProfile> findByStatus(RiskStatus status);
}

public interface RiskAlertRepository {
    RiskAlert save(RiskAlert alert);
    Optional<RiskAlert> findById(AlertId alertId);
    List<RiskAlert> findByUserId(UserId userId);
    List<RiskAlert> findActiveAlerts(UserId userId);
    List<RiskAlert> findBySeverity(AlertSeverity severity);
}
```

## 5. 领域事件 (Domain Event)

```java
package com.trading.risk.domain.event;

public class RiskProfileCreatedEvent extends DomainEvent {
    private final RiskProfileId profileId;
    private final UserId userId;
    private final RiskLevel riskLevel;
}

public class RiskCheckFailedEvent extends DomainEvent {
    private final RiskProfileId profileId;
    private final OrderId orderId;
    private final List<RiskViolation> violations;
}

public class RiskProfileSuspendedEvent extends DomainEvent {
    private final RiskProfileId profileId;
    private final String reason;
}

public class RiskWarningEvent extends DomainEvent {
    private final RiskProfileId profileId;
    private final String reason;
}

public class DailyLossUpdatedEvent extends DomainEvent {
    private final RiskProfileId profileId;
    private final Money currentLoss;
}
```

## 6. 应用层 (Application Layer)

```java
package com.trading.risk.application.service;

@Service
@Transactional
public class RiskApplicationService {
    
    private final RiskProfileRepository riskProfileRepository;
    private final RiskMonitoringService monitoringService;
    private final DomainEventPublisher eventPublisher;
    
    // 创建风险档案
    public RiskProfileDTO createRiskProfile(CreateRiskProfileCommand command) {
        RiskProfile profile = RiskProfile.create(
            UserId.of(command.getUserId()),
            AccountId.of(command.getAccountId()),
            RiskLevel.valueOf(command.getRiskLevel())
        );
        
        profile = riskProfileRepository.save(profile);
        eventPublisher.publish(profile.getDomainEvents());
        
        return RiskProfileAssembler.toDTO(profile);
    }
    
    // 检查订单风险
    public RiskCheckResultDTO checkOrderRisk(CheckOrderRiskCommand command) {
        RiskProfile profile = riskProfileRepository
            .findByAccountId(AccountId.of(command.getAccountId()))
            .orElseThrow(() -> new RiskProfileNotFoundException("Risk profile not found"));
        
        Order order = orderService.getOrder(OrderId.of(command.getOrderId()));
        
        RiskCheckResult result = profile.checkOrderRisk(order);
        
        if (result.isPassed()) {
            profile.incrementOrderCount();
        }
        
        riskProfileRepository.save(profile);
        eventPublisher.publish(profile.getDomainEvents());
        
        return RiskCheckResultAssembler.toDTO(result);
    }
    
    // 定时任务：重置日计数器
    @Scheduled(cron = "0 0 0 * * *")  // 每天午夜执行
    public void resetDailyCounters() {
        List<RiskProfile> profiles = riskProfileRepository.findAll();
        
        for (RiskProfile profile : profiles) {
            profile.resetDailyCounters();
            riskProfileRepository.save(profile);
        }
    }
    
    // 定时任务：监控账户风险
    @Scheduled(fixedRate = 60000)  // 每分钟执行
    public void monitorAllAccounts() {
        List<RiskProfile> profiles = riskProfileRepository.findByStatus(RiskStatus.NORMAL);
        
        for (RiskProfile profile : profiles) {
            try {
                monitoringService.monitorAccountRisk(profile.getAccountId());
            } catch (Exception e) {
                log.error("Failed to monitor account risk", e);
            }
        }
    }
}
```

## 7. 项目结构

```
risk-service/
├── risk-interfaces/
│   ├── rest/
│   │   ├── RiskProfileController.java
│   │   └── RiskAlertController.java
│   └── event/
│       └── RiskEventListener.java
├── risk-application/
│   ├── service/
│   │   └── RiskApplicationService.java
│   ├── command/
│   │   ├── CreateRiskProfileCommand.java
│   │   └── CheckOrderRiskCommand.java
│   └── scheduler/
│       └── RiskMonitoringScheduler.java
├── risk-domain/
│   ├── model/
│   │   ├── aggregate/
│   │   │   └── RiskProfile.java
│   │   ├── entity/
│   │   │   └── RiskAlert.java
│   │   └── valueobject/
│   │       ├── RiskLevel.java
│   │       ├── RiskStatus.java
│   │       └── RiskCheckResult.java
│   ├── service/
│   │   ├── RiskCalculationService.java
│   │   └── RiskMonitoringService.java
│   ├── repository/
│   │   ├── RiskProfileRepository.java
│   │   └── RiskAlertRepository.java
│   └── event/
│       └── RiskEvents.java
└── risk-infrastructure/
    ├── persistence/
    │   ├── RiskProfileRepositoryImpl.java
    │   └── RiskAlertRepositoryImpl.java
    └── messaging/
        └── RiskEventProducer.java
```

## 8. 总结

风控上下文的核心职责：
1. 管理用户风险档案和风险等级
2. 实时检查订单风险
3. 监控账户风险指标
4. 生成风险预警和告警
5. 自动暂停高风险账户
