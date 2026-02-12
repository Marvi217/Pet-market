package com.example.petmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class Cart {
    private List<CartItem> items = new ArrayList<>();

    public void addItem(Product product, Integer quantity) {
        if (product == null || product.getId() == null) return;

        Optional<CartItem> existingItem = items.stream()
                .filter(item -> item.getProduct() != null &&
                        item.getProduct().getId() != null &&
                        item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + (quantity != null ? quantity : 1));
        } else {
            items.add(new CartItem(product, quantity != null ? quantity : 1));
        }
    }

    public void removeItem(Long productId) {
        items.removeIf(item -> item.getProduct().getId().equals(productId));
    }


    public BigDecimal getTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Integer getTotalItems() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}