package com.trading.account.infrastructure.persistence.repository;

import com.trading.account.infrastructure.persistence.po.AccountTransactionPO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账户交易流水JPA仓储
 */
@Repository
public interface AccountTransactionJpaRepository extends JpaRepository<AccountTransactionPO, Long> {
    
    List<AccountTransactionPO> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    
    @Query("SELECT t FROM AccountTransactionPO t WHERE t.accountId = :accountId " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate " +
           "ORDER BY t.createdAt DESC")
    List<AccountTransactionPO> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
