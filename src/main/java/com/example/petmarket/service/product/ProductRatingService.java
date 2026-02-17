package com.example.petmarket.service.product;

import com.example.petmarket.entity.Product;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.repository.ReviewRepository;
import com.example.petmarket.service.interfaces.IProductRatingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductRatingService implements IProductRatingService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void updateProductRating(Long productId) {
        Double avg = reviewRepository.calculateApprovedAverageRatingForProduct(productId);
        Product product = productRepository.findById(productId).orElseThrow();
        product.setRating(avg != null ? avg : 0.0);
        productRepository.save(product);
    }

    @Override
    public Map<Integer, Long> getRatingDistributionForProduct(Long productId) {
        List<Object[]> results = reviewRepository.countApprovedReviewsByRatingForProduct(productId);
        Map<Integer, Long> distribution = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        for (Object[] row : results) {
            Integer rating = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            distribution.put(rating, count);
        }

        return distribution;
    }

    @Override
    public Double getAverageRating() {
        Double avg = reviewRepository.findAverageApprovedRating();
        return avg != null ? avg : 0.0;
    }
}