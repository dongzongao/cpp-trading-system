package com.trading.account.interfaces.rest;

import com.trading.account.application.command.CreateAccountCommand;
import com.trading.account.application.command.DepositCommand;
import com.trading.account.application.command.WithdrawCommand;
import com.trading.account.application.dto.AccountDTO;
import com.trading.account.application.service.AccountApplicationService;
import com.trading.account.interfaces.rest.request.CreateAccountRequest;
import com.trading.account.interfaces.rest.request.DepositRequest;
import com.trading.common.dto.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 账户控制器
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    
    private final AccountApplicationService accountApplicationService;
    
    public AccountController(AccountApplicationService accountApplicationService) {
        this.accountApplicationService = accountApplicationService;
    }
    
    /**
     * 创建账户
     */
    @PostMapping
    public Result<AccountDTO> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        CreateAccountCommand command = new CreateAccountCommand(
                request.getUserId(),
                request.getType(),
                request.getCurrency()
        );
        AccountDTO account = accountApplicationService.createAccount(command);
        return Result.success(account);
    }
    
    /**
     * 查询账户
     */
    @GetMapping("/{id}")
    public Result<AccountDTO> getAccount(@PathVariable Long id) {
        AccountDTO account = accountApplicationService.getAccount(id);
        return Result.success(account);
    }
    
    /**
     * 存款
     */
    @PostMapping("/{id}/deposit")
    public Result<AccountDTO> deposit(
            @PathVariable Long id,
            @Valid @RequestBody DepositRequest request) {
        DepositCommand command = new DepositCommand(
                id,
                request.getAmount(),
                request.getDescription()
        );
        AccountDTO account = accountApplicationService.deposit(command);
        return Result.success(account);
    }
    
    /**
     * 取款
     */
    @PostMapping("/{id}/withdraw")
    public Result<AccountDTO> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody DepositRequest request) {
        WithdrawCommand command = new WithdrawCommand(
                id,
                request.getAmount(),
                request.getDescription()
        );
        AccountDTO account = accountApplicationService.withdraw(command);
        return Result.success(account);
    }
    
    /**
     * 查询账户余额
     */
    @GetMapping("/{id}/balance")
    public Result<AccountDTO> getBalance(@PathVariable Long id) {
        AccountDTO account = accountApplicationService.getAccount(id);
        return Result.success(account);
    }
}
