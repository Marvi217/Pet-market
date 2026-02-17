package com.example.petmarket.dto;

import com.example.petmarket.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class CartItemDto {
    private Product product;
    private Integer quantity;
    private BigDecimal subtotal;
}