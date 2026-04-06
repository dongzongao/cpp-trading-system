package com.trading.account.infrastructure.persistence.po;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓持久化对象
 */
@Entity
@Table(name = "positions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "symbol"}),
       indexes = {
           @Index(name = "idx_positions_account_id", columnList = "account_id"),
           @Index(name = "idx_positions_symbol", columnList = "symbol")
       })
public class PositionPO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;
    
    @Column(name = "total_quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal totalQuantity;
    
    @Column(name = "available_quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal availableQuantity;
    
    @Column(name = "frozen_quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal frozenQuantity;
    
    @Column(name = "average_cost", nullable = false, precision = 20, scale = 8)
    private BigDecimal averageCost;
    
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(BigDecimal availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    
    public BigDecimal getFrozenQuantity() {
        return frozenQuantity;
    }
    
    public void setFrozenQuantity(BigDecimal frozenQuantity) {
        this.frozenQuantity = frozenQuantity;
    }
    
    public BigDecimal getAverageCost() {
        return averageCost;
    }
    
    public void setAverageCost(BigDecimal averageCost) {
        this.averageCost = averageCost;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
