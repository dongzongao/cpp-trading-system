package com.trading.user.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 地址值对象
 */
@Getter
@EqualsAndHashCode
public class Address implements Serializable {
    private final String country;
    private final String province;
    private final String city;
    private final String street;
    private final String postalCode;

    private Address(String country, String province, String city, String street, String postalCode) {
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be null or empty");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street cannot be null or empty");
        }
        this.country = country.trim();
        this.province = province != null ? province.trim() : "";
        this.city = city.trim();
        this.street = street.trim();
        this.postalCode = postalCode != null ? postalCode.trim() : "";
    }

    public static Address of(String country, String province, String city, String street, String postalCode) {
        return new Address(country, province, city, street, postalCode);
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street);
        if (!city.isEmpty()) sb.append(", ").append(city);
        if (!province.isEmpty()) sb.append(", ").append(province);
        if (!postalCode.isEmpty()) sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFullAddress();
    }
}
