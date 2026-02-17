package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.repository.OrderRepository;
import com.example.petmarket.service.interfaces.IOrderCrudService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderCrudServiceImpl implements IOrderCrudService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void updateOrderDetails(Long id, Order details) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono zamówienia o ID: " + id));
        existingOrder.setAdminNotes(details.getAdminNotes());
        orderRepository.save(existingOrder);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zamówienie o ID " + id + " nie istnieje"));
    }
}