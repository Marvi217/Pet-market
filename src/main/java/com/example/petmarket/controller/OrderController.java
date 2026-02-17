package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.enums.PaymentMethod;
import com.example.petmarket.service.order.OrderProcessingService;
import com.example.petmarket.service.interfaces.ICartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final ICartService cartService;
    private final OrderProcessingService orderProcessingService;
    private final SecurityHelper securityHelper;

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (cartService.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Koszyk jest pusty");
            return "redirect:/cart";
        }

        User currentUser = securityHelper.getCurrentUser(session);

        model.addAttribute("order", new Order());
        model.addAttribute("payments", PaymentMethod.values());
        model.addAttribute("deliveries", DeliveryMethod.values());
        model.addAttribute("total", cartService.getTotalPrice());
        model.addAttribute("user", currentUser);
        model.addAttribute("isGuest", currentUser == null);

        return "checkout";
    }

    @PostMapping("/confirm")
    public String confirmOrder(
            @ModelAttribute Order order,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = securityHelper.getCurrentUser(session);
            Order savedOrder = orderProcessingService.createOrder(order, currentUser);

            return "redirect:/order/payment/mock?orderNumber=" + savedOrder.getOrderNumber();

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas tworzenia zamówienia");
            return "redirect:/checkout";
        }
    }

    @GetMapping("/payment/mock")
    public String mockPayment(
            @RequestParam String orderNumber,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Order order = orderProcessingService.getOrderByNumber(orderNumber);
            User currentUser = securityHelper.getCurrentUser(session);

            if (!isAuthorizedForOrder(order, currentUser)) {
                redirectAttributes.addFlashAttribute("error", "Brak dostępu do tego zamówienia");
                return "redirect:/";
            }

            model.addAttribute("orderNumber", orderNumber);
            model.addAttribute("order", order);
            return "payment-gateway";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/finalize")
    public String finalizeOrder(
            @RequestParam String orderNumber,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            Order order = orderProcessingService.getOrderByNumber(orderNumber);
            User currentUser = securityHelper.getCurrentUser(session);

            if (!isAuthorizedForOrder(order, currentUser)) {
                redirectAttributes.addFlashAttribute("error", "Brak dostępu do tego zamówienia");
                return "redirect:/";
            }

            orderProcessingService.finalizeOrder(orderNumber);
            redirectAttributes.addFlashAttribute("success", "Zamówienie zostało opłacone");

            return "redirect:/order/success?num=" + orderNumber;

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/success")
    public String successPage(
            @RequestParam String num,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Order order = orderProcessingService.getOrderByNumber(num);
            User currentUser = securityHelper.getCurrentUser(session);

            if (!isAuthorizedForOrder(order, currentUser)) {
                redirectAttributes.addFlashAttribute("error", "Brak dostępu do tego zamówienia");
                return "redirect:/";
            }

            model.addAttribute("orderNumber", num);
            model.addAttribute("order", order);
            return "order-success";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Zamówienie nie zostało znalezione");
            return "redirect:/";
        }
    }

    private boolean isAuthorizedForOrder(Order order, User currentUser) {
        if (order.getUser() != null) {
            return currentUser != null && currentUser.getId().equals(order.getUser().getId());
        }
        return order.getGuestEmail() != null && currentUser == null;
    }
}