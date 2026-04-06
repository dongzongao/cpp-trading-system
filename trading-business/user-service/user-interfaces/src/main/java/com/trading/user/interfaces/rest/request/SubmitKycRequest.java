package com.trading.user.interfaces.rest.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.time.LocalDateTime;

/**
 * 提交KYC请求
 */
@Data
public class SubmitKycRequest {
    @NotBlank(message = "First name cannot be blank")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    private String lastName;

    @NotBlank(message = "ID type cannot be blank")
    private String idType;

    @NotBlank(message = "ID number cannot be blank")
    private String idNumber;

    @NotBlank(message = "Country cannot be blank")
    private String country;

    private String province;

    @NotBlank(message = "City cannot be blank")
    private String city;

    @NotBlank(message = "Street cannot be blank")
    private String street;

    private String postalCode;

    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    private LocalDateTime dateOfBirth;
}
