package com.example.petmarket.controller;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.OrderItem;
import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentMethod;
import com.example.petmarket.repository.OrderRepository;
import com.example.petmarket.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final CartService cartService;
    private final OrderRepository orderRepository;

    @GetMapping("/checkout")
    public String checkout(Model model) {
        if (cartService.getProductsInCart().isEmpty()) return "redirect:/cart";

        model.addAttribute("order", new Order());
        model.addAttribute("payments", PaymentMethod.values());
        model.addAttribute("deliveries", DeliveryMethod.values());
        model.addAttribute("total", cartService.getTotalPrice());
        return "checkout";
    }

    @PostMapping("/confirm")
    public String confirmOrder(@ModelAttribute Order order, Principal principal) {
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setTotalAmount(cartService.getTotalPrice());
        order.setStatus(OrderStatus.PENDING);

        cartService.getProductsInCart().forEach((product, qty) -> {
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(qty);
            item.setPrice(product.getPrice());
            item.setOrder(order);

            order.getItems().add(item);
        });

        orderRepository.save(order);
        cartService.clear();

        return "redirect:/order/payment/mock?orderNumber=" + order.getOrderNumber();
    }

    @GetMapping("/payment/mock")
    public String mockPayment(@RequestParam String orderNumber, Model model) {
        model.addAttribute("orderNumber", orderNumber);
        return "paymentGateway";
    }

    @PostMapping("/finalize")
    public String finalizeOrder(@RequestParam String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Zamówienie nie istnieje"));

        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        return "redirect:/order/success?num=" + orderNumber;
    }

    @GetMapping("/success")
    public String successPage(@RequestParam String num, Model model) {
        model.addAttribute("orderNumber", num);
        return "orderSuccessView";
    }
}