package com.trading.common.constants;

/**
 * 统一错误码定义
 */
public class ErrorCode {
    
    // 成功
    public static final int SUCCESS = 0;
    
    // 通用错误 (1xxx)
    public static final int SYSTEM_ERROR = 1000;
    public static final int INVALID_PARAMETER = 1001;
    public static final int UNAUTHORIZED = 1002;
    public static final int FORBIDDEN = 1003;
    public static final int NOT_FOUND = 1004;
    public static final int DUPLICATE_REQUEST = 1005;
    
    // 用户错误 (2xxx)
    public static final int USER_NOT_FOUND = 2001;
    public static final int USER_ALREADY_EXISTS = 2002;
    public static final int INVALID_CREDENTIALS = 2003;
    public static final int USER_DISABLED = 2004;
    public static final int KYC_NOT_VERIFIED = 2005;
    
    // 账户错误 (3xxx)
    public static final int ACCOUNT_NOT_FOUND = 3001;
    public static final int INSUFFICIENT_BALANCE = 3002;
    public static final int ACCOUNT_FROZEN = 3003;
    public static final int INVALID_ACCOUNT_TYPE = 3004;
    public static final int POSITION_NOT_FOUND = 3005;
    
    // 订单错误 (4xxx)
    public static final int ORDER_NOT_FOUND = 4001;
    public static final int INVALID_ORDER_PRICE = 4002;
    public static final int INVALID_ORDER_QUANTITY = 4003;
    public static final int ORDER_ALREADY_FILLED = 4004;
    public static final int ORDER_ALREADY_CANCELLED = 4005;
    public static final int INSTRUMENT_NOT_FOUND = 4006;
    public static final int TRADING_SUSPENDED = 4007;
    
    // 风控错误 (5xxx)
    public static final int RISK_CHECK_FAILED = 5001;
    public static final int POSITION_LIMIT_EXCEEDED = 5002;
    public static final int DAILY_TRADE_LIMIT_EXCEEDED = 5003;
    public static final int PRICE_LIMIT_EXCEEDED = 5004;
    public static final int SELF_TRADE_DETECTED = 5005;
    
    // 清算错误 (6xxx)
    public static final int SETTLEMENT_FAILED = 6001;
    public static final int RECONCILIATION_FAILED = 6002;
    public static final int SETTLEMENT_IN_PROGRESS = 6003;
    
    // 通知错误 (7xxx)
    public static final int NOTIFICATION_SEND_FAILED = 7001;
    
    // 系统错误 (9xxx)
    public static final int DATABASE_ERROR = 9001;
    public static final int NETWORK_ERROR = 9002;
    public static final int TIMEOUT_ERROR = 9003;
    public static final int EXTERNAL_SERVICE_ERROR = 9004;
    
    private ErrorCode() {
        // Utility class
    }
}
