package com.example.petmarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryDTO {

    private Long id;

    @NotBlank(message = "Nazwa kategorii jest wymagana")
    @Size(min = 2, max = 100, message = "Nazwa musi mieć od 2 do 100 znaków")
    private String name;

    @NotBlank(message = "Slug jest wymagany")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug może zawierać tylko małe litery, cyfry i myślniki")
    @Size(min = 2, max = 100, message = "Slug musi mieć od 2 do 100 znaków")
    private String slug;

    @Size(max = 4, message = "Ikona może mieć maksymalnie 4 znaki")
    private String icon;

    @Size(max = 500, message = "Opis może mieć maksymalnie 500 znaków")
    private String description;

    private String imageUrl;

    private boolean active = true;
}