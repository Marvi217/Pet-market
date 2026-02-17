package com.example.petmarket.repository;

import com.example.petmarket.entity.Subcategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    Page<Subcategory> findByCategoryId(Long categoryId, Pageable pageable);


    List<Subcategory> findByActiveTrue();

    List<Subcategory> findByCategoryIdAndActiveTrue(Long categoryId);


    List<Subcategory> findByCategoryIdOrderByDisplayOrderAsc(Long categoryId);

    boolean existsByNameAndCategoryId(String name, Long categoryId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Product p WHERE p.subcategory.id = :subcategoryId")
    boolean hasProducts(@Param("subcategoryId") Long subcategoryId);

    long countByActiveTrue();

    long countByCategoryId(Long categoryId);

    long countByCategoryIdAndActiveTrue(Long categoryId);

    @Query("SELECT s.id, s.name, COUNT(p) " +
            "FROM Subcategory s " +
            "LEFT JOIN Product p ON p.subcategory.id = s.id " +
            "WHERE s.category.id = :categoryId " +
            "GROUP BY s.id, s.name " +
            "ORDER BY s.displayOrder ASC")
    List<Object[]> findSubcategoriesWithProductCount(@Param("categoryId") Long categoryId);

    @Query("SELECT s FROM Subcategory s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Subcategory> searchSubcategories(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Subcategory s WHERE s.category.id = :categoryId AND (" +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Subcategory> searchSubcategoriesInCategory(
            @Param("categoryId") Long categoryId,
            @Param("query") String query,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Product p SET p.subcategory.id = :toSubcategoryId " +
            "WHERE p.subcategory.id = :fromSubcategoryId")
    void moveAllProducts(
            @Param("fromSubcategoryId") Long fromSubcategoryId,
            @Param("toSubcategoryId") Long toSubcategoryId
    );

}