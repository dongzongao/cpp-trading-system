package com.trading.user.infrastructure.persistence.po;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户持久化对象
 */
@Data
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class UserPO {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private String status;

    @Column(name = "roles", length = 200)
    private String roles; // 逗号分隔的角色列表

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // KYC 信息
    @Column(name = "kyc_first_name", length = 50)
    private String kycFirstName;

    @Column(name = "kyc_last_name", length = 50)
    private String kycLastName;

    @Column(name = "kyc_id_type", length = 20)
    private String kycIdType;

    @Column(name = "kyc_id_number", length = 30)
    private String kycIdNumber;

    @Column(name = "kyc_country", length = 50)
    private String kycCountry;

    @Column(name = "kyc_province", length = 50)
    private String kycProvince;

    @Column(name = "kyc_city", length = 50)
    private String kycCity;

    @Column(name = "kyc_street", length = 200)
    private String kycStreet;

    @Column(name = "kyc_postal_code", length = 20)
    private String kycPostalCode;

    @Column(name = "kyc_date_of_birth")
    private LocalDateTime kycDateOfBirth;

    @Column(name = "kyc_status", length = 20)
    private String kycStatus;

    @Column(name = "kyc_rejection_reason", length = 500)
    private String kycRejectionReason;

    @Column(name = "kyc_submitted_at")
    private LocalDateTime kycSubmittedAt;

    @Column(name = "kyc_reviewed_at")
    private LocalDateTime kycReviewedAt;
}
