package com.example.petmarket.dto;

import com.example.petmarket.enums.PromotionType;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PromotionDTO {

    private Long id;

    @NotBlank(message = "Nazwa promocji jest wymagana")
    @Size(min = 3, max = 200, message = "Nazwa musi mieć od 3 do 200 znaków")
    private String name;

    @Size(max = 1000, message = "Opis może mieć maksymalnie 1000 znaków")
    private String description;

    @NotNull(message = "Typ promocji jest wymagany")
    private PromotionType type;

    @DecimalMin(value = "0.01", message = "Procent zniżki musi być większy od 0")
    @DecimalMax(value = "100.00", message = "Procent zniżki nie może przekraczać 100")
    private BigDecimal discountPercentage;

    @DecimalMin(value = "0.01", message = "Kwota zniżki musi być większa od 0")
    private BigDecimal discountAmount;

    @NotNull(message = "Data rozpoczęcia jest wymagana")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private boolean active = true;

    @Size(max = 50, message = "Kod może mieć maksymalnie 50 znaków")
    private String code;

    @DecimalMin(value = "0.00", message = "Minimalna kwota zamówienia nie może być ujemna")
    private BigDecimal minOrderAmount;

    @Min(value = 0, message = "Maksymalne użycie nie może być ujemne")
    private Integer maxUsage;

    private List<Long> productIds;
}