package com.trading.account.infrastructure.persistence.mapper;

import com.trading.account.domain.model.entity.AccountTransaction;
import com.trading.account.domain.model.valueobject.*;
import com.trading.account.infrastructure.persistence.po.AccountTransactionPO;
import org.springframework.stereotype.Component;

/**
 * 账户交易流水映射器
 */
@Component
public class AccountTransactionMapper {
    
    /**
     * PO转领域对象
     */
    public AccountTransaction toDomain(AccountTransactionPO po) {
        if (po == null) {
            return null;
        }
        
        Currency currency = Currency.valueOf(po.getCurrency());
        
        AccountTransaction transaction = AccountTransaction.create(
                AccountId.of(po.getAccountId()),
                po.getTransactionType(),
                Money.of(po.getAmount(), currency),
                Money.of(po.getBalanceBefore(), currency),
                Money.of(po.getBalanceAfter(), currency),
                po.getDescription(),
                po.getReferenceId()
        );
        
        transaction.setId(TransactionId.of(po.getId()));
        
        return transaction;
    }
    
    /**
     * 领域对象转PO
     */
    public AccountTransactionPO toPO(AccountTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        
        AccountTransactionPO po = new AccountTransactionPO();
        if (transaction.getId() != null) {
            po.setId(transaction.getId().getValue());
        }
        po.setAccountId(transaction.getAccountId().getValue());
        po.setTransactionType(transaction.getType());
        po.setAmount(transaction.getAmount().getAmount());
        po.setCurrency(transaction.getAmount().getCurrency().name());
        po.setBalanceBefore(transaction.getBalanceBefore().getAmount());
        po.setBalanceAfter(transaction.getBalanceAfter().getAmount());
        po.setDescription(transaction.getDescription());
        po.setReferenceId(transaction.getReferenceId());
        po.setCreatedAt(transaction.getCreatedAt());
        
        return po;
    }
}
