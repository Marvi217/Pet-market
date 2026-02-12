package com.example.petmarket.repository;

import com.example.petmarket.entity.Promotion;
import com.example.petmarket.enums.PromotionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Promotion p " +
            "WHERE p.code = :code AND p.id != :excludeId")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("excludeId") Long excludeId);

    Page<Promotion> findByType(PromotionType type, Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.active = true " +
            "AND p.startDate <= :currentDate " +
            "AND (p.endDate IS NULL OR p.endDate >= :currentDate)")
    Page<Promotion> findActivePromotionsForDate(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.active = true " +
            "AND p.startDate > :currentDate")
    Page<Promotion> findUpcomingPromotions(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.endDate IS NOT NULL " +
            "AND p.endDate < :currentDate")
    Page<Promotion> findExpiredPromotions(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.active = true " +
            "AND p.endDate IS NOT NULL " +
            "AND p.endDate BETWEEN :startDate AND :endDate " +
            "ORDER BY p.endDate ASC")
    List<Promotion> findPromotionsEndingSoon(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT p FROM Promotion p JOIN p.products prod " +
            "WHERE prod.id = :productId AND p.active = true " +
            "AND p.startDate <= :currentDate " +
            "AND (p.endDate IS NULL OR p.endDate >= :currentDate)")
    List<Promotion> findActivePromotionsForProduct(
            @Param("productId") Long productId,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.active = true " +
            "AND p.startDate <= :currentDate " +
            "AND (p.endDate IS NULL OR p.endDate >= :currentDate)")
    long countActivePromotions(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT p FROM Promotion p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Promotion> searchPromotions(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Promotion p JOIN p.products prod " +
            "WHERE prod.id = :productId " +
            "AND p.active = true " +
            "AND p.startDate <= :currentDate " +
            "AND (p.endDate IS NULL OR p.endDate >= :currentDate) " +
            "AND (p.maxUsage IS NULL OR p.currentUsage < p.maxUsage) " +
            "ORDER BY p.priority DESC, p.discountPercentage DESC NULLS LAST")
    List<Promotion> findBestPromotionForProduct(
            @Param("productId") Long productId,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable
    );
}