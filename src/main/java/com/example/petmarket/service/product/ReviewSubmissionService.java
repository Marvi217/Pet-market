package com.example.petmarket.service.product;

import com.example.petmarket.entity.User;
import com.example.petmarket.service.interfaces.IOrderUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewSubmissionService {

    private final ReviewService reviewService;
    private final IOrderUserService orderUserService;

    public void submitReview(Long productId, User user, int rating, String comment) {
        validateReviewPermissions(productId, user);
        validateRating(rating);

        try {
            reviewService.createCustomerReview(productId, user, rating, comment);
            log.info("Review submitted for product {} by user {}", productId, user.getEmail());
        } catch (Exception e) {
            log.error("Error submitting review for product {} by user {}: {}",
                    productId, user.getEmail(), e.getMessage());
            throw e;
        }
    }

    private void validateReviewPermissions(Long productId, User user) {
        if (!orderUserService.canUserReviewProduct(user.getId(), productId)) {
            throw new IllegalStateException("Możesz oceniać tylko produkty które kupiłeś i otrzymałeś");
        }

        if (reviewService.hasUserReviewedProduct(user.getId(), productId)) {
            throw new IllegalStateException("Już oceniłeś ten produkt");
        }
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Ocena musi być w zakresie 1-5");
        }
    }
}