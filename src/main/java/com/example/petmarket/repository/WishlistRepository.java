package com.example.petmarket.repository;

import com.example.petmarket.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product WHERE w.user.id = :userId ORDER BY w.addedAt DESC")
    List<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);

    long countByUserId(Long userId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT w.product.id FROM Wishlist w WHERE w.user.id = :userId")
    Set<Long> findProductIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);

}