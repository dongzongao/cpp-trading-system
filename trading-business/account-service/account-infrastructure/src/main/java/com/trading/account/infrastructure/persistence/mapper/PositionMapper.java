package com.trading.account.infrastructure.persistence.mapper;

import com.trading.account.domain.model.entity.Position;
import com.trading.account.domain.model.valueobject.*;
import com.trading.account.infrastructure.persistence.po.PositionPO;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 持仓映射器
 */
@Component
public class PositionMapper {
    
    /**
     * PO转领域对象
     */
    public Position toDomain(PositionPO po) {
        if (po == null) {
            return null;
        }
        
        Currency currency = Currency.valueOf(po.getCurrency());
        Position position = Position.create(
                AccountId.of(po.getAccountId()),
                Symbol.of(po.getSymbol()),
                currency
        );
        
        // 使用反射设置字段
        try {
            setField(position, "id", PositionId.of(po.getId()));
            setField(position, "totalQuantity", Quantity.of(po.getTotalQuantity()));
            setField(position, "availableQuantity", Quantity.of(po.getAvailableQuantity()));
            setField(position, "frozenQuantity", Quantity.of(po.getFrozenQuantity()));
            setField(position, "averageCost", Money.of(po.getAverageCost(), currency));
            setField(position, "createdAt", po.getCreatedAt());
            setField(position, "updatedAt", po.getUpdatedAt());
        } catch (Exception e) {
            throw new RuntimeException("Failed to map PositionPO to Position", e);
        }
        
        return position;
    }
    
    /**
     * 领域对象转PO
     */
    public PositionPO toPO(Position position) {
        if (position == null) {
            return null;
        }
        
        PositionPO po = new PositionPO();
        if (position.getId() != null) {
            po.setId(position.getId().getValue());
        }
        po.setAccountId(position.getAccountId().getValue());
        po.setSymbol(position.getSymbol().getValue());
        po.setTotalQuantity(position.getTotalQuantity().getValue());
        po.setAvailableQuantity(position.getAvailableQuantity().getValue());
        po.setFrozenQuantity(position.getFrozenQuantity().getValue());
        po.setAverageCost(position.getAverageCost().getAmount());
        po.setCurrency(position.getAverageCost().getCurrency().name());
        po.setCreatedAt(position.getCreatedAt());
        po.setUpdatedAt(position.getUpdatedAt());
        
        return po;
    }
    
    /**
     * 使用反射设置私有字段
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
