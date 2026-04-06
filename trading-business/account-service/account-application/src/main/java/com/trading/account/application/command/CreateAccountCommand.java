package com.trading.account.application.command;

import com.trading.account.domain.model.valueobject.AccountType;
import com.trading.account.domain.model.valueobject.Currency;

/**
 * 创建账户命令
 */
public class CreateAccountCommand {
    
    private Long userId;
    private AccountType type;
    private Currency currency;
    
    public CreateAccountCommand() {
    }
    
    public CreateAccountCommand(Long userId, AccountType type, Currency currency) {
        this.userId = userId;
        this.type = type;
        this.currency = currency;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public AccountType getType() {
        return type;
    }
    
    public void setType(AccountType type) {
        this.type = type;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
