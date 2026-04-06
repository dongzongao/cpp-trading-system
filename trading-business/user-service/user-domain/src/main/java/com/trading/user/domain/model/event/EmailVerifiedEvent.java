package com.trading.user.domain.model.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 邮箱验证事件
 */
@Getter
public class EmailVerifiedEvent {
    private final String userId;
    private final String email;
    private final LocalDateTime occurredAt;

    public EmailVerifiedEvent(String userId, String email) {
        this.userId = userId;
        this.email = email;
        this.occurredAt = LocalDateTime.now();
    }
}
