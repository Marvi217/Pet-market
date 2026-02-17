package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.enums.PaymentStatus;
import com.example.petmarket.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderStatisticsService {

    private final OrderRepository orderRepository;

    public Map<String, Double> getMonthlySalesStats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Order> orders = orderRepository.findAllByOrderDateAfter(thirtyDaysAgo);

        Map<String, Double> stats = new TreeMap<>();

        for (int i = 30; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            stats.put(date, 0.0);
        }

        for (Order order : orders) {
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                continue;
            }
            String date = order.getOrderDate().toLocalDate().toString();
            BigDecimal deliveryCost = order.getDeliveryCost() != null ? order.getDeliveryCost() : BigDecimal.ZERO;
            BigDecimal revenue = order.getTotalAmount().subtract(deliveryCost);
            stats.put(date, stats.getOrDefault(date, 0.0) + revenue.doubleValue());
        }
        return stats;
    }

    public Map<String, Object> getUserOrderStatistics(Long userId) {
        List<Order> userOrders = orderRepository.findAllByUserId(userId);

        BigDecimal totalSpent = userOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = userOrders.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSpent", totalSpent);
        stats.put("orderCount", count);
        stats.put("averageValue", count > 0 ? totalSpent.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return stats;
    }

    public BigDecimal getTodayRevenue() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        BigDecimal revenue = orderRepository.sumTotalAmountByOrderDateBetween(startOfDay, endOfDay);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public Map<String, Double> getWeeklySalesStats() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> results = orderRepository.findWeeklySalesRaw(since);
        Map<String, Double> stats = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");

        for (Object[] result : results) {
            LocalDate localDate;

            if (result[0] instanceof java.sql.Date) {
                localDate = ((java.sql.Date) result[0]).toLocalDate();
            } else if (result[0] instanceof LocalDate) {
                localDate = (LocalDate) result[0];
            } else {
                throw new IllegalStateException("Unexpected date type: " + result[0].getClass());
            }

            String date = localDate.format(formatter);
            Double total = ((Number) result[1]).doubleValue();
            stats.put(date, total);
        }
        return stats;
    }

    public long getTotalOrdersCount() {
        return orderRepository.count();
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.sumTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
}