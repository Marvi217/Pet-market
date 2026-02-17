package com.example.petmarket.service.order;

import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;
import com.example.petmarket.repository.OrderRepository;
import com.example.petmarket.service.interfaces.IOrderUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderUserServiceImpl implements IOrderUserService {

    private final OrderRepository orderRepository;
    private final OrderCancellationService orderCancellationService;

    @Override
    public long getTotalOrdersCount(User user) {
        return orderRepository.countByUser(user);
    }

    @Override
    public BigDecimal getTotalAmountSpent(User user) {
        BigDecimal total = orderRepository.sumTotalSpentByUser(user);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Zamówienie nie istnieje"));

        if (user.getRole() != UserRole.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Nie masz uprawnień do tego zamówienia.");
        }

        orderCancellationService.cancel(order, "Anulowano przez użytkownika: " + user.getEmail());
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void adminCancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Zamówienie nie znalezione"));

        orderCancellationService.cancel(order, reason);
        orderRepository.save(order);
    }

    @Override
    public boolean canUserReviewProduct(Long userId, Long productId) {
        return orderRepository.hasUserPurchasedProduct(userId, productId);
    }

    @Override
    public List<Map<String, Object>> getTopCategoriesBySales(int limit) {
        List<Object[]> results = orderRepository.findTopCategoriesBySales(PageRequest.of(0, limit));
        Long totalItemsSold = orderRepository.countTotalItemsSold();
        if (totalItemsSold == null || totalItemsSold == 0) {
            totalItemsSold = 1L;
        }

        List<Map<String, Object>> categoryStats = new ArrayList<>();
        for (Object[] row : results) {
            Category category = (Category) row[0];
            Long totalSold = ((Number) row[1]).longValue();

            Map<String, Object> stat = new HashMap<>();
            stat.put("name", category.getName());
            stat.put("icon", category.getIcon());
            stat.put("totalSold", totalSold);
            stat.put("percentage", Math.round((double) totalSold / totalItemsSold * 100));
            categoryStats.add(stat);
        }
        return categoryStats;
    }
}