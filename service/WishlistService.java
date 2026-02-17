package com.example.petmarket.service;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.User;
import com.example.petmarket.entity.Wishlist;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.repository.WishlistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Transactional
    public boolean toggleProduct(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            wishlistRepository.deleteByUserIdAndProductId(userId, productId);
            return false;
        } else {
            addToWishlist(userId, productId);
            return true;
        }
    }

    @Transactional
    public void addToWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new IllegalStateException("Produkt już jest w ulubionych");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nie istnieje"));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(new User());
        wishlist.getUser().setId(userId);
        wishlist.setProduct(product);

        wishlistRepository.save(wishlist);
    }

    public List<Product> getUserWishlistProducts(Long userId) {
        return wishlistRepository.findByUserIdWithProducts(userId)
                .stream()
                .map(Wishlist::getProduct)
                .collect(Collectors.toList());
    }

    public Set<Long> getUserWishlistProductIds(Long userId) {
        return wishlistRepository.findProductIdsByUserId(userId);
    }

    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }

    @Transactional
    public void clearWishlist(Long userId) {
        wishlistRepository.deleteByUserId(userId);
    }
}