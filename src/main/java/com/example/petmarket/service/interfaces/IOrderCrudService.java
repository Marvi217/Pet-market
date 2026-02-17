package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.Order;
import java.util.Optional;

public interface IOrderCrudService {

    Order save(Order order);

    void updateOrderDetails(Long id, Order details);

    Optional<Order> findById(Long id);

    Order getOrderById(Long id);
}