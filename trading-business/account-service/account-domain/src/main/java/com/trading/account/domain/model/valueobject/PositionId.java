package com.trading.account.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 持仓ID值对象
 */
public class PositionId implements Serializable {
    
    private final Long value;
    
    private PositionId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Position ID must be positive");
        }
        this.value = value;
    }
    
    public static PositionId of(Long value) {
        return new PositionId(value);
    }
    
    public Long getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionId that = (PositionId) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
