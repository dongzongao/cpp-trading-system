package com.trading.user.domain.model.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户创建事件
 */
@Getter
public class UserCreatedEvent {
    private final String userId;
    private final String username;
    private final String email;
    private final LocalDateTime occurredAt;

    public UserCreatedEvent(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.occurredAt = LocalDateTime.now();
    }
}
