package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class OrderStatusManager {

    private static final int CANCELLATION_HOURS_LIMIT = 5;

    public void changeStatus(Order order, OrderStatus newStatus) {
        order.setStatus(newStatus);
        order.setStatusChangedAt(LocalDateTime.now());

        if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(LocalDateTime.now());
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.PAID);
        }
    }

    public boolean canBeCancelled(Order order) {
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.CANCELLED ||
                status == OrderStatus.DELIVERED ||
                status == OrderStatus.RETURNED) {
            return false;
        }

        if (order.getOrderDate() != null) {
            LocalDateTime deadline = LocalDateTime.now().minusHours(CANCELLATION_HOURS_LIMIT);
            return order.getOrderDate().isAfter(deadline);
        }

        return false;
    }

    public long getHoursUntilCancellationExpires(Order order) {
        if (order.getOrderDate() == null) {
            return 0;
        }

        LocalDateTime cancellationDeadline = order.getOrderDate().plusHours(CANCELLATION_HOURS_LIMIT);
        Duration duration = Duration.between(LocalDateTime.now(), cancellationDeadline);
        long hours = duration.toHours();
        return hours > 0 ? hours : 0;
    }

    public String getStatusDisplay(Order order) {
        return order.getStatus() != null ? order.getStatus().getDisplayName() : "";
    }
}