package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.dto.OrderStatsDto;
import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.service.interfaces.*;
import com.example.petmarket.service.product.SearchHistoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final IUserCrudService userCrudService;
    private final IUserQueryService userQueryService;
    private final IPasswordChangeService passwordChangeService;
    private final IOrderCrudService orderCrudService;
    private final IOrderQueryService orderQueryService;
    private final IOrderUserService orderUserService;
    private final SearchHistoryService.WishlistService wishlistService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String showAccount(Model model, HttpSession session) {
        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        long count = orderUserService.getTotalOrdersCount(user);
        BigDecimal spent = orderUserService.getTotalAmountSpent(user);

        List<Order> recentOrders = orderQueryService.findRecentByUser(user, 3);
        OrderStatsDto orderStats = new OrderStatsDto(count, spent);

        long wishlistCount = wishlistService.getWishlistCount(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("orderStats", orderStats);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("wishlistCount", wishlistCount);

        return "account/account";
    }

    @GetMapping("/orders")
    public String orders(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userQueryService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("orders", orderQueryService.findAllByUser(user));
        model.addAttribute("activePage", "orders");
        return "account/orders";
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userQueryService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "profile");
        return "account/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String phone,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = securityHelper.getCurrentUser(session);

            if (currentUser != null) {
                currentUser.setFirstName(firstName);
                currentUser.setLastName(lastName);
                currentUser.setPhone(phone);

                userCrudService.save(currentUser);

                session.setAttribute("user", currentUser);

                redirectAttributes.addFlashAttribute("success", "Dane zostały zaktualizowane!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji danych.");
        }
        return "redirect:/account/profile";
    }

    @GetMapping("/security")
    public String security(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userQueryService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "security");
        return "account/security";
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetails(@PathVariable Long id, HttpSession session, Model model) {
        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login?returnUrl=/account/orders/" + id;
        }

        Order order = orderCrudService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zamówienie nie istnieje"));

        if (!order.getUser().getId().equals(user.getId())) {
            return "redirect:/account/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("user", user);

        return "account/order-details";
    }

    @PostMapping("/security/update-password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Nowe hasła nie są identyczne.");
            return "redirect:/account/security";
        }

        try {
            passwordChangeService.changePassword(userDetails.getUsername(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", true);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/account/security";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);
        if (currentUser == null) return "redirect:/login";

        try {
            orderCrudService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono zamówienia."));

            orderUserService.cancelOrder(id, currentUser);

            redirectAttributes.addFlashAttribute("success", "Zamówienie zostało pomyślnie anulowane.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nie udało się anulować zamówienia: " + e.getMessage());
        }

        return "redirect:/account";
    }
}