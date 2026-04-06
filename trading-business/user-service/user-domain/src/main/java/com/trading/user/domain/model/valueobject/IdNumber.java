package com.trading.user.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 证件号值对象
 */
@Getter
@EqualsAndHashCode
public class IdNumber implements Serializable {
    private final String value;
    private final IdType type;

    private IdNumber(String value, IdType type) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ID number cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("ID type cannot be null");
        }
        if (value.length() < 5 || value.length() > 30) {
            throw new IllegalArgumentException("ID number length must be between 5 and 30");
        }
        this.value = value.trim().toUpperCase();
        this.type = type;
    }

    public static IdNumber of(String value, IdType type) {
        return new IdNumber(value, type);
    }

    @Override
    public String toString() {
        return type + ":" + value;
    }
}
