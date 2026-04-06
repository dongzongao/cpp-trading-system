package com.trading.account.domain.model.valueobject;

/**
 * 货币枚举
 */
public enum Currency {
    
    USD("美元", 2),
    EUR("欧元", 2),
    CNY("人民币", 2),
    USDT("泰达币", 6),
    BTC("比特币", 8),
    ETH("以太坊", 8);
    
    private final String displayName;
    private final int decimalPlaces;
    
    Currency(String displayName, int decimalPlaces) {
        this.displayName = displayName;
        this.decimalPlaces = decimalPlaces;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}
