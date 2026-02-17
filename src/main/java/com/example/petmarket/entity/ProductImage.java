package com.example.petmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean isMain = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(length = 200)
    private String altText;

    @Column(length = 200)
    private String title;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    private Long fileSize;

    @Column(length = 50)
    private String mimeType;

    private Integer width;

    private Integer height;

    @Column(length = 500)
    private String thumbnailUrl;

    @Transient
    public String getFormat() {
        if (imageUrl == null) {
            return null;
        }
        int lastDot = imageUrl.lastIndexOf('.');
        return lastDot >= 0 ? imageUrl.substring(lastDot + 1).toUpperCase() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductImage that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProductImage{" +
                "id=" + id +
                ", imageUrl='" + imageUrl + '\'' +
                ", isMain=" + isMain +
                ", displayOrder=" + displayOrder +
                '}';
    }
}