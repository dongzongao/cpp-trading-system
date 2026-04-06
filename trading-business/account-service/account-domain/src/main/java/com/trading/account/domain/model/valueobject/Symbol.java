package com.trading.account.domain.model.valueobject;

import java.io.Serializable;
import java.util.Objects;

/**
 * 交易品种代码值对象
 */
public class Symbol implements Serializable {
    
    private final String value;
    
    private Symbol(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (!value.matches("^[A-Z0-9]{1,20}$")) {
            throw new IllegalArgumentException("Symbol must contain only uppercase letters and numbers, max 20 characters");
        }
        this.value = value.toUpperCase();
    }
    
    public static Symbol of(String value) {
        return new Symbol(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(value, symbol.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
