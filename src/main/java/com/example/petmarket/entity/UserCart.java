package com.example.petmarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_carts")
public class UserCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserCartItem> items = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserCart(User user) {
        this.user = user;
    }

    public void addItem(Product product, Integer quantity) {
        if (product == null || product.getId() == null) return;

        Optional<UserCartItem> existingItem = items.stream()
                .filter(item -> item.getProduct() != null &&
                        item.getProduct().getId() != null &&
                        item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            UserCartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + (quantity != null ? quantity : 1));
        } else {
            UserCartItem newItem = new UserCartItem(this, product, quantity != null ? quantity : 1);
            items.add(newItem);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void removeItem(Long productId) {
        items.removeIf(item -> item.getProduct().getId().equals(productId));
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(UserCartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Integer getTotalItems() {
        return items.stream()
                .mapToInt(UserCartItem::getQuantity)
                .sum();
    }

    public void clear() {
        items.clear();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}