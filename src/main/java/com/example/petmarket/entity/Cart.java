package com.example.petmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class Cart {
    private List<CartItem> items = new ArrayList<>();
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("199");
    private static final BigDecimal DELIVERY_COST = new BigDecimal("15");

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

    public BigDecimal getSubtotal() {
        return getTotal();
    }

    public BigDecimal getTotalDiscount() {
        return items.stream()
                .map(item -> {
                    if (item.getProduct() == null) {
                        return BigDecimal.ZERO;
                    }

                    if (item.getProduct().isBuyXGetYPromotion() && item.isPromoApplied()) {
                        BigDecimal regularTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                        BigDecimal actualTotal = item.getTotalPrice();
                        return regularTotal.subtract(actualTotal);
                    }

                    if (item.getProduct().isHasDiscount() && !item.getProduct().isBuyXGetYPromotion()) {
                        BigDecimal oldPrice = item.getProduct().getOldPrice();
                        BigDecimal currentPrice = item.getProduct().getPrice();
                        if (oldPrice != null && currentPrice != null) {
                            return oldPrice.subtract(currentPrice).multiply(BigDecimal.valueOf(item.getQuantity()));
                        }
                    }

                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getDeliveryCost() {
        if (getSubtotal().compareTo(FREE_DELIVERY_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        return DELIVERY_COST;
    }

    public BigDecimal getTotalAmount() {
        return getSubtotal().add(getDeliveryCost());
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