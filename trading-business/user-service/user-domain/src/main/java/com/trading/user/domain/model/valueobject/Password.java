package com.trading.user.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * 密码值对象
 * 使用BCrypt加密存储
 */
@Getter
@EqualsAndHashCode
public class Password implements Serializable {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    private final String value;
    private final boolean encrypted;

    private Password(String value, boolean encrypted) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.value = value;
        this.encrypted = encrypted;
    }

    public static Password fromRaw(String rawPassword) {
        if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new IllegalArgumentException(
                "Password must be at least 8 characters and contain uppercase, lowercase, digit, and special character");
        }
        String encrypted = encoder.encode(rawPassword);
        return new Password(encrypted, true);
    }

    public static Password fromEncrypted(String encryptedPassword) {
        return new Password(encryptedPassword, true);
    }

    public boolean matches(String rawPassword) {
        if (!encrypted) {
            throw new IllegalStateException("Cannot match against unencrypted password");
        }
        return encoder.matches(rawPassword, value);
    }

    @Override
    public String toString() {
        return "******";
    }
}
