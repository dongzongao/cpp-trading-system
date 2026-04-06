package com.trading.account.domain.model.entity;

import com.trading.account.domain.model.valueobject.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 持仓实体
 */
public class Position {
    
    private PositionId id;
    private AccountId accountId;
    private Symbol symbol;
    private Quantity totalQuantity;
    private Quantity availableQuantity;
    private Quantity frozenQuantity;
    private Money averageCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Position() {
    }
    
    public static Position create(AccountId accountId, Symbol symbol, Currency currency) {
        Position position = new Position();
        position.accountId = accountId;
        position.symbol = symbol;
        position.totalQuantity = Quantity.zero();
        position.availableQuantity = Quantity.zero();
        position.frozenQuantity = Quantity.zero();
        position.averageCost = Money.zero(currency);
        position.createdAt = LocalDateTime.now();
        position.updatedAt = LocalDateTime.now();
        return position;
    }
    
    /**
     * 增加持仓
     */
    public void increase(Quantity quantity, Money cost) {
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (cost == null || !cost.isPositive()) {
            throw new IllegalArgumentException("Cost must be positive");
        }
        
        // 计算新的平均成本
        Money totalCost = averageCost.multiply(totalQuantity.getValue())
                                     .add(cost.multiply(quantity.getValue()));
        Quantity newTotalQuantity = totalQuantity.add(quantity);
        
        if (newTotalQuantity.isPositive()) {
            this.averageCost = totalCost.divide(newTotalQuantity.getValue());
        }
        
        this.totalQuantity = newTotalQuantity;
        this.availableQuantity = availableQuantity.add(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 减少持仓
     */
    public void decrease(Quantity quantity) {
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (availableQuantity.isLessThan(quantity)) {
            throw new IllegalStateException("Insufficient available quantity");
        }
        
        this.totalQuantity = totalQuantity.subtract(quantity);
        this.availableQuantity = availableQuantity.subtract(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 冻结持仓
     */
    public void freeze(Quantity quantity) {
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (availableQuantity.isLessThan(quantity)) {
            throw new IllegalStateException("Insufficient available quantity to freeze");
        }
        
        this.availableQuantity = availableQuantity.subtract(quantity);
        this.frozenQuantity = frozenQuantity.add(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 解冻持仓
     */
    public void unfreeze(Quantity quantity) {
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (frozenQuantity.isLessThan(quantity)) {
            throw new IllegalStateException("Insufficient frozen quantity to unfreeze");
        }
        
        this.frozenQuantity = frozenQuantity.subtract(quantity);
        this.availableQuantity = availableQuantity.add(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 扣减冻结持仓
     */
    public void deductFrozen(Quantity quantity) {
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (frozenQuantity.isLessThan(quantity)) {
            throw new IllegalStateException("Insufficient frozen quantity to deduct");
        }
        
        this.frozenQuantity = frozenQuantity.subtract(quantity);
        this.totalQuantity = totalQuantity.subtract(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters
    public PositionId getId() {
        return id;
    }
    
    public void setId(PositionId id) {
        this.id = id;
    }
    
    public AccountId getAccountId() {
        return accountId;
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public Quantity getTotalQuantity() {
        return totalQuantity;
    }
    
    public Quantity getAvailableQuantity() {
        return availableQuantity;
    }
    
    public Quantity getFrozenQuantity() {
        return frozenQuantity;
    }
    
    public Money getAverageCost() {
        return averageCost;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
