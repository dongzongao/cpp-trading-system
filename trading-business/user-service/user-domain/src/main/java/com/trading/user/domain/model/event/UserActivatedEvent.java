package com.trading.user.domain.model.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户激活事件
 */
@Getter
public class UserActivatedEvent {
    private final String userId;
    private final LocalDateTime occurredAt;

    public UserActivatedEvent(String userId) {
        this.userId = userId;
        this.occurredAt = LocalDateTime.now();
    }
}
