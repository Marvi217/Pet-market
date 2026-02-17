package com.example.petmarket.service;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Review;
import com.example.petmarket.enums.ReviewStatus;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public Page<Review> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opinia o ID " + id + " nie została znaleziona"));
    }

    public Page<Review> getReviewsByRatingRange(Integer minRating, Integer maxRating, Pageable pageable) {
        log.info("Pobieranie opinii w zakresie ocen: {} - {}", minRating, maxRating);

        int min = (minRating != null) ? minRating : 1;
        int max = (maxRating != null) ? maxRating : 5;

        return reviewRepository.findByRatingBetweenOrderByCreatedAtDesc(min, max, pageable);
    }

    @Transactional
    public void deleteReview(Long id) {
        Review review = getReviewById(id);
        Long productId = review.getProduct().getId();

        reviewRepository.delete(review);
        updateProductRating(productId);

        log.info("Usunięto opinię o ID: {}", id);
    }

    @Transactional
    public void updateProductRating(Long productId) {
        Double avg = reviewRepository.getAverageRatingForProduct(productId);
        Product product = productRepository.findById(productId).orElseThrow();
        product.setRating(avg != null ? avg : 0.0);
        productRepository.save(product);
    }

    public long getPendingReviewsCount() {
        return reviewRepository.countByStatus(ReviewStatus.PENDING);
    }
    public Double getAverageRating() {
        Double avg = reviewRepository.findAverageRating();
        return avg != null ? avg : 0.0;
    }
}