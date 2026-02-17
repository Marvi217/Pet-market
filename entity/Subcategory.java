package com.example.petmarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "subcategories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "category_id"},
                        name = "uk_subcategory_name_category")
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 50)
    private String icon;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private Category category;

    @OneToMany(mappedBy = "subcategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Product> products = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Subcategory(String name, String slug, String icon, String description, Category category) {
        this.name = name;
        this.slug = slug;
        this.icon = icon;
        this.description = description;
        this.category = category;
        this.products = new ArrayList<>();
        this.active = true;
        this.displayOrder = 0;
    }

    public Subcategory(String name, String slug, String icon, Category category) {
        this(name, slug, icon, null, category);
    }

    public void addProduct(Product product) {
        if (products == null) {
            products = new ArrayList<>();
        }
        products.add(product);
        product.setSubcategory(this);
    }

    public void removeProduct(Product product) {
        if (products != null) {
            products.remove(product);
            product.setSubcategory(null);
        }
    }

    public int getProductCount() {
        return products != null ? products.size() : 0;
    }

    public long getAvailableProductCount() {
        if (products == null) {
            return 0;
        }
        return products.stream()
                .filter(Product::isAvailable)
                .count();
    }

    public boolean hasProducts() {
        return products != null && !products.isEmpty();
    }

    public List<Product> getAvailableProducts() {
        if (products == null) {
            return new ArrayList<>();
        }
        return products.stream()
                .filter(Product::isAvailable)
                .toList();
    }

    public boolean isEmpty() {
        return !hasProducts();
    }

    public boolean canBeDeleted() {
        return isEmpty();
    }

    public static String generateSlug(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase()
                .replace("ą", "a")
                .replace("ć", "c")
                .replace("ę", "e")
                .replace("ł", "l")
                .replace("ń", "n")
                .replace("ó", "o")
                .replace("ś", "s")
                .replace("ź", "z")
                .replace("ż", "z")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    @PrePersist
    @PreUpdate
    private void generateSlugIfNeeded() {
        if (this.slug == null || this.slug.isEmpty()) {
            this.slug = generateSlug(this.name);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subcategory)) return false;
        Subcategory that = (Subcategory) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Subcategory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", active=" + active +
                ", displayOrder=" + displayOrder +
                '}';
    }
}