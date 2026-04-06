package com.trading.user.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 全名值对象
 */
@Getter
@EqualsAndHashCode
public class FullName implements Serializable {
    private final String firstName;
    private final String lastName;

    private FullName(String firstName, String lastName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        if (firstName.length() > 50 || lastName.length() > 50) {
            throw new IllegalArgumentException("Name length cannot exceed 50 characters");
        }
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public static FullName of(String firstName, String lastName) {
        return new FullName(firstName, lastName);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
