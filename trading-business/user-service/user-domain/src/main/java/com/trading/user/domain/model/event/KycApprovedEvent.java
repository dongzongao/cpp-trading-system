package com.trading.user.domain.model.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * KYC通过事件
 */
@Getter
public class KycApprovedEvent {
    private final String userId;
    private final LocalDateTime occurredAt;

    public KycApprovedEvent(String userId) {
        this.userId = userId;
        this.occurredAt = LocalDateTime.now();
    }
}
