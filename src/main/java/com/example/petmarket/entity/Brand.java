package com.example.petmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "brands")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 3000)
    private String description;

    @Builder.Default
    private boolean active = true;

    private String logoUrl;
    private String website;
    private String country;

    @OneToMany(mappedBy = "brand")
    @ToString.Exclude
    private List<Product> products;

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
}