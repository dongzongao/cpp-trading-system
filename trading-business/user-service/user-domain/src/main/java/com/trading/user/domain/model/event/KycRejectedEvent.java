package com.trading.user.domain.model.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * KYC拒绝事件
 */
@Getter
public class KycRejectedEvent {
    private final String userId;
    private final String reason;
    private final LocalDateTime occurredAt;

    public KycRejectedEvent(String userId, String reason) {
        this.userId = userId;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }
}
