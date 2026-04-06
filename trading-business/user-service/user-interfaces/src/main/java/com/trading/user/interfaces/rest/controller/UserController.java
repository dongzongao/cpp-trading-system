package com.trading.user.interfaces.rest.controller;

import com.trading.common.model.Result;
import com.trading.user.application.command.LoginCommand;
import com.trading.user.application.command.RegisterUserCommand;
import com.trading.user.application.command.SubmitKycCommand;
import com.trading.user.application.dto.LoginResultDTO;
import com.trading.user.application.dto.UserDTO;
import com.trading.user.application.service.UserApplicationService;
import com.trading.user.interfaces.rest.request.LoginRequest;
import com.trading.user.interfaces.rest.request.RegisterUserRequest;
import com.trading.user.interfaces.rest.request.SubmitKycRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserApplicationService userApplicationService;

    /**
     * 注册用户
     */
    @PostMapping("/register")
    public ResponseEntity<Result<UserDTO>> register(@Valid @RequestBody RegisterUserRequest request) {
        log.info("Register user request: {}", request.getUsername());
        
        RegisterUserCommand command = new RegisterUserCommand(
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            request.getPhone()
        );
        
        UserDTO user = userApplicationService.registerUser(command);
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResultDTO>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request: {}", request.getUsername());
        
        LoginCommand command = new LoginCommand(
            request.getUsername(),
            request.getPassword()
        );
        
        LoginResultDTO result = userApplicationService.login(command);
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 提交KYC
     */
    @PostMapping("/{userId}/kyc")
    public ResponseEntity<Result<UserDTO>> submitKyc(
            @PathVariable String userId,
            @Valid @RequestBody SubmitKycRequest request) {
        log.info("Submit KYC request for user: {}", userId);
        
        SubmitKycCommand command = new SubmitKycCommand(
            userId,
            request.getFirstName(),
            request.getLastName(),
            request.getIdType(),
            request.getIdNumber(),
            request.getCountry(),
            request.getProvince(),
            request.getCity(),
            request.getStreet(),
            request.getPostalCode(),
            request.getDateOfBirth()
        );
        
        UserDTO user = userApplicationService.submitKyc(command);
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * 批准KYC
     */
    @PostMapping("/{userId}/kyc/approve")
    public ResponseEntity<Result<UserDTO>> approveKyc(@PathVariable String userId) {
        log.info("Approve KYC for user: {}", userId);
        UserDTO user = userApplicationService.approveKyc(userId);
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * 拒绝KYC
     */
    @PostMapping("/{userId}/kyc/reject")
    public ResponseEntity<Result<UserDTO>> rejectKyc(
            @PathVariable String userId,
            @RequestParam String reason) {
        log.info("Reject KYC for user: {}", userId);
        UserDTO user = userApplicationService.rejectKyc(userId, reason);
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * 验证邮箱
     */
    @PostMapping("/{userId}/verify-email")
    public ResponseEntity<Result<UserDTO>> verifyEmail(@PathVariable String userId) {
        log.info("Verify email for user: {}", userId);
        UserDTO user = userApplicationService.verifyEmail(userId);
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * 查询用户信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Result<UserDTO>> getUser(@PathVariable String userId) {
        log.info("Get user: {}", userId);
        UserDTO user = userApplicationService.getUser(userId);
        return ResponseEntity.ok(Result.success(user));
    }
}
