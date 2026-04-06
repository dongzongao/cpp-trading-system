package com.trading.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 注册用户命令
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserCommand {
    private String username;
    private String email;
    private String password;
    private String phone;
}
