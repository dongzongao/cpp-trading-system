package com.trading.account.domain.model.valueobject;

/**
 * 冻结原因枚举
 */
public enum FreezeReason {
    
    /**
     * 下单冻结
     */
    ORDER_PLACEMENT("下单冻结"),
    
    /**
     * 风控冻结
     */
    RISK_CONTROL("风控冻结"),
    
    /**
     * 提现冻结
     */
    WITHDRAWAL("提现冻结"),
    
    /**
     * 手动冻结
     */
    MANUAL("手动冻结");
    
    private final String displayName;
    
    FreezeReason(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
