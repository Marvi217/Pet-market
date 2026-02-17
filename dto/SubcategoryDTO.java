package com.example.petmarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubcategoryDTO {

    private Long id;

    @NotBlank(message = "Nazwa subkategorii jest wymagana")
    @Size(min = 2, max = 100, message = "Nazwa musi mieć od 2 do 100 znaków")
    private String name;

    @NotBlank(message = "Slug jest wymagany")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug może zawierać tylko małe litery, cyfry i myślniki")
    @Size(min = 2, max = 100, message = "Slug musi mieć od 2 do 100 znaków")
    private String slug;

    @Size(max = 50, message = "Ikona może mieć maksymalnie 50 znaków")
    private String icon;

    @Size(max = 500, message = "Opis może mieć maksymalnie 500 znaków")
    private String description;

    @NotNull(message = "Kategoria nadrzędna jest wymagana")
    private Long categoryId;

    private String imageUrl;

    private boolean active = true;

    private Integer displayOrder = 0;
}