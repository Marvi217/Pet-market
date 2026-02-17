package com.example.petmarket.repository;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT o.user, COUNT(o), SUM(o.totalAmount) FROM Order o " +
            "WHERE o.orderDate BETWEEN :from AND :to AND o.user IS NOT NULL " +
            "GROUP BY o.user ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findTopCustomersInPeriod(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status WHERE o.id IN :ids")
    int updateStatusForIds(@Param("ids") List<Long> ids, @Param("status") OrderStatus status);

    List<Order> findByUserOrderByOrderDateDesc(User user);

    List<Order> findAllByUserOrderByOrderDateDesc(User user);

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :from AND :to GROUP BY o.status")
    List<Object[]> countOrdersByStatusInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Order> findTop3ByUserOrderByOrderDateDesc(User user);

    List<Order> findByGuestEmailOrderByOrderDateDesc(String email);

    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status, Pageable pageable);

    long countByUser(User user);

    long countByStatus(OrderStatus status);

    long countByOrderDateBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user = :user " +
            "AND o.status NOT IN (com.example.zoo.enums.OrderStatus.CANCELLED, com.example.zoo.enums.OrderStatus.FAILED)")
    BigDecimal sumTotalSpentByUser(@Param("user") User user);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderDate BETWEEN :from AND :to " +
            "AND o.status NOT IN (com.example.zoo.enums.OrderStatus.CANCELLED, com.example.zoo.enums.OrderStatus.FAILED)")
    BigDecimal sumTotalAmountByOrderDateBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT o FROM Order o ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders(Pageable pageable);

    @Query("SELECT oi.product, SUM(oi.quantity) as totalQuantity, " +
            "SUM(oi.price * oi.quantity) as totalRevenue " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.orderDate BETWEEN :from AND :to " +
            "AND o.status NOT IN (com.example.zoo.enums.OrderStatus.CANCELLED, " +
            "com.example.zoo.enums.OrderStatus.FAILED) " +
            "GROUP BY oi.product " +
            "ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingProducts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("SELECT CAST(o.orderDate AS date) as orderDate, SUM(o.totalAmount) as revenue " +
            "FROM Order o " +
            "WHERE o.orderDate BETWEEN :from AND :to " +
            "AND o.status NOT IN (com.example.zoo.enums.OrderStatus.CANCELLED, " +
            "com.example.zoo.enums.OrderStatus.FAILED) " +
            "GROUP BY CAST(o.orderDate AS date) " +
            "ORDER BY CAST(o.orderDate AS date)")
    List<Object[]> findDailyRevenue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT CAST(o.orderDate AS date) as orderDate, COUNT(o) as orderCount " +
            "FROM Order o " +
            "WHERE o.orderDate BETWEEN :from AND :to " +
            "GROUP BY CAST(o.orderDate AS date) " +
            "ORDER BY CAST(o.orderDate AS date)")
    List<Object[]> findDailyOrdersCount(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    @Query("SELECT o FROM Order o WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) AND " +
            "(:dateFrom IS NULL OR o.orderDate >= :dateFrom) AND " +
            "(:dateTo IS NULL OR o.orderDate <= :dateTo) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.guestEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.guestName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.user.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.user.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> filterOrders(
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE " +
            "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.guestEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.guestName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.user.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(o.user.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Order> searchOrders(@Param("query") String query, Pageable pageable);

    @Query("SELECT o.user, COUNT(o) as orderCount, SUM(o.totalAmount) as totalSpent " +
            "FROM Order o " +
            "WHERE o.user IS NOT NULL " +
            "AND o.status NOT IN (com.example.zoo.enums.OrderStatus.CANCELLED, " +
            "com.example.zoo.enums.OrderStatus.FAILED) " +
            "GROUP BY o.user " +
            "ORDER BY totalSpent DESC")
    List<Object[]> findTopCustomers(Pageable pageable);

    @Query("SELECT o.paymentMethod, COUNT(o) FROM Order o " +
            "WHERE o.paymentMethod IS NOT NULL " +
            "GROUP BY o.paymentMethod")
    List<Object[]> countOrdersByPaymentMethod();

    @Query("SELECT o.deliveryMethod, COUNT(o) FROM Order o " +
            "WHERE o.deliveryMethod IS NOT NULL " +
            "GROUP BY o.deliveryMethod")
    List<Object[]> countOrdersByDeliveryMethod();

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = com.example.zoo.enums.OrderStatus.DELIVERED OR o.status =  com.example.zoo.enums.OrderStatus.CONFIRMED")
    BigDecimal sumTotalRevenue();

    @Query(value = "SELECT CAST(order_date AS DATE) as d, SUM(total_amount) " +
            "FROM orders " +
            "WHERE order_date >= DATEADD('DAY', -7, CURRENT_DATE) " +
            "AND status <> 'CANCELLED' " +
            "GROUP BY CAST(order_date AS DATE) " +
            "ORDER BY d ASC", nativeQuery = true)
    List<Object[]> findWeeklySalesRaw();

    List<Order> findByStatusAndOrderDateBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);
}