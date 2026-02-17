package com.example.petmarket.repository;

import com.example.petmarket.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    boolean existsByName(String name);

    List<Brand> findByActiveTrue();

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Product p WHERE p.brand.id = :brandId")
    boolean hasProducts(@Param("brandId") Long brandId);

    long countByActiveTrue();

    @Query("SELECT b FROM Brand b WHERE " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.country) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Brand> searchBrands(@Param("query") String query, Pageable pageable);
}