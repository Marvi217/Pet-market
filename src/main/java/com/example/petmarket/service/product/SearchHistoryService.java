package com.example.petmarket.service.product;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.SearchHistory;
import com.example.petmarket.entity.User;
import com.example.petmarket.entity.Wishlist;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.repository.SearchHistoryRepository;
import com.example.petmarket.repository.WishlistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional
    public void recordSearch(String query, User user, String sessionId) {
        if (query == null || query.trim().length() < 2) return;

        String normalizedQuery = query.trim().toLowerCase();

        Optional<SearchHistory> existingSearch;
        if (user != null) {
            existingSearch = searchHistoryRepository.findByUserAndQueryIgnoreCase(user, normalizedQuery);
        } else {
            existingSearch = searchHistoryRepository.findBySessionIdAndQueryIgnoreCase(sessionId, normalizedQuery);
        }

        if (existingSearch.isPresent()) {
            SearchHistory history = existingSearch.get();
            history.incrementSearchCount();
            searchHistoryRepository.save(history);
        } else {
            SearchHistory history = SearchHistory.builder()
                    .query(normalizedQuery)
                    .user(user)
                    .sessionId(user == null ? sessionId : null)
                    .searchCount(1L)
                    .build();
            searchHistoryRepository.save(history);
        }
    }

    public List<SearchHistory> getUserSearchHistory(User user, String prefix, int limit) {
        return searchHistoryRepository.findUserSearchHistoryByPrefix(user, prefix, PageRequest.of(0, limit));
    }

    public List<SearchHistory> getSessionSearchHistory(String sessionId, String prefix, int limit) {
        return searchHistoryRepository.findSessionSearchHistoryByPrefix(sessionId, prefix, PageRequest.of(0, limit));
    }

    public List<Object[]> getPopularSearches(String prefix, int limit) {
        return searchHistoryRepository.findPopularSearchesByPrefix(prefix, PageRequest.of(0, limit));
    }

    @Service
    @RequiredArgsConstructor
    public static class WishlistService {

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

        public boolean isInWishlist(Long userId, Long productId) {
            return wishlistRepository.existsByUserIdAndProductId(userId, productId);
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

        public Set<Long> checkProductsInWishlist(Long userId, List<Long> productIds) {
            Set<Long> wishlistProductIds = getUserWishlistProductIds(userId);
            return productIds.stream()
                    .filter(wishlistProductIds::contains)
                    .collect(Collectors.toSet());
        }
    }
}