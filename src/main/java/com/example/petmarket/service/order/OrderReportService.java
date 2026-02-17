package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderReportService {

    private final OrderRepository orderRepository;

    public byte[] exportToCSV(OrderStatus status, LocalDateTime from, LocalDateTime to) {
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStatusAndOrderDateBetween(status, from, to);
        } else {
            orders = orderRepository.findByOrderDateBetween(from, to);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID;Numer;Data;Klient;Suma;Status\n");

        for (Order o : orders) {
            csv.append(o.getId()).append(";")
                    .append(o.getOrderNumber()).append(";")
                    .append(o.getOrderDate()).append(";")
                    .append(o.getUser() != null ? o.getUser().getEmail() : "Gość").append(";")
                    .append(o.getTotalAmount()).append(";")
                    .append(o.getStatus()).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}