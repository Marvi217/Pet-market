package com.example.petmarket.service.checkout;

import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CheckoutDeliveryMapper {

    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("199");

    private final Map<String, DeliveryMethod> deliveryMethodMap;
    private final Map<String, PaymentMethod> paymentMethodMap;

    public CheckoutDeliveryMapper() {
        deliveryMethodMap = Map.of(
                "inpost", DeliveryMethod.LOCKER,
                "pickup", DeliveryMethod.PICKUP,
                "courier", DeliveryMethod.COURIER
        );

        paymentMethodMap = Map.of(
                "transfer", PaymentMethod.TRANSFER,
                "cod", PaymentMethod.CASH_ON_DELIVERY,
                "blik", PaymentMethod.BLIK,
                "card", PaymentMethod.CARD
        );
    }

    public DeliveryMethod mapDeliveryMethod(String method) {
        if (method == null) return DeliveryMethod.COURIER;
        return deliveryMethodMap.getOrDefault(method.toLowerCase(), DeliveryMethod.COURIER);
    }

    public PaymentMethod mapPaymentMethod(String method) {
        if (method == null) return PaymentMethod.CARD;
        return paymentMethodMap.getOrDefault(method.toLowerCase(), PaymentMethod.CARD);
    }

    public BigDecimal calculateDeliveryCost(DeliveryMethod method, BigDecimal cartTotal) {
        if (cartTotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        return method.getPrice();
    }
}