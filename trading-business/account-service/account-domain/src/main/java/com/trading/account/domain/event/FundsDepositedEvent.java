package com.trading.account.domain.event;

import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.model.valueobject.Money;

import java.time.LocalDateTime;

/**
 * 资金存入事件
 */
public class FundsDepositedEvent {
    
    private final AccountId accountId;
    private final Money amount;
    private final Money balanceAfter;
    private final LocalDateTime occurredAt;
    
    public FundsDepositedEvent(AccountId accountId, Money amount, Money balanceAfter) {
        this.accountId = accountId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.occurredAt = LocalDateTime.now();
    }
    
    public AccountId getAccountId() {
        return accountId;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public Money getBalanceAfter() {
        return balanceAfter;
    }
    
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
