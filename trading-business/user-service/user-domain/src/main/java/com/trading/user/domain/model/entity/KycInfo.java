package com.trading.user.domain.model.entity;

import com.trading.user.domain.model.valueobject.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * KYC信息实体
 */
@Getter
public class KycInfo {
    private FullName fullName;
    private IdNumber idNumber;
    private Address address;
    private LocalDateTime dateOfBirth;
    private KycStatus status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    private KycInfo() {
        this.status = KycStatus.PENDING;
    }

    public static KycInfo create(FullName fullName, IdNumber idNumber, Address address, LocalDateTime dateOfBirth) {
        if (fullName == null) {
            throw new IllegalArgumentException("Full name cannot be null");
        }
        if (idNumber == null) {
            throw new IllegalArgumentException("ID number cannot be null");
        }
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth cannot be null");
        }
        if (dateOfBirth.isAfter(LocalDateTime.now().minusYears(18))) {
            throw new IllegalArgumentException("User must be at least 18 years old");
        }

        KycInfo kycInfo = new KycInfo();
        kycInfo.fullName = fullName;
        kycInfo.idNumber = idNumber;
        kycInfo.address = address;
        kycInfo.dateOfBirth = dateOfBirth;
        kycInfo.submittedAt = LocalDateTime.now();
        return kycInfo;
    }

    public void approve() {
        if (status != KycStatus.PENDING) {
            throw new IllegalStateException("Can only approve pending KYC");
        }
        this.status = KycStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (status != KycStatus.PENDING) {
            throw new IllegalStateException("Can only reject pending KYC");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason cannot be empty");
        }
        this.status = KycStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == KycStatus.PENDING;
    }

    public boolean isApproved() {
        return status == KycStatus.APPROVED;
    }
}
