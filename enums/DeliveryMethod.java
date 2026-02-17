package com.example.petmarket.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum DeliveryMethod {
    PICKUP("Odbiór osobisty w sklepie", new BigDecimal("0.00")),
    LOCKER("Kurier InPost (Paczkomat)", new BigDecimal("12.99")),
    COURIER("Kurier InPost (Adres)", new BigDecimal("18.50"));

    private final String description;
    private final BigDecimal price;

}