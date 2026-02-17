package com.example.petmarket.repository;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findAllByOrderDateAfter(LocalDateTime date);

    List<Order> findAllByUserId(Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByUserOrderByOrderDateDesc(User user);

    List<Order> findTop3ByUserOrderByOrderDateDesc(User user);

    long countByUser(User user);

    @Query("SELECT SUM(o.totalAmount - o.deliveryCost) FROM Order o WHERE o.user = :user " +
            "AND o.paymentStatus = com.example.petmarket.enums.PaymentStatus.PAID")
    BigDecimal sumTotalSpentByUser(@Param("user") User user);

    @Query("SELECT SUM(o.totalAmount - o.deliveryCost) FROM Order o WHERE o.orderDate BETWEEN :from AND :to " +
            "AND o.paymentStatus = com.example.petmarket.enums.PaymentStatus.PAID")
    BigDecimal sumTotalAmountByOrderDateBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT o FROM Order o ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
            "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.guestEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.guestName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.user.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.user.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Order> searchOrders(@Param("query") String query, Pageable pageable);


    @Query("SELECT SUM(o.discountAmount) FROM Order o WHERE o.discountAmount > 0 " +
            "AND o.status NOT IN (com.example.petmarket.enums.OrderStatus.CANCELLED, com.example.petmarket.enums.OrderStatus.FAILED)")
    BigDecimal sumTotalDiscounts();

    @Query("SELECT SUM(o.discountAmount) FROM Order o WHERE o.discountAmount > 0 " +
            "AND o.orderDate BETWEEN :from AND :to " +
            "AND o.status NOT IN (com.example.petmarket.enums.OrderStatus.CANCELLED, com.example.petmarket.enums.OrderStatus.FAILED)")
    BigDecimal sumDiscountsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.discountAmount > 0 " +
            "AND o.status NOT IN (com.example.petmarket.enums.OrderStatus.CANCELLED, com.example.petmarket.enums.OrderStatus.FAILED)")
    long countOrdersWithDiscount();

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    @Query("SELECT SUM(o.totalAmount - o.deliveryCost) FROM Order o WHERE o.paymentStatus = com.example.petmarket.enums.PaymentStatus.PAID")
    BigDecimal sumTotalRevenue();

    @Query(value = "SELECT CAST(order_date AS DATE) as d, SUM(total_amount - COALESCE(delivery_cost, 0)) " +
            "FROM orders " +
            "WHERE order_date >= :since " +
            "AND payment_status = 'PAID' " +
            "GROUP BY CAST(order_date AS DATE) " +
            "ORDER BY d ASC", nativeQuery = true)
    List<Object[]> findWeeklySalesRaw(@Param("since") LocalDateTime since);

    List<Order> findByStatusAndOrderDateBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
            "FROM OrderItem oi JOIN oi.order o " +
            "WHERE o.user.id = :userId AND oi.product.id = :productId " +
            "AND o.status IN (com.example.petmarket.enums.OrderStatus.DELIVERED, com.example.petmarket.enums.OrderStatus.RETURNED)")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT oi.product.category, SUM(oi.quantity) as totalSold, SUM(oi.price * oi.quantity) as totalRevenue " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status NOT IN (com.example.petmarket.enums.OrderStatus.CANCELLED, com.example.petmarket.enums.OrderStatus.FAILED) " +
            "AND oi.product.category IS NOT NULL " +
            "GROUP BY oi.product.category " +
            "ORDER BY totalSold DESC")
    List<Object[]> findTopCategoriesBySales(Pageable pageable);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status NOT IN (com.example.petmarket.enums.OrderStatus.CANCELLED, com.example.petmarket.enums.OrderStatus.FAILED)")
    Long countTotalItemsSold();

}