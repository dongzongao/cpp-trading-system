package com.trading.account.infrastructure.persistence.mapper;

import com.trading.account.domain.model.aggregate.Account;
import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.model.valueobject.Money;
import com.trading.account.infrastructure.persistence.po.AccountPO;
import com.trading.user.domain.model.valueobject.UserId;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 账户映射器
 */
@Component
public class AccountMapper {
    
    /**
     * PO转领域对象
     */
    public Account toDomain(AccountPO po) {
        if (po == null) {
            return null;
        }
        
        // 创建账户
        Account account = Account.create(
                UserId.of(po.getUserId()),
                po.getType(),
                po.getCurrency()
        );
        
        // 使用反射设置ID和其他字段
        try {
            setField(account, "id", AccountId.of(po.getId()));
            setField(account, "totalBalance", Money.of(po.getTotalBalance(), po.getCurrency()));
            setField(account, "availableBalance", Money.of(po.getAvailableBalance(), po.getCurrency()));
            setField(account, "frozenBalance", Money.of(po.getFrozenBalance(), po.getCurrency()));
            setField(account, "status", po.getStatus());
            setField(account, "createdAt", po.getCreatedAt());
            setField(account, "updatedAt", po.getUpdatedAt());
        } catch (Exception e) {
            throw new RuntimeException("Failed to map AccountPO to Account", e);
        }
        
        return account;
    }
    
    /**
     * 领域对象转PO
     */
    public AccountPO toPO(Account account) {
        if (account == null) {
            return null;
        }
        
        AccountPO po = new AccountPO();
        if (account.getId() != null) {
            po.setId(account.getId().getValue());
        }
        po.setUserId(account.getUserId().getValue());
        po.setType(account.getType());
        po.setCurrency(account.getCurrency());
        po.setTotalBalance(account.getTotalBalance().getAmount());
        po.setAvailableBalance(account.getAvailableBalance().getAmount());
        po.setFrozenBalance(account.getFrozenBalance().getAmount());
        po.setStatus(account.getStatus());
        po.setCreatedAt(account.getCreatedAt());
        po.setUpdatedAt(account.getUpdatedAt());
        
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
