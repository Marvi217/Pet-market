package com.example.petmarket.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    USER("Użytkownik"),
    ADMIN("Administrator"),
    SUPERUSER("Superużytkownik");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }
}