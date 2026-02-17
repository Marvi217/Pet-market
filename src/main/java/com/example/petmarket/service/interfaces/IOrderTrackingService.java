package com.example.petmarket.service.interfaces;

import com.example.petmarket.enums.DeliveryMethod;

public interface IOrderTrackingService {

    void addTrackingNumber(Long orderId, String trackingNumber, DeliveryMethod carrier);

    String generateAndSetTrackingNumber(Long orderId);
}