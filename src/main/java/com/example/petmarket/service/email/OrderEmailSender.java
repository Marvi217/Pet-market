package com.example.petmarket.service.email;

import com.example.petmarket.entity.Order;

public interface OrderEmailSender {
    void sendOrderConfirmationEmail(Order order);
    void sendShippingNotification(Order order);
}