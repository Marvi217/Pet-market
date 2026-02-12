package com.example.petmarket.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("Oczekujące", "Zamówienie zostało złożone i oczekuje na przetworzenie"),
    CONFIRMED("Potwierdzone", "Zamówienie zostało potwierdzone"),
    PROCESSING("W realizacji", "Zamówienie jest przygotowywane do wysyłki"),
    PACKED("Zapakowane", "Zamówienie zostało zapakowane"),
    SHIPPED("Wysłane", "Zamówienie zostało wysłane"),
    IN_TRANSIT("W transporcie", "Zamówienie jest w drodze do klienta"),
    OUT_FOR_DELIVERY("W dostawie", "Zamówienie jest dostarczane przez kuriera"),
    DELIVERED("Dostarczone", "Zamówienie zostało dostarczone"),
    CANCELLED("Anulowane", "Zamówienie zostało anulowane"),
    RETURNED("Zwrócone", "Zamówienie zostało zwrócone"),
    REFUNDED("Zwrot pieniędzy", "Zwrócono pieniądze za zamówienie"),
    FAILED("Nieudane", "Zamówienie nie powiodło się");

    private final String displayName;
    private final String description;

    OrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Sprawdza czy status oznacza zakończone zamówienie
     */
    public boolean isFinalStatus() {
        return this == DELIVERED ||
                this == CANCELLED ||
                this == RETURNED ||
                this == REFUNDED ||
                this == FAILED;
    }

    /**
     * Sprawdza czy zamówienie można anulować w tym statusie
     */
    public boolean isCancellable() {
        return this == PENDING ||
                this == CONFIRMED ||
                this == PROCESSING;
    }

    /**
     * Sprawdza czy zamówienie można edytować w tym statusie
     */
    public boolean isEditable() {
        return this == PENDING;
    }

    /**
     * Zwraca kolor do wyświetlania w interfejsie
     */
    public String getColor() {
        return switch (this) {
            case PENDING -> "warning";
            case CONFIRMED, PROCESSING, PACKED -> "info";
            case SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY -> "primary";
            case DELIVERED -> "success";
            case CANCELLED, FAILED -> "danger";
            case RETURNED, REFUNDED -> "secondary";
            default -> "secondary";
        };
    }

    /**
     * Zwraca ikonę Font Awesome
     */
    public String getIcon() {
        return switch (this) {
            case PENDING -> "fa-clock";
            case CONFIRMED -> "fa-check-circle";
            case PROCESSING -> "fa-cog";
            case PACKED -> "fa-box";
            case SHIPPED, IN_TRANSIT -> "fa-truck";
            case OUT_FOR_DELIVERY -> "fa-shipping-fast";
            case DELIVERED -> "fa-check-double";
            case CANCELLED -> "fa-times-circle";
            case RETURNED -> "fa-undo";
            case REFUNDED -> "fa-money-bill-wave";
            case FAILED -> "fa-exclamation-triangle";
            default -> "fa-question-circle";
        };
    }

    /**
     * Pobiera następny logiczny status
     */
    public OrderStatus getNextStatus() {
        return switch (this) {
            case PENDING -> CONFIRMED;
            case CONFIRMED -> PROCESSING;
            case PROCESSING -> PACKED;
            case PACKED -> SHIPPED;
            case SHIPPED -> IN_TRANSIT;
            case IN_TRANSIT -> OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            default -> null; // Brak następnego statusu
        };
    }
}