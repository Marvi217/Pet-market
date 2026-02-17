package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.UserRole;
import com.example.petmarket.repository.OrderRepository;
import com.example.petmarket.service.interfaces.IOrderStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusServiceImpl implements IOrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderStatusManager orderStatusManager;

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Zamówienie o ID " + orderId + " nie istnieje"));

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Tylko administratorzy mogą zmieniać status zamówienia");
        }

        orderStatusManager.changeStatus(order, newStatus);
        orderRepository.save(order);

        log.info("Zmieniono status zamówienia {} na {} przez {}", orderId, newStatus, currentUser.getEmail());
    }

    @Override
    @Async
    public void scheduleStatusChangeToProcessing(Long orderId) {
        try {
            Thread.sleep(30000);

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null && order.getStatus() == OrderStatus.CONFIRMED) {
                orderStatusManager.changeStatus(order, OrderStatus.PROCESSING);

                String note = String.format("[%s] Status automatycznie zmieniony na: W realizacji (po 30 sekundach od potwierdzenia płatności)",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                order.setAdminNotes(order.getAdminNotes() != null ?
                        order.getAdminNotes() + "\n" + note : note);

                orderRepository.save(order);
                log.info("Order {} status changed from CONFIRMED to PROCESSING after 30 seconds", orderId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting to change order {} status", orderId, e);
        } catch (Exception e) {
            log.error("Error changing order {} status to PROCESSING", orderId, e);
        }
    }
}