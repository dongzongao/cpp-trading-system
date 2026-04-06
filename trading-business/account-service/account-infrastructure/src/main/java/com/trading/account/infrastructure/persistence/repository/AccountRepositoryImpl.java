package com.trading.account.infrastructure.persistence.repository;

import com.trading.account.domain.model.aggregate.Account;
import com.trading.account.domain.model.valueobject.AccountId;
import com.trading.account.domain.model.valueobject.Currency;
import com.trading.account.domain.repository.AccountRepository;
import com.trading.account.infrastructure.persistence.mapper.AccountMapper;
import com.trading.account.infrastructure.persistence.po.AccountPO;
import com.trading.user.domain.model.valueobject.UserId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 账户仓储实现
 */
@Repository
public class AccountRepositoryImpl implements AccountRepository {
    
    private final AccountJpaRepository jpaRepository;
    private final AccountMapper mapper;
    
    public AccountRepositoryImpl(AccountJpaRepository jpaRepository, AccountMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Account save(Account account) {
        AccountPO po = mapper.toPO(account);
        po = jpaRepository.save(po);
        return mapper.toDomain(po);
    }
    
    @Override
    public Optional<Account> findById(AccountId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Account> findByUserId(UserId userId) {
        return jpaRepository.findByUserId(userId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Account> findByUserIdAndCurrency(UserId userId, Currency currency) {
        return jpaRepository.findByUserIdAndCurrency(userId.getValue(), currency)
                .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsByUserId(UserId userId) {
        return jpaRepository.existsByUserId(userId.getValue());
    }
}
