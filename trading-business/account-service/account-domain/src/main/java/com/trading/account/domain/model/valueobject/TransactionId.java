package com.trading.account.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 交易流水ID值对象
 */
public class TransactionId implements Serializable {
    
    private final Long value;
    
    private TransactionId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Transaction ID must be positive");
        }
        this.value = value;
    }
    
    public static TransactionId of(Long value) {
        return new TransactionId(value);
    }
    
    public Long getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
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
