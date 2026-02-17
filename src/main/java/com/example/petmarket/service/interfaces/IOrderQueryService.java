package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IOrderQueryService {

    Page<Order> getUserOrders(Long userId, Pageable pageable);

    Order getOrderByNumber(String orderNumber);

    List<Order> findRecentByUser(User user, int limit);

    List<Order> findAllByUser(User user);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    List<Order> getOrdersByStatus(OrderStatus status);

    List<Order> getRecentOrders(int limit);

    Page<Order> getAllOrders(Pageable pageable);

    Page<Order> searchOrders(String query, Pageable pageable);
}