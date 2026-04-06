package com.trading.user.domain.service;

import com.trading.user.domain.model.aggregate.User;
import com.trading.user.domain.model.valueobject.Password;
import com.trading.user.domain.model.valueobject.Username;
import com.trading.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户认证领域服务
 */
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    /**
     * 认证用户
     * @return JWT Token
     */
    public String authenticate(Username username, String rawPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!user.getPassword().matches(rawPassword)) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new IllegalStateException("User is not active");
        }

        return jwtTokenService.generateToken(user);
    }
}
