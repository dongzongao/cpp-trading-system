package com.trading.account.infrastructure.persistence.repository;

import com.trading.account.domain.model.valueobject.Currency;
import com.trading.account.infrastructure.persistence.po.AccountPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 账户JPA仓储
 */
@Repository
public interface AccountJpaRepository extends JpaRepository<AccountPO, Long> {
    
    List<AccountPO> findByUserId(Long userId);
    
    Optional<AccountPO> findByUserIdAndCurrency(Long userId, Currency currency);
    
    boolean existsByUserId(Long userId);
}
