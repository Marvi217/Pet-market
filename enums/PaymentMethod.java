package com.example.petmarket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    TRANSFER("Przelew bankowy"),
    CARD("Karta płatnicza"),
    BLIK("BLIK"),
    CASH_ON_DELIVERY("Płatność przy odbiorze");

    private final String displayName;
}