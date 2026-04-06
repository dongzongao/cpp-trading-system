package com.trading.user.application.service;

import com.trading.user.application.assembler.UserAssembler;
import com.trading.user.application.command.LoginCommand;
import com.trading.user.application.command.RegisterUserCommand;
import com.trading.user.application.command.SubmitKycCommand;
import com.trading.user.application.dto.LoginResultDTO;
import com.trading.user.application.dto.UserDTO;
import com.trading.user.domain.model.aggregate.User;
import com.trading.user.domain.model.entity.KycInfo;
import com.trading.user.domain.model.valueobject.*;
import com.trading.user.domain.repository.UserRepository;
import com.trading.user.domain.service.JwtTokenService;
import com.trading.user.domain.service.UserAuthenticationService;
import com.trading.user.domain.service.UserDuplicationCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {
    private final UserRepository userRepository;
    private final UserDuplicationCheckService duplicationCheckService;
    private final UserAuthenticationService authenticationService;
    private final JwtTokenService jwtTokenService;
    private final UserAssembler userAssembler;

    /**
     * 注册用户
     */
    @Transactional
    public UserDTO registerUser(RegisterUserCommand command) {
        log.info("Registering user: {}", command.getUsername());

        // 创建值对象
        Username username = Username.of(command.getUsername());
        Email email = Email.of(command.getEmail());
        Password password = Password.fromRaw(command.getPassword());
        Phone phone = command.getPhone() != null ? Phone.of(command.getPhone()) : null;

        // 检查重复
        duplicationCheckService.checkDuplication(username, email);

        // 创建用户
        User user = User.create(username, email, password, phone);

        // 保存用户
        user = userRepository.save(user);

        log.info("User registered successfully: {}", user.getUserId().getValue());

        return userAssembler.toDTO(user);
    }

    /**
     * 用户登录
     */
    @Transactional(readOnly = true)
    public LoginResultDTO login(LoginCommand command) {
        log.info("User login: {}", command.getUsername());

        Username username = Username.of(command.getUsername());
        String token = authenticationService.authenticate(username, command.getPassword());

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return LoginResultDTO.builder()
            .token(token)
            .userId(user.getUserId().getValue())
            .username(user.getUsername().getValue())
            .email(user.getEmail().getValue())
            .build();
    }

    /**
     * 提交KYC
     */
    @Transactional
    public UserDTO submitKyc(SubmitKycCommand command) {
        log.info("Submitting KYC for user: {}", command.getUserId());

        UserId userId = UserId.of(command.getUserId());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 创建KYC信息
        FullName fullName = FullName.of(command.getFirstName(), command.getLastName());
        IdNumber idNumber = IdNumber.of(command.getIdNumber(), IdType.valueOf(command.getIdType()));
        Address address = Address.of(
            command.getCountry(),
            command.getProvince(),
            command.getCity(),
            command.getStreet(),
            command.getPostalCode()
        );

        KycInfo kycInfo = KycInfo.create(fullName, idNumber, address, command.getDateOfBirth());

        // 提交KYC
        user.submitKyc(kycInfo);

        // 保存
        user = userRepository.save(user);

        log.info("KYC submitted successfully for user: {}", userId.getValue());

        return userAssembler.toDTO(user);
    }

    /**
     * 批准KYC
     */
    @Transactional
    public UserDTO approveKyc(String userId) {
        log.info("Approving KYC for user: {}", userId);

        UserId id = UserId.of(userId);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.approveKyc();
        user = userRepository.save(user);

        log.info("KYC approved for user: {}", userId);

        return userAssembler.toDTO(user);
    }

    /**
     * 拒绝KYC
     */
    @Transactional
    public UserDTO rejectKyc(String userId, String reason) {
        log.info("Rejecting KYC for user: {}", userId);

        UserId id = UserId.of(userId);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.rejectKyc(reason);
        user = userRepository.save(user);

        log.info("KYC rejected for user: {}", userId);

        return userAssembler.toDTO(user);
    }

    /**
     * 验证邮箱
     */
    @Transactional
    public UserDTO verifyEmail(String userId) {
        log.info("Verifying email for user: {}", userId);

        UserId id = UserId.of(userId);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.verifyEmail();
        user = userRepository.save(user);

        log.info("Email verified for user: {}", userId);

        return userAssembler.toDTO(user);
    }

    /**
     * 查询用户
     */
    @Transactional(readOnly = true)
    public UserDTO getUser(String userId) {
        UserId id = UserId.of(userId);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userAssembler.toDTO(user);
    }
}
