package com.trading.account.infrastructure.persistence.mapper;

import com.trading.account.domain.model.aggregate.Account;
import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.model.valueobject.Money;
import com.trading.account.infrastructure.persistence.po.AccountPO;
import com.trading.user.domain.model.valueobject.UserId;
import org.springframework.stereotype.Component;

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
        
        Account account = Account.create(
                UserId.of(po.getUserId()),
                po.getType(),
                po.getCurrency()
        );
        
        // 使用反射或其他方式设置私有字段
        // 这里简化处理,实际应该通过领域方法设置
        account.setId(AccountId.of(po.getId()));
        
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
}
