package com.trading.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 登录命令
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginCommand {
    private String username;
    private String password;
}
