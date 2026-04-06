package com.trading.account.domain.repository;

import com.trading.account.domain.model.entity.AccountTransaction;
import com.trading.account.domain.model.valueobject.AccountId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账户交易流水仓储接口
 */
public interface AccountTransactionRepository {
    
    /**
     * 保存交易流水
     */
    AccountTransaction save(AccountTransaction transaction);
    
    /**
     * 根据账户ID查找交易流水(分页)
     */
    List<AccountTransaction> findByAccountId(AccountId accountId, int page, int size);
    
    /**
     * 根据账户ID和日期范围查找交易流水
     */
    List<AccountTransaction> findByAccountIdAndDateRange(
            AccountId accountId, 
            LocalDateTime startDate, 
            LocalDateTime endDate);
}
