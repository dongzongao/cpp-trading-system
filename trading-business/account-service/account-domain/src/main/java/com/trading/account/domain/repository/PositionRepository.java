package com.trading.account.domain.repository;

import com.trading.account.domain.model.entity.Position;
import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.model.valueobject.PositionId;
import com.trading.account.domain.model.valueobject.Symbol;

import java.util.List;
import java.util.Optional;

/**
 * 持仓仓储接口
 */
public interface PositionRepository {
    
    /**
     * 保存持仓
     */
    Position save(Position position);
    
    /**
     * 根据ID查找持仓
     */
    Optional<Position> findById(PositionId id);
    
    /**
     * 根据账户ID和交易品种查找持仓
     */
    Optional<Position> findByAccountIdAndSymbol(AccountId accountId, Symbol symbol);
    
    /**
     * 根据账户ID查找所有持仓
     */
    List<Position> findByAccountId(AccountId accountId);
}
