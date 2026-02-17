package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.Product;
import java.math.BigDecimal;

public interface ICartItem {
    Product getProduct();
    Integer getQuantity();
    void setQuantity(Integer quantity);
    BigDecimal getSubtotal();
    BigDecimal getTotalPrice();
}
