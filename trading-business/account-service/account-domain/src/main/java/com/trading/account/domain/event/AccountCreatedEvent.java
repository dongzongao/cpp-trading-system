package com.trading.account.domain.event;

import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.model.valueobject.AccountType;
import com.trading.account.domain.model.valueobject.Currency;
import com.trading.user.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 账户创建事件
 */
public class AccountCreatedEvent {
    
    private final AccountId accountId;
    private final UserId userId;
    private final AccountType type;
    private final Currency currency;
    private final LocalDateTime occurredAt;
    
    public AccountCreatedEvent(AccountId accountId, UserId userId, AccountType type, Currency currency) {
        this.accountId = accountId;
        this.userId = userId;
        this.type = type;
        this.currency = currency;
        this.occurredAt = LocalDateTime.now();
    }
    
    public AccountId getAccountId() {
        return accountId;
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
    
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
