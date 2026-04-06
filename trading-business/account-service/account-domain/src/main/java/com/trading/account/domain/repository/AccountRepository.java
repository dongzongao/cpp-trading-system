package com.trading.account.domain.repository;

import com.trading.account.domain.model.aggregate.Account;
import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.model.valueobject.Currency;
import com.trading.user.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 账户仓储接口
 */
public interface AccountRepository {
    
    /**
     * 保存账户
     */
    Account save(Account account);
    
    /**
     * 根据ID查找账户
     */
    Optional<Account> findById(AccountId id);
    
    /**
     * 根据用户ID查找账户列表
     */
    List<Account> findByUserId(UserId userId);
    
    /**
     * 根据用户ID和货币查找账户
     */
    Optional<Account> findByUserIdAndCurrency(UserId userId, Currency currency);
    
    /**
     * 检查用户是否已有账户
     */
    boolean existsByUserId(UserId userId);
}
