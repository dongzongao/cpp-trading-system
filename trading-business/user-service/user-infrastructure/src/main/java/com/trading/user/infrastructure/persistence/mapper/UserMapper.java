package com.trading.user.infrastructure.persistence.mapper;

import com.trading.user.domain.model.aggregate.User;
import com.trading.user.domain.model.entity.KycInfo;
import com.trading.user.domain.model.valueobject.*;
import com.trading.user.infrastructure.persistence.po.UserPO;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户映射器
 */
@Component
public class UserMapper {

    /**
     * PO转领域对象
     */
    public User toDomain(UserPO po) {
        try {
            User user = User.class.getDeclaredConstructor().newInstance();
            
            // 设置基本字段
            setField(user, "userId", UserId.of(po.getUserId()));
            setField(user, "username", Username.of(po.getUsername()));
            setField(user, "email", Email.of(po.getEmail()));
            setField(user, "password", Password.fromEncrypted(po.getPassword()));
            
            if (po.getPhone() != null) {
                setField(user, "phone", Phone.of(po.getPhone()));
            }
            
            setField(user, "status", UserStatus.valueOf(po.getStatus()));
            setField(user, "createdAt", po.getCreatedAt());
            setField(user, "updatedAt", po.getUpdatedAt());
            
            // 设置角色
            Set<Role> roles = Arrays.stream(po.getRoles().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Role::valueOf)
                .collect(Collectors.toSet());
            setField(user, "roles", roles);
            
            // 设置KYC信息
            if (po.getKycStatus() != null) {
                KycInfo kycInfo = createKycInfo(po);
                setField(user, "kycInfo", kycInfo);
            }
            
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map UserPO to User", e);
        }
    }

    /**
     * 领域对象转PO
     */
    public UserPO toPO(User user) {
        UserPO po = new UserPO();
        
        po.setUserId(user.getUserId().getValue());
        po.setUsername(user.getUsername().getValue());
        po.setEmail(user.getEmail().getValue());
        po.setPassword(user.getPassword().getValue());
        
        if (user.getPhone() != null) {
            po.setPhone(user.getPhone().getValue());
        }
        
        po.setStatus(user.getStatus().name());
        po.setCreatedAt(user.getCreatedAt());
        po.setUpdatedAt(user.getUpdatedAt());
        
        // 角色转字符串
        String rolesStr = user.getRoles().stream()
            .map(Role::name)
            .collect(Collectors.joining(","));
        po.setRoles(rolesStr);
        
        // KYC信息
        if (user.getKycInfo() != null) {
            KycInfo kyc = user.getKycInfo();
            po.setKycFirstName(kyc.getFullName().getFirstName());
            po.setKycLastName(kyc.getFullName().getLastName());
            po.setKycIdType(kyc.getIdNumber().getType().name());
            po.setKycIdNumber(kyc.getIdNumber().getValue());
            po.setKycCountry(kyc.getAddress().getCountry());
            po.setKycProvince(kyc.getAddress().getProvince());
            po.setKycCity(kyc.getAddress().getCity());
            po.setKycStreet(kyc.getAddress().getStreet());
            po.setKycPostalCode(kyc.getAddress().getPostalCode());
            po.setKycDateOfBirth(kyc.getDateOfBirth());
            po.setKycStatus(kyc.getStatus().name());
            po.setKycRejectionReason(kyc.getRejectionReason());
            po.setKycSubmittedAt(kyc.getSubmittedAt());
            po.setKycReviewedAt(kyc.getReviewedAt());
        }
        
        return po;
    }

    private KycInfo createKycInfo(UserPO po) {
        try {
            FullName fullName = FullName.of(po.getKycFirstName(), po.getKycLastName());
            IdNumber idNumber = IdNumber.of(po.getKycIdNumber(), IdType.valueOf(po.getKycIdType()));
            Address address = Address.of(
                po.getKycCountry(),
                po.getKycProvince(),
                po.getKycCity(),
                po.getKycStreet(),
                po.getKycPostalCode()
            );
            
            KycInfo kycInfo = KycInfo.create(fullName, idNumber, address, po.getKycDateOfBirth());
            
            // 设置状态
            setField(kycInfo, "status", KycStatus.valueOf(po.getKycStatus()));
            setField(kycInfo, "rejectionReason", po.getKycRejectionReason());
            setField(kycInfo, "submittedAt", po.getKycSubmittedAt());
            setField(kycInfo, "reviewedAt", po.getKycReviewedAt());
            
            return kycInfo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KycInfo", e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
