package com.trading.account.interfaces.rest.request;

import com.trading.account.domain.model.valueobject.AccountType;
import com.trading.account.domain.model.valueobject.Currency;

import jakarta.validation.constraints.NotNull;

/**
 * 创建账户请求
 */
public class CreateAccountRequest {
    
    @NotNull(message = "User ID cannot be null")
    private Long userId;
    
    @NotNull(message = "Account type cannot be null")
    private AccountType type;
    
    @NotNull(message = "Currency cannot be null")
    private Currency currency;
    
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
