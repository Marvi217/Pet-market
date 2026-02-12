package com.example.petmarket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("Oczekuje na płatność", "Płatność nie została jeszcze dokonana"),
    PROCESSING("Przetwarzanie", "Płatność jest przetwarzana"),
    PAID("Opłacone", "Płatność została zrealizowana pomyślnie"),
    FAILED("Nieudana", "Płatność nie powiodła się"),
    CANCELLED("Anulowana", "Płatność została anulowana"),
    REFUNDED("Zwrócono", "Pieniądze zostały zwrócone klientowi"),
    PARTIALLY_REFUNDED("Częściowy zwrot", "Część kwoty została zwrócona"),
    CHARGEBACK("Chargeback", "Klient zgłosił reklamację do banku");

    private final String displayName;
    private final String description;


    public String getColor() {
        return switch (this) {
            case PENDING, PROCESSING -> "warning";
            case PAID -> "success";
            case FAILED, CANCELLED, CHARGEBACK -> "danger";
            case REFUNDED, PARTIALLY_REFUNDED -> "info";
        };
    }

    public String getIcon() {
        return switch (this) {
            case PENDING -> "fa-clock";
            case PROCESSING -> "fa-spinner";
            case PAID -> "fa-check-circle";
            case FAILED -> "fa-times-circle";
            case CANCELLED -> "fa-ban";
            case REFUNDED, PARTIALLY_REFUNDED -> "fa-undo";
            case CHARGEBACK -> "fa-exclamation-triangle";
        };
    }
}