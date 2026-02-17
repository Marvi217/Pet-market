package com.example.petmarket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    TRANSFER("Przelew bankowy", new BigDecimal("0.00")),
    CARD("Karta płatnicza", new BigDecimal("0.00")),
    BLIK("BLIK", new BigDecimal("0.00")),
    CASH_ON_DELIVERY("Płatność przy odbiorze", new BigDecimal("5.00"));

    private final String displayName;
    private final BigDecimal price;
}