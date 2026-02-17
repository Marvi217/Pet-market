package com.example.petmarket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("Użytkownik"),
    ADMIN("Administrator"),
    SUPERUSER("Superużytkownik");

    private final String displayName;

}