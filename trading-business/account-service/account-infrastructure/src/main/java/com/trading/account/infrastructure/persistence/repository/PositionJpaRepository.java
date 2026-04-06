package com.trading.account.infrastructure.persistence.repository;

import com.trading.account.infrastructure.persistence.po.PositionPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 持仓JPA仓储
 */
@Repository
public interface PositionJpaRepository extends JpaRepository<PositionPO, Long> {
    
    Optional<PositionPO> findByAccountIdAndSymbol(Long accountId, String symbol);
    
    List<PositionPO> findByAccountId(Long accountId);
}
