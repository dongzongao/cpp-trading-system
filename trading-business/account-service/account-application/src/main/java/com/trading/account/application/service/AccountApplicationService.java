package com.trading.account.application.service;

import com.trading.account.application.command.CreateAccountCommand;
import com.trading.account.application.command.DepositCommand;
import com.trading.account.application.command.WithdrawCommand;
import com.trading.account.application.dto.AccountDTO;
import com.trading.account.domain.model.aggregate.Account;
import com.trading.account.domain.model.entity.AccountTransaction;
import com.trading.account.domain.model.valueobject.*;
import com.trading.account.domain.repository.AccountRepository;
import com.trading.account.domain.repository.AccountTransactionRepository;
import com.trading.user.domain.model.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账户应用服务
 */
@Service
public class AccountApplicationService {
    
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    
    public AccountApplicationService(
            AccountRepository accountRepository,
            AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * 创建账户
     */
    @Transactional
    public AccountDTO createAccount(CreateAccountCommand command) {
        UserId userId = UserId.of(command.getUserId());
        
        // 检查是否已存在相同货币的账户
        if (accountRepository.findByUserIdAndCurrency(userId, command.getCurrency()).isPresent()) {
            throw new IllegalStateException("Account already exists for this currency");
        }
        
        // 创建账户
        Account account = Account.create(userId, command.getType(), command.getCurrency());
        account = accountRepository.save(account);
        
        return toDTO(account);
    }
    
    /**
     * 存款
     */
    @Transactional
    public AccountDTO deposit(DepositCommand command) {
        AccountId accountId = AccountId.of(command.getAccountId());
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        Money amount = Money.of(command.getAmount(), account.getCurrency());
        Money balanceBefore = account.getTotalBalance();
        
        // 执行存款
        account.deposit(amount);
        account = accountRepository.save(account);
        
        // 记录流水
        AccountTransaction transaction = AccountTransaction.create(
                accountId,
                TransactionType.DEPOSIT,
                amount,
                balanceBefore,
                account.getTotalBalance(),
                command.getDescription(),
                null
        );
        transactionRepository.save(transaction);
        
        return toDTO(account);
    }
    
    /**
     * 取款
     */
    @Transactional
    public AccountDTO withdraw(WithdrawCommand command) {
        AccountId accountId = AccountId.of(command.getAccountId());
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        Money amount = Money.of(command.getAmount(), account.getCurrency());
        Money balanceBefore = account.getTotalBalance();
        
        // 执行取款
        account.withdraw(amount);
        account = accountRepository.save(account);
        
        // 记录流水
        AccountTransaction transaction = AccountTransaction.create(
                accountId,
                TransactionType.WITHDRAWAL,
                amount,
                balanceBefore,
                account.getTotalBalance(),
                command.getDescription(),
                null
        );
        transactionRepository.save(transaction);
        
        return toDTO(account);
    }
    
    /**
     * 查询账户
     */
    public AccountDTO getAccount(Long accountId) {
        Account account = accountRepository.findById(AccountId.of(accountId))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return toDTO(account);
    }
    
    /**
     * 转换为DTO
     */
    private AccountDTO toDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId().getValue());
        dto.setUserId(account.getUserId().getValue());
        dto.setType(account.getType());
        dto.setCurrency(account.getCurrency());
        dto.setTotalBalance(account.getTotalBalance().getAmount());
        dto.setAvailableBalance(account.getAvailableBalance().getAmount());
        dto.setFrozenBalance(account.getFrozenBalance().getAmount());
        dto.setStatus(account.getStatus());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        return dto;
    }
}
