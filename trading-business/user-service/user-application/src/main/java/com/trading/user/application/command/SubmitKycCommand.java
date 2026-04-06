package com.trading.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 提交KYC命令
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitKycCommand {
    private String userId;
    private String firstName;
    private String lastName;
    private String idType;
    private String idNumber;
    private String country;
    private String province;
    private String city;
    private String street;
    private String postalCode;
    private LocalDateTime dateOfBirth;
}
