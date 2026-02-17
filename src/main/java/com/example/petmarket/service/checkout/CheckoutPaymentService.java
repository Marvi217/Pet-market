package com.example.petmarket.service.checkout;

import com.example.petmarket.entity.Order;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.service.email.OrderEmailSender;
import com.example.petmarket.service.interfaces.IOrderCrudService;
import com.example.petmarket.service.interfaces.IOrderQueryService;
import com.example.petmarket.service.interfaces.IOrderStatusService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CheckoutPaymentService {

    private final IOrderQueryService orderQueryService;
    private final IOrderCrudService orderCrudService;
    private final IOrderStatusService orderStatusService;
    private final CheckoutAuthService checkoutAuthService;
    private final OrderEmailSender orderEmailSender;

    public Order getOrderForPayment(String orderNumber, HttpSession session) {
        Order order = orderQueryService.getOrderByNumber(orderNumber);

        if (checkoutAuthService.isAuthorizedForOrder(order, session)) {
            throw new IllegalArgumentException("Brak dostępu do tego zamówienia");
        }

        if (order.getPaymentStatus() != com.example.petmarket.enums.PaymentStatus.PENDING) {
            throw new IllegalArgumentException("To zamówienie zostało już opłacone lub anulowane");
        }

        return order;
    }

    public String processPayment(String orderNumber, String status, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = orderQueryService.getOrderByNumber(orderNumber);

        if (checkoutAuthService.isAuthorizedForOrder(order, session)) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu do tego zamówienia");
            return "redirect:/";
        }

        if (order.getPaymentStatus() != com.example.petmarket.enums.PaymentStatus.PENDING) {
            redirectAttributes.addFlashAttribute("error", "Status płatności tego zamówienia nie może być już zmieniony");
            return "redirect:/";
        }

        return switch (status.toUpperCase()) {
            case "PAID" -> handleSuccessfulPayment(order, redirectAttributes);
            case "FAILED" -> handleFailedPayment(order, orderNumber, redirectAttributes);
            case "CANCELLED" -> handleCancelledPayment(order, redirectAttributes);
            default -> handleUnknownStatus(orderNumber, redirectAttributes);
        };
    }

    private String handleSuccessfulPayment(Order order, RedirectAttributes redirectAttributes) {
        order.setPaymentStatus(com.example.petmarket.enums.PaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED);
        orderCrudService.save(order);

        orderStatusService.scheduleStatusChangeToProcessing(order.getId());

        try {
            orderEmailSender.sendOrderConfirmationEmail(order);
        } catch (Exception e) {
            log.error("Nie udało się wysłać emaila potwierdzenia dla zamówienia {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }

        redirectAttributes.addFlashAttribute("orderNumber", order.getOrderNumber());
        redirectAttributes.addFlashAttribute("success", "Płatność zakończona sukcesem!");
        return "redirect:/checkout/confirmation";
    }

    private String handleFailedPayment(Order order, String orderNumber, RedirectAttributes redirectAttributes) {
        order.setPaymentStatus(com.example.petmarket.enums.PaymentStatus.FAILED);
        order.setStatus(OrderStatus.FAILED);
        orderCrudService.save(order);
        redirectAttributes.addFlashAttribute("error", "Płatność nie powiodła się. Spróbuj ponownie.");
        return "redirect:/checkout/payment?orderNumber=" + orderNumber;
    }

    private String handleCancelledPayment(Order order, RedirectAttributes redirectAttributes) {
        order.setPaymentStatus(com.example.petmarket.enums.PaymentStatus.CANCELLED);
        order.setStatus(OrderStatus.CANCELLED);
        orderCrudService.save(order);
        redirectAttributes.addFlashAttribute("error", "Płatność została anulowana.");
        return "redirect:/";
    }

    private String handleUnknownStatus(String orderNumber, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Nieznany status płatności");
        return "redirect:/checkout/payment?orderNumber=" + orderNumber;
    }
}