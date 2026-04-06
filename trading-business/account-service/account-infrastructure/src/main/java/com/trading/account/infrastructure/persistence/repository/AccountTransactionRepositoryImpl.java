package com.trading.account.infrastructure.persistence.repository;

import com.trading.account.domain.model.entity.AccountTransaction;
import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.repository.AccountTransactionRepository;
import com.trading.account.infrastructure.persistence.mapper.AccountTransactionMapper;
import com.trading.account.infrastructure.persistence.po.AccountTransactionPO;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户交易流水仓储实现
 */
@Repository
public class AccountTransactionRepositoryImpl implements AccountTransactionRepository {
    
    private final AccountTransactionJpaRepository jpaRepository;
    private final AccountTransactionMapper mapper;
    
    public AccountTransactionRepositoryImpl(
            AccountTransactionJpaRepository jpaRepository,
            AccountTransactionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public AccountTransaction save(AccountTransaction transaction) {
        AccountTransactionPO po = mapper.toPO(transaction);
        po = jpaRepository.save(po);
        return mapper.toDomain(po);
    }
    
    @Override
    public List<AccountTransaction> findByAccountId(AccountId accountId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return jpaRepository.findByAccountIdOrderByCreatedAtDesc(accountId.getValue(), pageRequest)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AccountTransaction> findByAccountIdAndDateRange(
            AccountId accountId, 
            LocalDateTime startDate, 
            LocalDateTime endDate) {
        return jpaRepository.findByAccountIdAndDateRange(
                        accountId.getValue(), 
                        startDate, 
                        endDate)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
