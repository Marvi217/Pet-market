package com.example.petmarket.service.product;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Review;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.ReviewRepository;
import com.example.petmarket.service.interfaces.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDisplayService {

    private final IProductCrudService productCrudService;
    private final IProductQueryService productQueryService;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final IOrderUserService orderUserService;
    private final IProductRatingService productRatingService;
    private final SearchHistoryService.WishlistService wishlistService;

    public record ProductDisplayData(
            Product product,
            List<Review> reviews,
            List<Product> relatedProducts,
            boolean canReview,
            boolean hasReviewed,
            boolean isInWishlist,
            Map<Integer, Long> ratingDistribution
    ) {}

    public ProductDisplayData getProductDisplayData(Long productId, User currentUser) {
        Product product = getValidProduct(productId);
        validateProductAccess(product);

        List<Review> reviews = getApprovedReviews(product);
        List<Product> relatedProducts = getRelatedProducts(product, productId);
        Map<Integer, Long> ratingDistribution = productRatingService.getRatingDistributionForProduct(productId);

        boolean canReview = false;
        boolean hasReviewed = false;
        boolean isInWishlist = false;

        if (currentUser != null) {
            canReview = orderUserService.canUserReviewProduct(currentUser.getId(), productId);
            hasReviewed = reviewService.hasUserReviewedProduct(currentUser.getId(), productId);
            isInWishlist = wishlistService.isInWishlist(currentUser.getId(), productId);
        }

        return new ProductDisplayData(
                product,
                reviews,
                relatedProducts,
                canReview && !hasReviewed,
                hasReviewed,
                isInWishlist,
                ratingDistribution
        );
    }

    private Product getValidProduct(Long productId) {
        try {
            return productCrudService.getProductById(productId);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Produkt nie został znaleziony");
        }
    }

    private void validateProductAccess(Product product) {
        if (product.getStatus() == ProductStatus.DRAFT) {
            throw new IllegalArgumentException("Produkt jest niedostępny");
        }

        if (product.getCategory() != null && !product.getCategory().isActive()) {
            throw new IllegalArgumentException("Kategoria produktu jest nieaktywna");
        }

        if (product.getSubcategory() != null && !product.getSubcategory().isActive()) {
            throw new IllegalArgumentException("Podkategoria produktu jest nieaktywna");
        }

        if (product.getBrand() != null && !product.getBrand().isActive()) {
            throw new IllegalArgumentException("Marka produktu jest nieaktywna");
        }
    }

    private List<Review> getApprovedReviews(Product product) {
        return new ArrayList<>(reviewRepository.findByProductOrderByCreatedAtDesc(product));
    }

    private List<Product> getRelatedProducts(Product product, Long currentProductId) {
        return productQueryService.getProductsByCategory(product.getCategory())
                .stream()
                .filter(p -> !p.getId().equals(currentProductId))
                .limit(4)
                .toList();
    }
}