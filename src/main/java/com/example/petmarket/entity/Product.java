package com.example.petmarket.entity;

import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.enums.PromotionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products")
@Inheritance(strategy = InheritanceType.JOINED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discounted_price", precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Builder.Default
    @Column(name = "average_rating")
    private Double rating = 0.0;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, name = "status")
    private ProductStatus status;

    @Builder.Default
    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @Column(length = 50, unique = true)
    private String sku;

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(length = 100)
    private String dimensions;

    @Column(length = 3000)
    private String ingredients;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @ManyToMany(mappedBy = "products", fetch = FetchType.EAGER)
    @Builder.Default
    private Set<Promotion> promotions = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Integer getReviewCount() {
        return reviews != null ? reviews.size() : 0;
    }

    public boolean isAvailable() {
        return this.status == ProductStatus.ACTIVE &&
                this.stockQuantity != null &&
                this.stockQuantity > 0;
    }

    @Transient
    public String getUnavailableMessage() {
        if (this.status == ProductStatus.SOLDOUT) {
            return "Wyprzedany";
        }
        if (this.status == ProductStatus.ACTIVE && (this.stockQuantity == null || this.stockQuantity <= 0)) {
            return "Wyprzedany";
        }
        return "Niedostępny";
    }

    public void decreaseStock(int quantity) {
        if (stockQuantity < quantity) {
            throw new IllegalStateException("Niewystarczająca ilość produktu w magazynie. " +
                    "Dostępne: " + stockQuantity + ", wymagane: " + quantity);
        }
        stockQuantity -= quantity;
        if (stockQuantity == 0) {
            status = ProductStatus.SOLDOUT;
        }
    }

    public void increaseStock(int quantity) {
        stockQuantity += quantity;
        if (stockQuantity > 0 && status == ProductStatus.SOLDOUT) {
            status = ProductStatus.ACTIVE;
        }
    }

    @Transient
    public boolean isHasDiscount() {
        if (discountedPrice != null && discountedPrice.compareTo(price) < 0) {
            return true;
        }

        return getCurrentPromotion() != null;
    }

    @Transient
    public BigDecimal getOldPrice() {
        if (isHasDiscount()) {
            return price;
        }
        return null;
    }

    @Transient
    public BigDecimal getCurrentPrice() {
        if (discountedPrice != null && discountedPrice.compareTo(price) < 0) {
            return discountedPrice;
        }

        Promotion promotion = getCurrentPromotion();
        if (promotion != null) {
            return promotion.calculateDiscountedPrice(price);
        }

        return price;
    }

    @Transient
    public Integer getDiscountPercent() {
        if (!isHasDiscount()) {
            return 0;
        }

        BigDecimal currentPrice = getCurrentPrice();
        if (currentPrice.compareTo(price) >= 0) {
            return 0;
        }

        BigDecimal discount = price.subtract(currentPrice);
        BigDecimal percent = discount.multiply(new BigDecimal("100"))
                .divide(price, 0, RoundingMode.HALF_UP);
        return percent.intValue();
    }

    @Transient
    public BigDecimal getSavingsAmount() {
        if (!isHasDiscount()) {
            return BigDecimal.ZERO;
        }
        return price.subtract(getCurrentPrice());
    }

    @Transient
    public Promotion getCurrentPromotion() {
        if (promotions == null || promotions.isEmpty()) {
            return null;
        }

        return promotions.stream()
                .filter(Promotion::isCurrentlyActive)
                .filter(p -> (p.getCode() == null || p.getCode().isEmpty()) &&
                        (p.getType() == PromotionType.PERCENTAGE_DISCOUNT ||
                                p.getType() == PromotionType.BUY_X_GET_Y))
                .max((p1, p2) -> {
                    int priorityCompare = Integer.compare(p1.getPriority(), p2.getPriority());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    BigDecimal discount1 = p1.calculateDiscount(price);
                    BigDecimal discount2 = p2.calculateDiscount(price);
                    return discount1.compareTo(discount2);
                })
                .orElse(null);
    }

    @Transient
    public boolean isBuyXGetYPromotion() {
        Promotion promotion = getCurrentPromotion();
        return promotion != null && promotion.getType() == PromotionType.BUY_X_GET_Y;
    }

    @Transient
    public String getBuyXGetYDescription() {
        Promotion promotion = getCurrentPromotion();
        if (promotion == null || promotion.getType() != PromotionType.BUY_X_GET_Y) {
            return null;
        }
        Integer buyQty = promotion.getBuyQuantity();
        Integer getQty = promotion.getGetQuantity();
        BigDecimal discountPct = promotion.getDiscountPercentage();

        if (buyQty == null || getQty == null) {
            return null;
        }

        if (discountPct != null && discountPct.compareTo(new BigDecimal("100")) == 0) {
            return "Kup " + buyQty + " dostań " + getQty + " gratis";
        } else if (discountPct != null) {
            return "Kup " + buyQty + " dostań " + getQty + " za " + discountPct.intValue() + "% ceny";
        } else {
            return "Kup " + buyQty + " dostań " + getQty + " gratis";
        }
    }

    @Transient
    public Integer getBuyXGetYTotalQuantity() {
        Promotion promotion = getCurrentPromotion();
        if (promotion == null || promotion.getType() != PromotionType.BUY_X_GET_Y) {
            return null;
        }
        Integer buyQty = promotion.getBuyQuantity();
        Integer getQty = promotion.getGetQuantity();
        if (buyQty == null || getQty == null) {
            return null;
        }
        return buyQty + getQty;
    }

    @Transient
    public Integer getBuyXGetYBuyQuantity() {
        Promotion promotion = getCurrentPromotion();
        if (promotion == null || promotion.getType() != PromotionType.BUY_X_GET_Y) {
            return null;
        }
        return promotion.getBuyQuantity();
    }

    @Transient
    public BigDecimal getBuyXGetYEffectivePrice() {
        Promotion promotion = getCurrentPromotion();
        if (promotion == null || promotion.getType() != PromotionType.BUY_X_GET_Y) {
            return null;
        }
        Integer buyQty = promotion.getBuyQuantity();
        Integer getQty = promotion.getGetQuantity();
        if (buyQty == null || getQty == null || buyQty <= 0) {
            return null;
        }
        BigDecimal totalPrice = price.multiply(new BigDecimal(buyQty));
        BigDecimal totalQuantity = new BigDecimal(buyQty + getQty);
        return totalPrice.divide(totalQuantity, 2, RoundingMode.HALF_UP);
    }

    @Transient
    public String getShortDescription() {
        if (description == null || description.isEmpty()) {
            return "";
        }
        return description.length() > 100 ?
                description.substring(0, 97) + "..." :
                description;
    }

    @Transient
    public String getMainImageUrl() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        }

        try {
            if (images != null && !images.isEmpty()) {
                return images.stream()
                        .filter(ProductImage::isMain)
                        .findFirst()
                        .map(ProductImage::getImageUrl)
                        .orElseGet(() -> images.get(0).getImageUrl());
            }
        } catch (org.hibernate.LazyInitializationException ignored) {
        }

        return "/images/no-image.png";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return id != null && id.equals(product.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                ", status=" + status +
                '}';
    }
}