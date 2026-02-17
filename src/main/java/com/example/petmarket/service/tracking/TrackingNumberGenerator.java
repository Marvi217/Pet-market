package com.example.petmarket.service.tracking;

import com.example.petmarket.enums.DeliveryMethod;

public interface TrackingNumberGenerator {
    String generate();
    DeliveryMethod getSupportedMethod();
}