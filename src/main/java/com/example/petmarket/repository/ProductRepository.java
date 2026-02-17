package com.example.petmarket.repository;

import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
            "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:availableOnly = false OR p.status = 'ACTIVE')")
    List<Product> searchProducts(
            @Param("query") String query,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("availableOnly") Boolean availableOnly
    );

    List<Product> findByCategoryId(Long categoryId);
    List<Product> findBySubcategoryId(Long subcategoryId);
    List<Product> findByBrandId(Long brandId);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 AND p.stockQuantity <= 10")
    Page<Product> findLowStockProductsPage(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity > 0 AND p.stockQuantity <= 10")
    long countLowStockProducts();

    long countByStockQuantity(Integer stockQuantity);

    List<Product> findByNameContainingIgnoreCase(String name);

    @Query("SELECT p FROM Product p WHERE " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId) AND " +
            "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
            "(:status IS NULL OR p.status = :status)")
    Page<Product> filterProducts(
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId,
            @Param("brandId") Long brandId,
            @Param("status") ProductStatus status,
            Pageable pageable);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.product.id = :productId")
    boolean isProductInOrders(@Param("productId") Long productId);
    List<Product> findByCategory(Category category);

    List<Product> findByStatus(ProductStatus status);
    boolean existsBySku(String sku);


    @Query("SELECT p.rating, COUNT(p) FROM Product p WHERE p.category.slug = :slug GROUP BY p.rating")
    List<Object[]> countProductsByRatingForCategory(@Param("slug") String slug);

    @Query("SELECT p FROM Product p WHERE " +
            "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:availableOnly = false OR p.status = 'ACTIVE')")
    Page<Product> searchProducts(@Param("query") String query,
                                 @Param("categoryId") Long categoryId,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("availableOnly") boolean availableOnly,
                                 Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category = :category " +
            "AND (:subcategoryIds IS NULL OR p.subcategory.id IN :subcategoryIds) " +
            "AND (:brandIds IS NULL OR p.brand.id IN :brandIds) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND p.status != com.example.petmarket.enums.ProductStatus.DRAFT")
    Page<Product> findFilteredProductsAdvanced(
            @Param("category") Category category,
            @Param("subcategoryIds") List<Long> subcategoryIds,
            @Param("brandIds") List<Long> brandIds,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable);

    @Query("SELECT p.brand.id, p.brand.name, COUNT(p) FROM Product p WHERE p.category = :category GROUP BY p.brand.id, p.brand.name ORDER BY p.brand.name")
    List<Object[]> findBrandsWithCountByCategory(@Param("category") Category category);

    @Query("SELECT p.subcategory.name, COUNT(p), p.subcategory.slug FROM Product p WHERE p.category.slug = :categorySlug GROUP BY p.subcategory.name, p.subcategory.slug")
    List<Object[]> findSubcategoriesByCategorySlug(@Param("categorySlug") String categorySlug);

    @Query("SELECT p.brand.id, p.brand.name, COUNT(p) FROM Product p WHERE p.category = :category " +
            "AND (:subcategoryIds IS NULL OR p.subcategory.id IN :subcategoryIds) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND p.status != com.example.petmarket.enums.ProductStatus.DRAFT " +
            "GROUP BY p.brand.id, p.brand.name ORDER BY p.brand.name")
    List<Object[]> findBrandsWithCountByCategoryFiltered(
            @Param("category") Category category,
            @Param("subcategoryIds") List<Long> subcategoryIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT p.subcategory.id, p.subcategory.name, COUNT(p) FROM Product p WHERE p.category.slug = :categorySlug " +
            "AND (:brandIds IS NULL OR p.brand.id IN :brandIds) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND p.status != com.example.petmarket.enums.ProductStatus.DRAFT " +
            "GROUP BY p.subcategory.id, p.subcategory.name")
    List<Object[]> findSubcategoryCountsFiltered(
            @Param("categorySlug") String categorySlug,
            @Param("brandIds") List<Long> brandIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);
}