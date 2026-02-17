package com.example.petmarket.service.tracking;

import com.example.petmarket.enums.DeliveryMethod;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PickupTrackingGenerator implements TrackingNumberGenerator {

    private static final String PREFIX = "ODB";

    @Override
    public String generate() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        String random = String.format("%06d", (int) (Math.random() * 1000000));
        return PREFIX + timestamp + random;
    }

    @Override
    public DeliveryMethod getSupportedMethod() {
        return DeliveryMethod.PICKUP;
    }
}