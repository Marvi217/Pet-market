package com.example.petmarket.service;

import com.example.petmarket.dto.CartItemDto;
import com.example.petmarket.entity.Product;
import com.example.petmarket.exception.ProductNotFoundException;
import com.example.petmarket.valueobject.Money;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@SessionScope
public class CartService {

    private final Map<Long, Integer> cart = new HashMap<>();
    private final ProductService productService;

    public CartService(ProductService productService) {
        this.productService = productService;
    }

    public List<CartItemDto> getCartItems() {
        List<CartItemDto> items = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            try {
                Product product = productService.getProductById(entry.getKey());
                BigDecimal subtotal = product.getPrice()
                        .multiply(BigDecimal.valueOf(entry.getValue()));
                items.add(new CartItemDto(product, entry.getValue(), subtotal));
            } catch (ProductNotFoundException e) {
                cart.remove(entry.getKey());
            }
        }

        return items;
    }

    public BigDecimal getTotalPrice() {
        BigDecimal total = getCartItems().stream()
                .map(CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Money(total, "PLN").amount();
    }

    public boolean isEmpty() {
        return cart.isEmpty();
    }

    public void clear() {
        cart.clear();
    }

    public Map<Long, Integer> getCartMap() {
        return new HashMap<>(cart);
    }

    public Map<Product, Integer> getProductsInCart() {
        Map<Product, Integer> productsInCart = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Product product = productService.getProductById(entry.getKey());
            productsInCart.put(product, entry.getValue());
        }
        return productsInCart;
    }
}