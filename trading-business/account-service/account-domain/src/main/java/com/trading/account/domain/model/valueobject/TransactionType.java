package com.trading.account.domain.model.valueobject;

/**
 * 交易类型枚举
 */
public enum TransactionType {
    
    // 资金操作
    DEPOSIT("存款"),
    WITHDRAWAL("取款"),
    
    // 冻结操作
    FREEZE("冻结"),
    UNFREEZE("解冻"),
    DEDUCT_FROZEN("扣减冻结"),
    
    // 交易操作
    TRADE_BUY("买入"),
    TRADE_SELL("卖出"),
    
    // 其他
    FEE("手续费"),
    INTEREST("利息"),
    DIVIDEND("分红");
    
    private final String displayName;
    
    TransactionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
