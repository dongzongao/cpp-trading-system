package com.trading.account.domain.model.entity;

import com.trading.account.domain.model.valueobject.*;

import java.time.LocalDateTime;

/**
 * 账户交易流水实体
 */
public class AccountTransaction {
    
    private TransactionId id;
    private AccountId accountId;
    private TransactionType type;
    private Money amount;
    private Money balanceBefore;
    private Money balanceAfter;
    private String description;
    private String referenceId;
    private LocalDateTime createdAt;
    
    private AccountTransaction() {
    }
    
    public static AccountTransaction create(
            AccountId accountId,
            TransactionType type,
            Money amount,
            Money balanceBefore,
            Money balanceAfter,
            String description,
            String referenceId) {
        
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (balanceBefore == null || balanceAfter == null) {
            throw new IllegalArgumentException("Balance cannot be null");
        }
        
        AccountTransaction transaction = new AccountTransaction();
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
    
    // Getters
    public TransactionId getId() {
        return id;
    }
    
    public void setId(TransactionId id) {
        this.id = id;
    }
    
    public AccountId getAccountId() {
        return accountId;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public Money getBalanceBefore() {
        return balanceBefore;
    }
    
    public Money getBalanceAfter() {
        return balanceAfter;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getReferenceId() {
        return referenceId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
