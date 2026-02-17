package com.example.petmarket.dto;

import com.example.petmarket.entity.CartItem;
import com.example.petmarket.enums.DeliveryMethod;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class CartViewModel {
    private final List<CartItem> items;
    private final BigDecimal subtotal;
    private final BigDecimal deliveryCost;
    private final BigDecimal totalAmount;

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("199");

    public CartViewModel(List<CartItem> items, BigDecimal subtotal) {
        this.items = items;
        this.subtotal = subtotal;
        this.deliveryCost = calculateDeliveryCost(subtotal);
        this.totalAmount = subtotal.add(this.deliveryCost);
    }

    private BigDecimal calculateDeliveryCost(BigDecimal subtotal) {
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        return DeliveryMethod.COURIER.getPrice();
    }

}