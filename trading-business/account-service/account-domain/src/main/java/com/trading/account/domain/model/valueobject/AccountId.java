package com.trading.account.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 账户ID值对象
 */
public class AccountId implements Serializable {
    
    private final Long value;
    
    private AccountId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Account ID must be positive");
        }
        this.value = value;
    }
    
    public static AccountId of(Long value) {
        return new AccountId(value);
    }
    
    public Long getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountId accountId = (AccountId) o;
        return Objects.equals(value, accountId.value);
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
