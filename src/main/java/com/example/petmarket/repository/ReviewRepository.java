package com.example.petmarket.repository;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Review;
import com.example.petmarket.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double findAverageRating();

    List<Review> findByProductOrderByCreatedAtDesc(Product product);

    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(
            Long productId,
            ReviewStatus status,
            Pageable pageable
    );

    Page<Review> findByRatingBetweenOrderByCreatedAtDesc(int min, int max, Pageable pageable);

    long countByStatus(ReviewStatus status);

    long countByProductId(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId ")
    Double getAverageRatingForProduct(@Param("productId") Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId " )
    Double calculateAverageRatingForProduct(@Param("productId") Long productId);

    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE " +
            "LOWER(r.comment) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(r.user.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(r.user.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(r.product.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Review> searchReviews(@Param("query") String query, Pageable pageable);
}