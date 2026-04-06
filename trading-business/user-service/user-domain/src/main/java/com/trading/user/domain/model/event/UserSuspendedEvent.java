package com.trading.user.domain.model.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户暂停事件
 */
@Getter
public class UserSuspendedEvent {
    private final String userId;
    private final String reason;
    private final LocalDateTime occurredAt;

    public UserSuspendedEvent(String userId, String reason) {
        this.userId = userId;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }
}
