package com.trading.account.domain.model.valueobject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 数量值对象
 */
public class Quantity implements Serializable {
    
    private static final int SCALE = 8;
    private final BigDecimal value;
    
    private Quantity(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    public static Quantity of(BigDecimal value) {
        return new Quantity(value);
    }
    
    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO);
    }
    
    public Quantity add(Quantity other) {
        return new Quantity(this.value.add(other.value));
    }
    
    public Quantity subtract(Quantity other) {
        BigDecimal result = this.value.subtract(other.value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result quantity cannot be negative");
        }
        return new Quantity(result);
    }
    
    public Quantity multiply(BigDecimal multiplier) {
        return new Quantity(this.value.multiply(multiplier));
    }
    
    public boolean isGreaterThan(Quantity other) {
        return this.value.compareTo(other.value) > 0;
    }
    
    public boolean isLessThan(Quantity other) {
        return this.value.compareTo(other.value) < 0;
    }
    
    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value.compareTo(other.value) >= 0;
    }
    
    public boolean isLessThanOrEqual(Quantity other) {
        return this.value.compareTo(other.value) <= 0;
    }
    
    public boolean isZero() {
        return this.value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isPositive() {
        return this.value.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quantity quantity = (Quantity) o;
        return Objects.equals(value, quantity.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
