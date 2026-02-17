package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentStatus;
import com.example.petmarket.repository.OrderRepository;
import com.example.petmarket.service.interfaces.IOrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements IOrderQueryService {

    private final OrderRepository orderRepository;

    @Override
    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    @Override
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }

    @Override
    public List<Order> findRecentByUser(User user, int limit) {
        return orderRepository.findTop3ByUserOrderByOrderDateDesc(user);
    }

    @Override
    public List<Order> findAllByUser(User user) {
        return orderRepository.findAllByUserOrderByOrderDateDesc(user);
    }

    @Override
    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable) {
        return orderRepository.findByPaymentStatus(paymentStatus, pageable);
    }

    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Override
    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findRecentOrders(PageRequest.of(0, limit));
    }

    @Override
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Override
    public Page<Order> searchOrders(String query, Pageable pageable) {
        return orderRepository.searchOrders(query, pageable);
    }
}