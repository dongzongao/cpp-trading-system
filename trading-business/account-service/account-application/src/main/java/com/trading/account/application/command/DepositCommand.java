package com.trading.account.application.command;

import java.math.BigDecimal;

/**
 * 存款命令
 */
public class DepositCommand {
    
    private Long accountId;
    private BigDecimal amount;
    private String description;
    
    public DepositCommand() {
    }
    
    public DepositCommand(Long accountId, BigDecimal amount, String description) {
        this.accountId = accountId;
        this.amount = amount;
        this.description = description;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
