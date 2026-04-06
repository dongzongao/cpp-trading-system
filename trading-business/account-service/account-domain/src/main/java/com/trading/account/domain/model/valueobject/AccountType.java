package com.trading.account.domain.model.valueobject;

/**
 * 账户类型枚举
 */
public enum AccountType {
    
    /**
     * 现金账户
     */
    CASH("现金账户"),
    
    /**
     * 保证金账户
     */
    MARGIN("保证金账户");
    
    private final String displayName;
    
    AccountType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
