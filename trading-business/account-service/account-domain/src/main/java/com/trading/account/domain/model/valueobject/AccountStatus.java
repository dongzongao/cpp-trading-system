package com.trading.account.domain.model.valueobject;

/**
 * 账户状态枚举
 */
public enum AccountStatus {
    
    /**
     * 活跃
     */
    ACTIVE("活跃"),
    
    /**
     * 暂停
     */
    SUSPENDED("暂停"),
    
    /**
     * 关闭
     */
    CLOSED("关闭");
    
    private final String displayName;
    
    AccountStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
