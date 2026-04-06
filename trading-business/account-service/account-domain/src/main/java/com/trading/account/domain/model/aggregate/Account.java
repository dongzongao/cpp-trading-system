package com.trading.account.domain.model.aggregate;

import com.trading.account.domain.model.valueobject.*;
import com.trading.user.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 账户聚合根
 */
public class Account {
    
    private AccountId id;
    private UserId userId;
    private AccountType type;
    private Currency currency;
    private Money totalBalance;
    private Money availableBalance;
    private Money frozenBalance;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Account() {
    }
    
    /**
     * 创建账户
     */
    public static Account create(UserId userId, AccountType type, Currency currency) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        
        Account account = new Account();
        account.userId = userId;
        account.type = type;
        account.currency = currency;
        account.totalBalance = Money.zero(currency);
        account.availableBalance = Money.zero(currency);
        account.frozenBalance = Money.zero(currency);
        account.status = AccountStatus.ACTIVE;
        account.createdAt = LocalDateTime.now();
        account.updatedAt = LocalDateTime.now();
        
        return account;
    }
    
    /**
     * 存款
     */
    public void deposit(Money amount) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        this.totalBalance = totalBalance.add(amount);
        this.availableBalance = availableBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 取款
     */
    public void withdraw(Money amount) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Insufficient available balance");
        }
        
        this.totalBalance = totalBalance.subtract(amount);
        this.availableBalance = availableBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 冻结资金
     */
    public void freeze(Money amount, FreezeReason reason) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Insufficient available balance to freeze");
        }
        
        this.availableBalance = availableBalance.subtract(amount);
        this.frozenBalance = frozenBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 解冻资金
     */
    public void unfreeze(Money amount) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        if (frozenBalance.isLessThan(amount)) {
            throw new IllegalStateException("Insufficient frozen balance to unfreeze");
        }
        
        this.frozenBalance = frozenBalance.subtract(amount);
        this.availableBalance = availableBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 扣减冻结资金
     */
    public void deductFrozen(Money amount) {
        validateActive();
        validatePositiveAmount(amount);
        validateCurrency(amount);
        
        if (frozenBalance.isLessThan(amount)) {
            throw new IllegalStateException("Insufficient frozen balance to deduct");
        }
        
        this.frozenBalance = frozenBalance.subtract(amount);
        this.totalBalance = totalBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 暂停账户
     */
    public void suspend() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot suspend a closed account");
        }
        this.status = AccountStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 激活账户
     */
    public void activate() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot activate a closed account");
        }
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 关闭账户
     */
    public void close() {
        if (!totalBalance.isZero()) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }
        this.status = AccountStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }
    
    // 验证方法
    private void validateActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
    }
    
    private void validatePositiveAmount(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
    
    private void validateCurrency(Money amount) {
        if (amount.getCurrency() != this.currency) {
            throw new IllegalArgumentException("Currency mismatch");
        }
    }
    
    private boolean hasAvailableBalance(Money amount) {
        return availableBalance.isGreaterThanOrEqual(amount);
    }
    
    // 查询方法
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
    
    public Money getTotalBalance() {
        return totalBalance;
    }
    
    public Money getAvailableBalance() {
        return availableBalance;
    }
    
    public Money getFrozenBalance() {
        return frozenBalance;
    }
    
    // Getters
    public AccountId getId() {
        return id;
    }
    
    public void setId(AccountId id) {
        this.id = id;
    }
    
    public UserId getUserId() {
        return userId;
    }
    
    public AccountType getType() {
        return type;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public AccountStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
