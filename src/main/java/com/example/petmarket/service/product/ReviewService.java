package com.example.petmarket.service.product;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Review;
import com.example.petmarket.entity.User;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.repository.ReviewRepository;
import com.example.petmarket.service.interfaces.IProductRatingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ProfanityFilterService profanityFilterService;
    private final IProductRatingService productRatingService;

    @Transactional
    public void createCustomerReview(Long productId, User user, int rating, String comment) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produkt nie istnieje"));

        if (reviewRepository.hasUserReviewedProduct(user.getId(), productId)) {
            throw new RuntimeException("Już oceniłeś ten produkt");
        }

        String filteredComment = profanityFilterService.filter(comment);

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(filteredComment);

        reviewRepository.save(review);

        productRatingService.updateProductRating(productId);
    }

    public boolean hasUserReviewedProduct(Long userId, Long productId) {
        return reviewRepository.hasUserReviewedProduct(userId, productId);
    }

    public Page<Review> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    public List<Review> getRecentReviews() {
        return reviewRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opinia o ID " + id + " nie została znaleziona"));
    }

    public Page<Review> getReviewsByRatingRange(Integer minRating, Integer maxRating, Pageable pageable) {
        int min = (minRating != null) ? minRating : 1;
        int max = (maxRating != null) ? maxRating : 5;

        return reviewRepository.findByRatingBetweenOrderByCreatedAtDesc(min, max, pageable);
    }

    @Transactional
    public void deleteReview(Long id) {
        Review review = getReviewById(id);
        Long productId = review.getProduct().getId();

        reviewRepository.delete(review);

        productRatingService.updateProductRating(productId);
    }

    public Page<Review> getReviewsByUser(Long userId, Pageable pageable) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}