package com.example.petmarket.service;

import com.example.petmarket.entity.*;
import com.example.petmarket.enums.*;
import com.example.petmarket.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;


    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Map<String, Double> getMonthlySalesStats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Order> orders = orderRepository.findAllByOrderDateAfter(thirtyDaysAgo);

        Map<String, Double> stats = new TreeMap<>();

        for (int i = 30; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            stats.put(date, 0.0);
        }

        for (Order order : orders) {
            String date = order.getOrderDate().toLocalDate().toString();
            stats.put(date, stats.getOrDefault(date, 0.0) + order.getTotalAmount().doubleValue());
        }
        return stats;
    }

    @Transactional
    public void updateOrderDetails(Long id, Order details) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono zamówienia o ID: " + id));

        existingOrder.setStatus(details.getStatus());
        existingOrder.setPaymentStatus(details.getPaymentStatus());
        existingOrder.setAdminNotes(details.getAdminNotes());

        orderRepository.save(existingOrder);
    }

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zamówienie o ID " + id + " nie istnieje"));
    }

    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
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

    public byte[] exportToCSV(OrderStatus status, LocalDateTime from, LocalDateTime to) {
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStatusAndOrderDateBetween(status, from, to);
        } else {
            orders = orderRepository.findByOrderDateBetween(from, to);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID;Numer;Data;Klient;Suma;Status\n");

        for (Order o : orders) {
            csv.append(o.getId()).append(";")
                    .append(o.getOrderNumber()).append(";")
                    .append(o.getOrderDate()).append(";")
                    .append(o.getUser() != null ? o.getUser().getEmail() : "Gość").append(";")
                    .append(o.getTotalAmount()).append(";")
                    .append(o.getStatus()).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public void addTrackingNumber(Long orderId, String trackingNumber, DeliveryMethod carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono zamówienia o ID: " + orderId));

        order.setTrackingNumber(trackingNumber);
        order.setDeliveryMethod(carrier);
        order.setStatus(OrderStatus.SHIPPED);

        String shippingNote = String.format("[%s] Zamówienie wysłane. Przewoźnik: %s, Numer: %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                carrier,
                trackingNumber);

        order.setAdminNotes(order.getAdminNotes() != null ?
                order.getAdminNotes() + "\n" + shippingNote : shippingNote);

        orderRepository.save(order);
        log.info("Zamówienie {} zostało oznaczone jako wysłane. Przewoźnik: {}", orderId, carrier);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Zamówienie o ID " + orderId + " nie istnieje"));

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Tylko administratorzy mogą zmieniać status zamówienia");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Zmieniono status zamówienia {} na {} przez {}", orderId, newStatus, currentUser.getEmail());
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, PaymentStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono zamówienia o ID: " + orderId));

        order.setPaymentStatus(newStatus);

        if (newStatus == PaymentStatus.PAID && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.PROCESSING);
        }

        String paymentNote = String.format("[%s] Status płatności zmieniony na: %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                newStatus.name());

        order.setAdminNotes(order.getAdminNotes() != null ?
                order.getAdminNotes() + "\n" + paymentNote : paymentNote);

        orderRepository.save(order);
    }

    public long getTotalOrdersCount(User user) {
        return orderRepository.countByUser(user);
    }

    public BigDecimal getTotalAmountSpent(User user) {
        BigDecimal total = orderRepository.sumTotalSpentByUser(user);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<Order> findRecentByUser(User user, int limit) {
        return orderRepository.findTop3ByUserOrderByOrderDateDesc(user);
    }

    public List<Order> findAllByUser(User user) {
        return orderRepository.findAllByUserOrderByOrderDateDesc(user);
    }

    @Transactional
    public void cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Zamówienie nie istnieje"));

        if (user.getRole() != UserRole.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Nie masz uprawnień do tego zamówienia.");
        }

        performCancellation(order, "Anulowano przez użytkownika: " + user.getEmail());
    }

    private void performCancellation(Order order, String note) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Zamówienie jest już anulowane.");
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setAdminNotes(order.getAdminNotes() != null ?
                order.getAdminNotes() + "\n" + note : note);

        orderRepository.save(order);
    }

    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    public Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable) {
        return orderRepository.findByPaymentStatus(paymentStatus, pageable);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public BigDecimal getTodayRevenue() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        BigDecimal revenue = orderRepository.sumTotalAmountByOrderDateBetween(startOfDay, endOfDay);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public Map<String, Double> getWeeklySalesStats() {
        List<Object[]> results = orderRepository.findWeeklySalesRaw();
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

    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findRecentOrders(PageRequest.of(0, limit));
    }

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Page<Order> searchOrders(String query, Pageable pageable) {
        return orderRepository.searchOrders(query, pageable);
    }

    @Transactional
    public void adminCancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Zamówienie nie znalezione"));

        order.cancel(reason);
        orderRepository.save(order);

        log.info("Admin anulował zamówienie {}: {}", orderId, reason);
    }
}