package com.trading.user.application.assembler;

import com.trading.user.application.dto.UserDTO;
import com.trading.user.domain.model.aggregate.User;
import com.trading.user.domain.model.valueobject.Role;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 用户组装器
 */
@Component
public class UserAssembler {
    
    public UserDTO toDTO(User user) {
        return UserDTO.builder()
            .userId(user.getUserId().getValue())
            .username(user.getUsername().getValue())
            .email(user.getEmail().getValue())
            .phone(user.getPhone() != null ? user.getPhone().getValue() : null)
            .status(user.getStatus().name())
            .kycStatus(user.getKycInfo() != null ? user.getKycInfo().getStatus().name() : null)
            .roles(user.getRoles().stream().map(Role::name).collect(Collectors.toSet()))
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
