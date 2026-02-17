package com.example.petmarket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {
    ACTIVE("Aktywny"),
    INACTIVE("Nieaktywny"),
    DRAFT("Szkic"),
    SOLDOUT("Wyprzedany");

    private final String displayName;
}