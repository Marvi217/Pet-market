package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;

public interface IOrderStatusService {

    void updateOrderStatus(Long orderId, OrderStatus newStatus, User currentUser);

    void scheduleStatusChangeToProcessing(Long orderId);
}