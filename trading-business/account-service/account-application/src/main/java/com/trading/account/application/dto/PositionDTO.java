package com.trading.account.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓DTO
 */
public class PositionDTO {
    
    private Long id;
    private Long accountId;
    private String symbol;
    private BigDecimal totalQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal frozenQuantity;
    private BigDecimal averageCost;
    private String currency;
    private LocalDateTime createdAt;
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
