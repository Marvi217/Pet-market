package com.example.petmarket.entity;

import com.example.petmarket.service.interfaces.ICartItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_cart_items")
public class UserCartItem implements ICartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private UserCart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    public UserCartItem(UserCart cart, Product product, Integer quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return getTotalPrice();
    }
    @Override
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
}