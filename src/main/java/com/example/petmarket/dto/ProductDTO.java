package com.example.petmarket.dto;

import com.example.petmarket.enums.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Nazwa produktu jest wymagana")
    @Size(min = 3, max = 200, message = "Nazwa musi mieć od 3 do 200 znaków")
    private String name;

    @Size(max = 2000, message = "Opis może mieć maksymalnie 2000 znaków")
    private String description;

    @NotNull(message = "Cena jest wymagana")
    @DecimalMin(value = "0.01", message = "Cena musi być większa od 0")
    @Digits(integer = 8, fraction = 2, message = "Nieprawidłowy format ceny")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Cena promocyjna nie może być ujemna")
    @Digits(integer = 8, fraction = 2, message = "Nieprawidłowy format ceny")
    private BigDecimal discountedPrice;

    @NotNull(message = "Stan magazynowy jest wymagany")
    @Min(value = 0, message = "Stan magazynowy nie może być ujemny")
    private Integer stockQuantity;

    @NotNull(message = "Status jest wymagany")
    private ProductStatus status;

    @NotNull(message = "Kategoria jest wymagana")
    private Long categoryId;

    @NotNull(message = "Subkategoria jest wymagana")
    private Long subcategoryId;

    @NotNull(message = "Marka jest wymagana")
    private Long brandId;

    @Size(max = 50, message = "SKU może mieć maksymalnie 50 znaków")
    private String sku;

    @DecimalMin(value = "0.00", message = "Waga nie może być ujemna")
    private BigDecimal weight;

    @Size(max = 100, message = "Wymiary mogą mieć maksymalnie 100 znaków")
    private String dimensions;
}