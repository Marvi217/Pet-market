package com.example.petmarket.service.interfaces;

import java.util.Map;

public interface IProductRatingService {

    void updateProductRating(Long productId);

    Map<Integer, Long> getRatingDistributionForProduct(Long productId);

    Double getAverageRating();
}