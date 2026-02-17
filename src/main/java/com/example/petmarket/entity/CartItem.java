package com.example.petmarket.entity;

import com.example.petmarket.service.interfaces.ICartItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements ICartItem {
    private Product product;
    private Integer quantity;

    public BigDecimal getPrice() {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        return product.getCurrentPrice();
    }

    public BigDecimal getTotalPrice() {
        if (product == null || quantity == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal unitPrice = product.getCurrentPrice();

        if (product.isBuyXGetYPromotion()) {
            Integer buyQty = product.getBuyXGetYBuyQuantity();
            Integer totalQty = product.getBuyXGetYTotalQuantity();

            if (buyQty != null && totalQty != null && totalQty > 0 && quantity >= totalQty) {
                int remainingItems = quantity - totalQty;
                BigDecimal originalPrice = product.getPrice();
                BigDecimal promoPrice = originalPrice.multiply(BigDecimal.valueOf(buyQty));
                BigDecimal regularPrice = originalPrice.multiply(BigDecimal.valueOf(remainingItems));

                return promoPrice.add(regularPrice);
            }
        }

        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getSubtotal() {
        return getTotalPrice();
    }

    public boolean isPromoApplied() {
        if (product == null || !product.isBuyXGetYPromotion()) {
            return false;
        }
        Integer totalQty = product.getBuyXGetYTotalQuantity();
        return totalQty != null && quantity >= totalQty;
    }
}