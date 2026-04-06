package com.trading.user.domain.model.valueobject;

/**
 * KYC状态枚举
 */
public enum KycStatus {
    /**
     * 待审核
     */
    PENDING,
    
    /**
     * 已通过
     */
    APPROVED,
    
    /**
     * 已拒绝
     */
    REJECTED
}
