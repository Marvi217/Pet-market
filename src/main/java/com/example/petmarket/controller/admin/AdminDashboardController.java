package com.example.petmarket.controller.admin;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.service.order.OrderStatisticsService;
import com.example.petmarket.service.product.ReviewService;
import com.example.petmarket.service.interfaces.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/admin/main")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final IOrderQueryService orderQueryService;
    private final IOrderUserService orderUserService;
    private final OrderStatisticsService orderStatisticsService;
    private final IUserAdminService userAdminService;
    private final ReviewService reviewService;
    private final IProductRatingService productRatingService;
    private final SecurityHelper securityHelper;

    @GetMapping({"/dashboard"})
    public String showDashboard(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("totalOrders", orderStatisticsService.getTotalOrdersCount());
        model.addAttribute("pendingOrders", orderQueryService.getOrdersByStatus(OrderStatus.PENDING).size());
        model.addAttribute("totalRevenue", orderStatisticsService.getTotalRevenue());

        model.addAttribute("todayRevenue", orderStatisticsService.getTodayRevenue());
        Map<String, Double> weeklySales = orderStatisticsService.getWeeklySalesStats();
        model.addAttribute("weeklySales", weeklySales);

        model.addAttribute("monthlySales", orderStatisticsService.getMonthlySalesStats());

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        model.addAttribute("newUsersThisMonth", userAdminService.getNewUsersCount(thirtyDaysAgo, LocalDateTime.now()));
        model.addAttribute("averageRating", productRatingService.getAverageRating());
        model.addAttribute("topCategories", orderUserService.getTopCategoriesBySales(5));

        model.addAttribute("recentOrders", orderQueryService.getRecentOrders(5));
        model.addAttribute("recentReviews", reviewService.getRecentReviews());
        model.addAttribute("recentUsers", userAdminService.getRecentUsers());
        model.addAttribute("currentUser", currentUser);

        return "admin/dashboard";
    }

    @GetMapping("/orders")
    public String showOrders(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        return "admin/orders";
    }

    @GetMapping("/products")
    public String showProducts(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        return "admin/products";
    }

    @GetMapping("/brands")
    public String showBrands(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        return "admin/brands";
    }

    @GetMapping("/promotions")
    public String showPromotions(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        return "admin/promotions";
    }

    @GetMapping("/reviews")
    public String showReviews(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        return "admin/reviews";
    }

    @GetMapping("/users")
    public String showUsers(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        return "admin/users";
    }

}