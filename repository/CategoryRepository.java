package com.example.petmarket.repository;

import com.example.petmarket.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    List<Category> findByActiveTrue();

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Subcategory s WHERE s.category.id = :categoryId")
    boolean hasSubcategories(@Param("categoryId") Long categoryId);

    long countByActiveTrue();

    @Query("SELECT c, COUNT(p) as productCount FROM Category c " +
            "LEFT JOIN Product p ON p.category.id = c.id " +
            "GROUP BY c.id " +
            "ORDER BY c.name")
    List<Object[]> findCategoriesWithProductCount();

    @Query("SELECT c FROM Category c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Category> searchCategories(@Param("query") String query, Pageable pageable);


    @Query("SELECT c FROM Category c ORDER BY size(c.products) DESC")
    List<Category> findTopCategoriesByProductCount(Pageable pageable);
}