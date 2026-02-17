package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IOrderUserService {

    long getTotalOrdersCount(User user);

    BigDecimal getTotalAmountSpent(User user);

    void cancelOrder(Long orderId, User user);

    void adminCancelOrder(Long orderId, String reason);

    boolean canUserReviewProduct(Long userId, Long productId);

    List<Map<String, Object>> getTopCategoriesBySales(int limit);
}