package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.repository.OrderRepository;
import com.example.petmarket.service.interfaces.IOrderTrackingService;
import com.example.petmarket.service.tracking.TrackingNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrderTrackingServiceImpl implements IOrderTrackingService {

    private final OrderRepository orderRepository;
    private final TrackingNumberService trackingNumberService;
    private final OrderStatusManager orderStatusManager;

    @Override
    @Transactional
    public void addTrackingNumber(Long orderId, String trackingNumber, DeliveryMethod carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono zamówienia o ID: " + orderId));

        order.setTrackingNumber(trackingNumber);
        if (carrier != null) {
            order.setDeliveryMethod(carrier);
        }

        orderStatusManager.changeStatus(order, OrderStatus.SHIPPED);

        String shippingNote = String.format("[%s] Zamówienie wysłane. Przewoźnik: %s, Numer: %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                order.getDeliveryMethod(),
                trackingNumber);

        order.setAdminNotes(order.getAdminNotes() != null ?
                order.getAdminNotes() + "\n" + shippingNote : shippingNote);

        orderRepository.save(order);
    }

    @Override
    @Transactional
    public String generateAndSetTrackingNumber(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono zamówienia o ID: " + orderId));

        if (order.getTrackingNumber() != null && !order.getTrackingNumber().isEmpty()) {
            throw new IllegalStateException("Zamówienie ma już wygenerowany numer przesyłki: " + order.getTrackingNumber());
        }

        String trackingNumber = trackingNumberService.generateTrackingNumber(order.getDeliveryMethod());
        order.setTrackingNumber(trackingNumber);
        orderStatusManager.changeStatus(order, OrderStatus.SHIPPED);

        String shippingNote = String.format("[%s] Wygenerowano numer przesyłki. Przewoźnik: %s, Numer: %s. Status: Wysłane",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                order.getDeliveryMethod() != null ? order.getDeliveryMethod().getDescription() : "Brak",
                trackingNumber);

        order.setAdminNotes(order.getAdminNotes() != null ?
                order.getAdminNotes() + "\n" + shippingNote : shippingNote);

        orderRepository.save(order);
        return trackingNumber;
    }
}