package com.example.petmarket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewStatus {
    PENDING("Oczekuje na moderację", "Opinia została dodana i czeka na zatwierdzenie przez moderatora"),
    APPROVED("Zatwierdzona", "Opinia została zatwierdzona i jest widoczna publicznie"),
    REJECTED("Odrzucona", "Opinia została odrzucona przez moderatora");

    private final String displayName;
    private final String description;

    public boolean isPublic() {
        return this == APPROVED;
    }

    public String getColor() {
        return switch (this) {
            case PENDING -> "warning";
            case APPROVED -> "success";
            case REJECTED -> "danger";
        };
    }

    public String getIcon() {
        return switch (this) {
            case PENDING -> "fa-clock";
            case APPROVED -> "fa-check-circle";
            case REJECTED -> "fa-times-circle";
        };
    }
}