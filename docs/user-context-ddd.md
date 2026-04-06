# 用户上下文 (User Context) - DDD 详细设计

## 1. 概述

用户上下文负责管理用户的注册、认证、KYC（Know Your Customer）和权限管理。这是一个核心的限界上下文，为其他上下文提供用户身份和权限信息。

## 2. 领域模型

### 2.1 聚合根 (Aggregate Root)

#### User（用户聚合根）

```java
package com.trading.user.domain.model.aggregate;

@Entity
@Table(name = "users")
public class User extends AggregateRoot<UserId> {
    
    private UserId userId;
    private Username username;
    private Email email;
    private Phone phone;
    private Password password;
    private UserStatus status;
    private KycInfo kycInfo;
    private Set<Role> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 工厂方法：创建新用户
    public static User create(Username username, Email email, 
                             Password password) {
        User user = new User();
        user.userId = UserId.generate();
        user.username = username;
        user.email = email;
        user.password = password;
        user.status = UserStatus.PENDING_VERIFICATION;
        user.roles = new HashSet<>();
        user.createdAt = LocalDateTime.now();
        
        // 发布领域事件
        user.registerEvent(new UserCreatedEvent(user.userId, username, email));
        
        return user;
    }
    
    // 领域行为：验证邮箱
    public void verifyEmail() {
        if (this.status != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("User is not in pending verification status");
        }
        
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new EmailVerifiedEvent(this.userId));
    }
    
    // 领域行为：提交 KYC
    public void submitKyc(KycInfo kycInfo) {
        if (this.status != UserStatus.ACTIVE) {
            throw new IllegalStateException("User must be active to submit KYC");
        }
        
        this.kycInfo = kycInfo;
        this.kycInfo.submit();
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new KycSubmittedEvent(this.userId, kycInfo));
    }
    
    // 领域行为：批准 KYC
    public void approveKyc() {
        if (this.kycInfo == null || !this.kycInfo.isPending()) {
            throw new IllegalStateException("No pending KYC to approve");
        }
        
        this.kycInfo.approve();
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new KycApprovedEvent(this.userId));
    }
    
    // 领域行为：拒绝 KYC
    public void rejectKyc(String reason) {
        if (this.kycInfo == null || !this.kycInfo.isPending()) {
            throw new IllegalStateException("No pending KYC to reject");
        }
        
        this.kycInfo.reject(reason);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new KycRejectedEvent(this.userId, reason));
    }
    
    // 领域行为：冻结用户
    public void suspend(String reason) {
        if (this.status == UserStatus.CLOSED) {
            throw new IllegalStateException("Cannot suspend a closed user");
        }
        
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new UserSuspendedEvent(this.userId, reason));
    }
    
    // 领域行为：激活用户
    public void activate() {
        if (this.status != UserStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended users can be activated");
        }
        
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new UserActivatedEvent(this.userId));
    }
    
    // 领域行为：添加角色
    public void addRole(Role role) {
        this.roles.add(role);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new RoleAddedEvent(this.userId, role));
    }
    
    // 领域行为：移除角色
    public void removeRole(Role role) {
        this.roles.remove(role);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new RoleRemovedEvent(this.userId, role));
    }
    
    // 领域行为：修改密码
    public void changePassword(Password oldPassword, Password newPassword) {
        if (!this.password.matches(oldPassword)) {
            throw new InvalidPasswordException("Old password does not match");
        }
        
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new PasswordChangedEvent(this.userId));
    }
    
    // 查询方法
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
    
    public boolean isKycApproved() {
        return this.kycInfo != null && this.kycInfo.isApproved();
    }
    
    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }
}
```

### 2.2 实体 (Entity)

#### KycInfo（KYC 信息实体）

```java
package com.trading.user.domain.model.entity;

@Embeddable
public class KycInfo {
    
    private FullName fullName;
    private IdNumber idNumber;
    private IdType idType;
    private Country country;
    private Address address;
    private KycStatus status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    
    public void submit() {
        this.status = KycStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
    }
    
    public void approve() {
        if (this.status != KycStatus.PENDING) {
            throw new IllegalStateException("KYC is not in pending status");
        }
        this.status = KycStatus.APPROVED;
        this.verifiedAt = LocalDateTime.now();
    }
    
    public void reject(String reason) {
        if (this.status != KycStatus.PENDING) {
            throw new IllegalStateException("KYC is not in pending status");
        }
        this.status = KycStatus.REJECTED;
        this.rejectionReason = reason;
        this.verifiedAt = LocalDateTime.now();
    }
    
    public boolean isPending() {
        return this.status == KycStatus.PENDING;
    }
    
    public boolean isApproved() {
        return this.status == KycStatus.APPROVED;
    }
}
```

### 2.3 值对象 (Value Object)

#### UserId（用户ID）

```java
package com.trading.user.domain.model.valueobject;

@Embeddable
public class UserId implements ValueObject {
    
    @Column(name = "user_id")
    private Long value;
    
    private UserId() {}
    
    private UserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        this.value = value;
    }
    
    public static UserId of(Long value) {
        return new UserId(value);
    }
    
    public static UserId generate() {
        return new UserId(IdGenerator.nextId());
    }
    
    public Long getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId)) return false;
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
```

#### Username（用户名）

```java
package com.trading.user.domain.model.valueobject;

@Embeddable
public class Username implements ValueObject {
    
    @Column(name = "username")
    private String value;
    
    private Username() {}
    
    private Username(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (value.length() < 3 || value.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        if (!value.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers and underscores");
        }
        this.value = value;
    }
    
    public static Username of(String value) {
        return new Username(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Username)) return false;
        Username username = (Username) o;
        return Objects.equals(value, username.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
```

#### Email（邮箱）

```java
package com.trading.user.domain.model.valueobject;

@Embeddable
public class Email implements ValueObject {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    @Column(name = "email")
    private String value;
    
    private Email() {}
    
    private Email(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.value = value.toLowerCase();
    }
    
    public static Email of(String value) {
        return new Email(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email)) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
```

#### Password（密码）

```java
package com.trading.user.domain.model.valueobject;

@Embeddable
public class Password implements ValueObject {
    
    @Column(name = "password_hash")
    private String hashedValue;
    
    private Password() {}
    
    private Password(String plainPassword, boolean isHashed) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        if (isHashed) {
            this.hashedValue = plainPassword;
        } else {
            validatePasswordStrength(plainPassword);
            this.hashedValue = hashPassword(plainPassword);
        }
    }
    
    public static Password fromPlain(String plainPassword) {
        return new Password(plainPassword, false);
    }
    
    public static Password fromHashed(String hashedPassword) {
        return new Password(hashedPassword, true);
    }
    
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
    }
    
    private String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
    
    public boolean matches(Password other) {
        return BCrypt.checkpw(other.hashedValue, this.hashedValue);
    }
    
    public String getHashedValue() {
        return hashedValue;
    }
}
```

### 2.4 枚举

```java
package com.trading.user.domain.model.valueobject;

public enum UserStatus {
    PENDING_VERIFICATION,  // 待验证
    ACTIVE,                // 活跃
    SUSPENDED,             // 冻结
    CLOSED                 // 关闭
}

public enum KycStatus {
    PENDING,    // 待审核
    APPROVED,   // 已批准
    REJECTED    // 已拒绝
}

public enum IdType {
    ID_CARD,       // 身份证
    PASSPORT,      // 护照
    DRIVERS_LICENSE // 驾照
}

public enum Role {
    TRADER,        // 交易员
    ADMIN,         // 管理员
    RISK_MANAGER,  // 风控人员
    AUDITOR        // 审计员
}
```

## 3. 领域服务 (Domain Service)

### UserAuthenticationService（用户认证服务）

```java
package com.trading.user.domain.service;

@Service
public class UserAuthenticationService {
    
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    
    public AuthenticationResult authenticate(Username username, Password password) {
        // 查找用户
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        
        // 检查用户状态
        if (!user.isActive()) {
            throw new UserNotActiveException("User is not active");
        }
        
        // 验证密码
        if (!user.getPassword().matches(password)) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        // 生成 Token
        String token = jwtTokenService.generateToken(user);
        
        return new AuthenticationResult(user.getUserId(), token);
    }
    
    public void validateToken(String token) {
        jwtTokenService.validateToken(token);
    }
}
```

### UserDuplicationCheckService（用户重复检查服务）

```java
package com.trading.user.domain.service;

@Service
public class UserDuplicationCheckService {
    
    private final UserRepository userRepository;
    
    public void checkDuplication(Username username, Email email) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException("Username already exists");
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already exists");
        }
    }
}
```

## 4. 仓储接口 (Repository)

```java
package com.trading.user.domain.repository;

public interface UserRepository {
    
    // 保存用户
    User save(User user);
    
    // 查找用户
    Optional<User> findById(UserId userId);
    Optional<User> findByUsername(Username username);
    Optional<User> findByEmail(Email email);
    
    // 检查存在性
    boolean existsByUsername(Username username);
    boolean existsByEmail(Email email);
    
    // 查询用户列表
    Page<User> findAll(Pageable pageable);
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    Page<User> findByKycStatus(KycStatus kycStatus, Pageable pageable);
    
    // 删除用户
    void delete(User user);
}
```

## 5. 领域事件 (Domain Event)

```java
package com.trading.user.domain.event;

public class UserCreatedEvent extends DomainEvent {
    private final UserId userId;
    private final Username username;
    private final Email email;
    
    public UserCreatedEvent(UserId userId, Username username, Email email) {
        super();
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
    
    // Getters...
}

public class EmailVerifiedEvent extends DomainEvent {
    private final UserId userId;
    
    public EmailVerifiedEvent(UserId userId) {
        super();
        this.userId = userId;
    }
    
    // Getters...
}

public class KycSubmittedEvent extends DomainEvent {
    private final UserId userId;
    private final KycInfo kycInfo;
    
    public KycSubmittedEvent(UserId userId, KycInfo kycInfo) {
        super();
        this.userId = userId;
        this.kycInfo = kycInfo;
    }
    
    // Getters...
}

public class KycApprovedEvent extends DomainEvent {
    private final UserId userId;
    
    public KycApprovedEvent(UserId userId) {
        super();
        this.userId = userId;
    }
    
    // Getters...
}
```

## 6. 应用层 (Application Layer)

### UserApplicationService（用户应用服务）

```java
package com.trading.user.application.service;

@Service
@Transactional
public class UserApplicationService {
    
    private final UserRepository userRepository;
    private final UserDuplicationCheckService duplicationCheckService;
    private final UserAuthenticationService authenticationService;
    private final DomainEventPublisher eventPublisher;
    
    // 注册用户
    public UserDTO registerUser(RegisterUserCommand command) {
        // 检查重复
        duplicationCheckService.checkDuplication(
            Username.of(command.getUsername()),
            Email.of(command.getEmail())
        );
        
        // 创建用户
        User user = User.create(
            Username.of(command.getUsername()),
            Email.of(command.getEmail()),
            Password.fromPlain(command.getPassword())
        );
        
        // 保存用户
        user = userRepository.save(user);
        
        // 发布领域事件
        eventPublisher.publish(user.getDomainEvents());
        
        return UserAssembler.toDTO(user);
    }
    
    // 用户登录
    public LoginResultDTO login(LoginCommand command) {
        AuthenticationResult result = authenticationService.authenticate(
            Username.of(command.getUsername()),
            Password.fromPlain(command.getPassword())
        );
        
        User user = userRepository.findById(result.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        return new LoginResultDTO(
            result.getUserId().getValue(),
            user.getUsername().getValue(),
            result.getToken()
        );
    }
    
    // 提交 KYC
    public void submitKyc(SubmitKycCommand command) {
        User user = userRepository.findById(UserId.of(command.getUserId()))
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        KycInfo kycInfo = new KycInfo(
            FullName.of(command.getFullName()),
            IdNumber.of(command.getIdNumber()),
            IdType.valueOf(command.getIdType()),
            Country.of(command.getCountry()),
            Address.of(command.getAddress())
        );
        
        user.submitKyc(kycInfo);
        userRepository.save(user);
        eventPublisher.publish(user.getDomainEvents());
    }
    
    // 批准 KYC
    public void approveKyc(ApproveKycCommand command) {
        User user = userRepository.findById(UserId.of(command.getUserId()))
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        user.approveKyc();
        userRepository.save(user);
        eventPublisher.publish(user.getDomainEvents());
    }
    
    // 拒绝 KYC
    public void rejectKyc(RejectKycCommand command) {
        User user = userRepository.findById(UserId.of(command.getUserId()))
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        user.rejectKyc(command.getReason());
        userRepository.save(user);
        eventPublisher.publish(user.getDomainEvents());
    }
}
```

## 7. 接口层 (Interface Layer)

### REST Controller

```java
package com.trading.user.interfaces.rest;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserApplicationService userApplicationService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(
            @Valid @RequestBody RegisterUserRequest request) {
        
        RegisterUserCommand command = new RegisterUserCommand(
            request.getUsername(),
            request.getEmail(),
            request.getPassword()
        );
        
        UserDTO user = userApplicationService.registerUser(command);
        
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResultDTO>> login(
            @Valid @RequestBody LoginRequest request) {
        
        LoginCommand command = new LoginCommand(
            request.getUsername(),
            request.getPassword()
        );
        
        LoginResultDTO result = userApplicationService.login(command);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @PostMapping("/kyc")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<ApiResponse<Void>> submitKyc(
            @Valid @RequestBody SubmitKycRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        SubmitKycCommand command = new SubmitKycCommand(
            principal.getUserId(),
            request.getFullName(),
            request.getIdNumber(),
            request.getIdType(),
            request.getCountry(),
            request.getAddress()
        );
        
        userApplicationService.submitKyc(command);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

## 8. 基础设施层 (Infrastructure Layer)

### JPA Repository 实现

```java
package com.trading.user.infrastructure.persistence;

@Repository
public class UserRepositoryImpl implements UserRepository {
    
    private final UserJpaRepository jpaRepository;
    
    @Override
    public User save(User user) {
        UserPO po = UserMapper.toPO(user);
        po = jpaRepository.save(po);
        return UserMapper.toDomain(po);
    }
    
    @Override
    public Optional<User> findById(UserId userId) {
        return jpaRepository.findById(userId.getValue())
            .map(UserMapper::toDomain);
    }
    
    @Override
    public Optional<User> findByUsername(Username username) {
        return jpaRepository.findByUsername(username.getValue())
            .map(UserMapper::toDomain);
    }
    
    @Override
    public boolean existsByUsername(Username username) {
        return jpaRepository.existsByUsername(username.getValue());
    }
    
    // 其他方法实现...
}

interface UserJpaRepository extends JpaRepository<UserPO, Long> {
    Optional<UserPO> findByUsername(String username);
    Optional<UserPO> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

## 9. 完整项目结构

```
user-service/
├── user-interfaces/                    # 接口层
│   ├── rest/
│   │   ├── UserController.java
│   │   ├── request/
│   │   │   ├── RegisterUserRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   └── SubmitKycRequest.java
│   │   └── response/
│   │       ├── UserDTO.java
│   │       └── LoginResultDTO.java
│   ├── grpc/
│   │   └── UserGrpcService.java
│   └── event/
│       └── UserEventListener.java
│
├── user-application/                   # 应用层
│   ├── service/
│   │   └── UserApplicationService.java
│   ├── command/
│   │   ├── RegisterUserCommand.java
│   │   ├── LoginCommand.java
│   │   └── SubmitKycCommand.java
│   └── assembler/
│       └── UserAssembler.java
│
├── user-domain/                        # 领域层
│   ├── model/
│   │   ├── aggregate/
│   │   │   └── User.java
│   │   ├── entity/
│   │   │   └── KycInfo.java
│   │   └── valueobject/
│   │       ├── UserId.java
│   │       ├── Username.java
│   │       ├── Email.java
│   │       ├── Password.java
│   │       ├── UserStatus.java
│   │       └── KycStatus.java
│   ├── service/
│   │   ├── UserAuthenticationService.java
│   │   └── UserDuplicationCheckService.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── event/
│       ├── UserCreatedEvent.java
│       ├── EmailVerifiedEvent.java
│       ├── KycSubmittedEvent.java
│       └── KycApprovedEvent.java
│
└── user-infrastructure/                # 基础设施层
    ├── persistence/
    │   ├── UserRepositoryImpl.java
    │   ├── UserJpaRepository.java
    │   ├── po/
    │   │   └── UserPO.java
    │   └── mapper/
    │       └── UserMapper.java
    ├── messaging/
    │   ├── KafkaEventPublisher.java
    │   └── UserEventProducer.java
    └── external/
        └── EmailService.java
```

## 10. 总结

这个用户上下文的 DDD 设计具有以下特点：

1. **清晰的领域模型**: User 作为聚合根，封装了所有用户相关的业务逻辑
2. **丰富的值对象**: Username、Email、Password 等值对象保证了数据的有效性
3. **领域事件**: 通过领域事件实现与其他上下文的解耦
4. **分层架构**: 严格的分层架构，职责清晰
5. **业务规则内聚**: 所有业务规则都在领域层，易于测试和维护

这种设计可以很容易地扩展到其他限界上下文（账户、交易、风控等）。
