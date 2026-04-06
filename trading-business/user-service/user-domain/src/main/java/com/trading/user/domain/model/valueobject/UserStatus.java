package com.trading.user.domain.model.valueobject;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    /**
     * 待验证邮箱
     */
    PENDING_VERIFICATION,
    
    /**
     * 活跃状态
     */
    ACTIVE,
    
    /**
     * 已暂停
     */
    SUSPENDED,
    
    /**
     * 已关闭
     */
    CLOSED
}
