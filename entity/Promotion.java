package com.example.petmarket.entity;

import com.example.petmarket.enums.PromotionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PromotionType type;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(unique = true, length = 50)
    private String code;

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    private Integer maxUsage;

    @Builder.Default
    @Column(nullable = false)
    private Integer currentUsage = 0;

    private Integer maxUsagePerUser;

    @Builder.Default
    @Column(nullable = false)
    private boolean stackable = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "get_quantity")
    private Integer getQuantity;

    @ManyToMany
    @JoinTable(
            name = "promotion_products",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    private Set<Product> products = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "promotion_categories",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "promotion")
    @Builder.Default
    private Set<Order> orders = new HashSet<>();

    @Column(length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean featured = false;

    @Column(length = 1000)
    private String adminNotes;

    @org.hibernate.annotations.CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public boolean isCurrentlyActive() {
        if (!active) {
            return false;
        }

        LocalDate now = LocalDate.now();

        if (startDate.isAfter(now)) {
            return false;
        }

        if (endDate != null && endDate.isBefore(now)) {
            return false;
        }

        if (maxUsage != null && currentUsage >= maxUsage) {
            return false;
        }

        return true;
    }

    public boolean isApplicableForAmount(BigDecimal orderAmount) {
        if (minOrderAmount == null) {
            return true;
        }
        return orderAmount.compareTo(minOrderAmount) >= 0;
    }

    public BigDecimal calculateDiscount(BigDecimal price) {
        if (!isCurrentlyActive()) {
            return BigDecimal.ZERO;
        }

        switch (type) {
            case PERCENTAGE_DISCOUNT:
                if (discountPercentage != null) {
                    return price.multiply(discountPercentage)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                }

            case FIXED_AMOUNT_DISCOUNT:
                if (discountAmount != null) {
                    return discountAmount.min(price);
                }
                break;

            default:
                return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    public BigDecimal calculateDiscountedPrice(BigDecimal originalPrice) {
        BigDecimal discount = calculateDiscount(originalPrice);
        BigDecimal finalPrice = originalPrice.subtract(discount);

        return finalPrice.max(BigDecimal.ZERO);
    }

    public void incrementUsage() {
        this.currentUsage++;
    }

    public void addProduct(Product product) {
        this.products.add(product);
        product.getPromotions().add(this);
    }

    public void removeProduct(Product product) {
        this.products.remove(product);
        product.getPromotions().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Promotion promotion)) return false;
        return id != null && id.equals(promotion.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Promotion{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", active=" + active +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}