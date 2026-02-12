package com.example.petmarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandDTO {

    private Long id;

    @NotBlank(message = "Nazwa marki jest wymagana")
    @Size(min = 2, max = 100, message = "Nazwa marki musi mieć od 2 do 100 znaków")
    private String name;

    @Size(max = 1000, message = "Opis nie może przekraczać 1000 znaków")
    private String description;

    @Size(max = 200, message = "Adres strony nie może przekraczać 200 znaków")
    private String website;

    @Size(max = 100, message = "Kraj nie może przekraczać 100 znaków")
    private String country;

    private String logoUrl;

    @Builder.Default
    private boolean active = true;
}