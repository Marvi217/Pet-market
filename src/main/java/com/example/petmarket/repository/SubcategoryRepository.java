package com.example.petmarket.repository;

import com.example.petmarket.entity.Subcategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    Page<Subcategory> findByCategoryId(Long categoryId, Pageable pageable);

    List<Subcategory> findByCategoryId(Long categoryId);

    List<Subcategory> findByActiveTrue();

    List<Subcategory> findByCategoryIdAndActiveTrue(Long categoryId);

    boolean existsByNameAndCategoryId(String name, Long categoryId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Product p WHERE p.subcategory.id = :subcategoryId")
    boolean hasProducts(@Param("subcategoryId") Long subcategoryId);

    @Query("SELECT s FROM Subcategory s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Subcategory> searchSubcategories(@Param("query") String query, Pageable pageable);
}