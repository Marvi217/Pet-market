package com.example.petmarket.service.interfaces;

import com.example.petmarket.dto.CartItemDto;
import com.example.petmarket.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ICartService {

    List<CartItemDto> getCartItems();

    BigDecimal getTotalPrice();

    boolean isEmpty();

    void clear();

    Map<Product, Integer> getProductsInCart();
}