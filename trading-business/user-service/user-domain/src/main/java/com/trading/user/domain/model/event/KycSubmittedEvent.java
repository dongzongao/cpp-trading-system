package com.trading.user.domain.model.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * KYC提交事件
 */
@Getter
public class KycSubmittedEvent {
    private final String userId;
    private final LocalDateTime occurredAt;

    public KycSubmittedEvent(String userId) {
        this.userId = userId;
        this.occurredAt = LocalDateTime.now();
    }
}
