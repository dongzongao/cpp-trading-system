package com.trading.account.application.service;

import com.trading.account.application.command.CreateAccountCommand;
import com.trading.account.application.command.DepositCommand;
import com.trading.account.application.command.WithdrawCommand;
import com.trading.account.application.dto.AccountDTO;
import com.trading.account.application.dto.PositionDTO;
import com.trading.account.application.dto.TransactionDTO;
import com.trading.account.domain.model.aggregate.Account;
import com.trading.account.domain.model.entity.AccountTransaction;
import com.trading.account.domain.model.entity.Position;
import com.trading.account.domain.model.valueobject.*;
import com.trading.account.domain.repository.AccountRepository;
import com.trading.account.domain.repository.AccountTransactionRepository;
import com.trading.account.domain.repository.PositionRepository;
import com.trading.user.domain.model.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户应用服务
 */
@Service
public class AccountApplicationService {
    
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final PositionRepository positionRepository;
    
    public AccountApplicationService(
            AccountRepository accountRepository,
            AccountTransactionRepository transactionRepository,
            PositionRepository positionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.positionRepository = positionRepository;
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
        
        return toAccountDTO(account);
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
        
        return toAccountDTO(account);
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
        
        return toAccountDTO(account);
    }
    
    /**
     * 查询账户
     */
    public AccountDTO getAccount(Long accountId) {
        Account account = accountRepository.findById(AccountId.of(accountId))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return toAccountDTO(account);
    }
    
    /**
     * 查询用户的所有账户
     */
    public List<AccountDTO> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(UserId.of(userId))
                .stream()
                .map(this::toAccountDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询账户持仓
     */
    public List<PositionDTO> getPositions(Long accountId) {
        return positionRepository.findByAccountId(AccountId.of(accountId))
                .stream()
                .map(this::toPositionDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询交易流水
     */
    public List<TransactionDTO> getTransactions(Long accountId, int page, int size) {
        return transactionRepository.findByAccountId(AccountId.of(accountId), page, size)
                .stream()
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为AccountDTO
     */
    private AccountDTO toAccountDTO(Account account) {
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
    
    /**
     * 转换为PositionDTO
     */
    private PositionDTO toPositionDTO(Position position) {
        PositionDTO dto = new PositionDTO();
        dto.setId(position.getId().getValue());
        dto.setAccountId(position.getAccountId().getValue());
        dto.setSymbol(position.getSymbol().getValue());
        dto.setTotalQuantity(position.getTotalQuantity().getValue());
        dto.setAvailableQuantity(position.getAvailableQuantity().getValue());
        dto.setFrozenQuantity(position.getFrozenQuantity().getValue());
        dto.setAverageCost(position.getAverageCost().getAmount());
        dto.setCurrency(position.getAverageCost().getCurrency().name());
        dto.setCreatedAt(position.getCreatedAt());
        dto.setUpdatedAt(position.getUpdatedAt());
        return dto;
    }
    
    /**
     * 转换为TransactionDTO
     */
    private TransactionDTO toTransactionDTO(AccountTransaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId().getValue());
        dto.setAccountId(transaction.getAccountId().getValue());
        dto.setType(transaction.getType());
        dto.setAmount(transaction.getAmount().getAmount());
        dto.setCurrency(transaction.getAmount().getCurrency().name());
        dto.setBalanceBefore(transaction.getBalanceBefore().getAmount());
        dto.setBalanceAfter(transaction.getBalanceAfter().getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setReferenceId(transaction.getReferenceId());
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }
}
