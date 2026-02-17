package com.example.petmarket.repository;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT AVG(r.rating) FROM Review r ")
    Double findAverageApprovedRating();

    List<Review> findByProductOrderByCreatedAtDesc(Product product);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);


    Page<Review> findByRatingBetweenOrderByCreatedAtDesc(int min, int max, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double calculateApprovedAverageRatingForProduct(@Param("productId") Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r " +
            "WHERE r.product.id = :productId " +
            "GROUP BY r.rating " +
            "ORDER BY r.rating DESC")
    List<Object[]> countApprovedReviewsByRatingForProduct(@Param("productId") Long productId);


    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Review r WHERE r.user.id = :userId AND r.product.id = :productId")
    boolean hasUserReviewedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

}