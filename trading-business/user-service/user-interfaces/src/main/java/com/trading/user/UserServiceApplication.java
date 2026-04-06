package com.trading.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * User Service Application
 * 
 * @author Trading System Team
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.trading.user",
    "com.trading.common"
})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.java, args);
    }
}
