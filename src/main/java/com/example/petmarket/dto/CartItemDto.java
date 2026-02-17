package com.example.petmarket.dto;

import com.example.petmarket.entity.Product;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDto {
    private Product product;
    private Integer quantity;
    private BigDecimal subtotal;

    public CartItemDto(Product product, Integer quantity, BigDecimal subtotal) {
        this.product = product;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

}