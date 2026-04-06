package com.trading.user.domain.model.aggregate;

import com.trading.user.domain.model.entity.KycInfo;
import com.trading.user.domain.model.event.*;
import com.trading.user.domain.model.valueobject.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户聚合根
 */
@Getter
public class User {
    private UserId userId;
    private Username username;
    private Email email;
    private Password password;
    private Phone phone;
    private UserStatus status;
    private KycInfo kycInfo;
    private Set<Role> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 领域事件列表
    private transient List<Object> domainEvents = new ArrayList<>();

    private User() {
        this.roles = new HashSet<>();
    }

    /**
     * 创建用户（工厂方法）
     */
    public static User create(Username username, Email email, Password password, Phone phone) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        User user = new User();
        user.userId = UserId.generate();
        user.username = username;
        user.email = email;
        user.password = password;
        user.phone = phone;
        user.status = UserStatus.PENDING_VERIFICATION;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        user.roles.add(Role.TRADER); // 默认角色

        // 发布用户创建事件
        user.addDomainEvent(new UserCreatedEvent(
            user.userId.getValue(),
            user.username.getValue(),
            user.email.getValue()
        ));

        return user;
    }

    /**
     * 验证邮箱
     */
    public void verifyEmail() {
        if (status != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("User is not in pending verification status");
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new EmailVerifiedEvent(userId.getValue(), email.getValue()));
    }

    /**
     * 提交KYC
     */
    public void submitKyc(KycInfo kycInfo) {
        if (status != UserStatus.ACTIVE) {
            throw new IllegalStateException("User must be active to submit KYC");
        }
        if (kycInfo == null) {
            throw new IllegalArgumentException("KYC info cannot be null");
        }
        if (this.kycInfo != null && this.kycInfo.isApproved()) {
            throw new IllegalStateException("KYC already approved");
        }

        this.kycInfo = kycInfo;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new KycSubmittedEvent(userId.getValue()));
    }

    /**
     * 批准KYC
     */
    public void approveKyc() {
        if (kycInfo == null) {
            throw new IllegalStateException("No KYC info to approve");
        }
        
        kycInfo.approve();
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new KycApprovedEvent(userId.getValue()));
    }

    /**
     * 拒绝KYC
     */
    public void rejectKyc(String reason) {
        if (kycInfo == null) {
            throw new IllegalStateException("No KYC info to reject");
        }
        
        kycInfo.reject(reason);
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new KycRejectedEvent(userId.getValue(), reason));
    }

    /**
     * 暂停用户
     */
    public void suspend(String reason) {
        if (status == UserStatus.CLOSED) {
            throw new IllegalStateException("Cannot suspend closed user");
        }
        if (status == UserStatus.SUSPENDED) {
            throw new IllegalStateException("User already suspended");
        }

        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new UserSuspendedEvent(userId.getValue(), reason));
    }

    /**
     * 激活用户
     */
    public void activate() {
        if (status != UserStatus.SUSPENDED) {
            throw new IllegalStateException("Can only activate suspended user");
        }

        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new UserActivatedEvent(userId.getValue()));
    }

    /**
     * 添加角色
     */
    public void addRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.roles.add(role);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 移除角色
     */
    public void removeRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (role == Role.TRADER && roles.size() == 1) {
            throw new IllegalStateException("Cannot remove last role");
        }
        this.roles.remove(role);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否有指定角色
     */
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    /**
     * 检查是否已完成KYC
     */
    public boolean isKycApproved() {
        return kycInfo != null && kycInfo.isApproved();
    }

    /**
     * 检查用户是否活跃
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * 添加领域事件
     */
    private void addDomainEvent(Object event) {
        if (domainEvents == null) {
            domainEvents = new ArrayList<>();
        }
        domainEvents.add(event);
    }

    /**
     * 获取并清空领域事件
     */
    public List<Object> getDomainEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}
