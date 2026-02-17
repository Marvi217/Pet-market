package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.OrderItem;
import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.repository.OrderRepository;
import com.example.petmarket.service.interfaces.ICartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final ICartService cartService;

    public Order createOrder(Order orderData, User currentUser) {
        if (cartService.isEmpty()) {
            throw new IllegalStateException("Koszyk jest pusty");
        }

        Order order = new Order();
        order.setUser(currentUser);
        order.setOrderNumber(generateOrderNumber());
        order.setTotalAmount(cartService.getTotalPrice());
        order.setStatus(OrderStatus.PENDING);

        copyOrderData(orderData, order);

        List<OrderItem> items = createOrderItems(order);
        order.setItems(items);

        Order savedOrder = orderRepository.save(order);
        cartService.clear();

        log.info("Created order: {} for user: {}", savedOrder.getOrderNumber(),
                currentUser != null ? currentUser.getEmail() : "guest");

        return savedOrder;
    }

    public Order finalizeOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Zamówienie nie istnieje: " + orderNumber));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Zamówienie nie może być sfinalizowane w tym statusie");
        }

        order.setStatus(OrderStatus.PROCESSING);
        return orderRepository.save(order);
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Zamówienie nie istnieje: " + orderNumber));
    }

    private void copyOrderData(Order source, Order target) {
        target.setPaymentMethod(source.getPaymentMethod());
        target.setDeliveryMethod(source.getDeliveryMethod());
        target.setShippingAddress(source.getShippingAddress());
        target.setGuestName(source.getGuestName());
        target.setGuestEmail(source.getGuestEmail());
        target.setGuestPhone(source.getGuestPhone());
        target.setNotes(source.getNotes());
    }

    private List<OrderItem> createOrderItems(Order order) {
        List<OrderItem> items = new ArrayList<>();
        Map<Product, Integer> productsInCart = cartService.getProductsInCart();

        for (Map.Entry<Product, Integer> entry : productsInCart.entrySet()) {
            OrderItem item = new OrderItem();
            item.setProduct(entry.getKey());
            item.setQuantity(entry.getValue());
            item.setPrice(entry.getKey().getPrice());
            item.setOrder(order);
            items.add(item);
        }

        return items;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }
}