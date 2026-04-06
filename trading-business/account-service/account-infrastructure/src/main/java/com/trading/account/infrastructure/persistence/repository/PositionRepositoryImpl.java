package com.trading.account.infrastructure.persistence.repository;

import com.trading.account.domain.model.entity.Position;
import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.model.valueobject.PositionId;
import com.trading.account.domain.model.valueobject.Symbol;
import com.trading.account.domain.repository.PositionRepository;
import com.trading.account.infrastructure.persistence.mapper.PositionMapper;
import com.trading.account.infrastructure.persistence.po.PositionPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 持仓仓储实现
 */
@Repository
public class PositionRepositoryImpl implements PositionRepository {
    
    private final PositionJpaRepository jpaRepository;
    private final PositionMapper mapper;
    
    public PositionRepositoryImpl(PositionJpaRepository jpaRepository, PositionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Position save(Position position) {
        PositionPO po = mapper.toPO(position);
        po = jpaRepository.save(po);
        return mapper.toDomain(po);
    }
    
    @Override
    public Optional<Position> findById(PositionId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<Position> findByAccountIdAndSymbol(AccountId accountId, Symbol symbol) {
        return jpaRepository.findByAccountIdAndSymbol(accountId.getValue(), symbol.getValue())
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Position> findByAccountId(AccountId accountId) {
        return jpaRepository.findByAccountId(accountId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
