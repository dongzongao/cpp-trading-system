package com.trading.user.domain.service;

import com.trading.user.domain.model.valueobject.Email;
import com.trading.user.domain.model.valueobject.Username;
import com.trading.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户重复检查服务
 */
@Service
@RequiredArgsConstructor
public class UserDuplicationCheckService {
    private final UserRepository userRepository;

    /**
     * 检查用户名和邮箱是否重复
     */
    public void checkDuplication(Username username, Email email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username.getValue());
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email.getValue());
        }
    }
}
