package com.example.petmarket.controller.admin;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/main")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService orderService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final CategoryService categoryService;
    private final SecurityHelper securityHelper;

    @GetMapping({"/dashboard"})
    public String showDashboard(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("totalOrders", orderService.getTotalOrdersCount());
        model.addAttribute("pendingOrders", orderService.getOrdersByStatus(OrderStatus.PENDING).size());
        model.addAttribute("totalRevenue", orderService.getTotalRevenue());

        model.addAttribute("todayRevenue", orderService.getTodayRevenue());
        Map<String, Double> weeklySales = orderService.getWeeklySalesStats();
        model.addAttribute("weeklySales", weeklySales);

        model.addAttribute("monthlySales", orderService.getMonthlySalesStats());

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        model.addAttribute("newUsersThisMonth", userService.getNewUsersCount(thirtyDaysAgo, LocalDateTime.now()));
        model.addAttribute("averageRating", reviewService.getAverageRating());
        model.addAttribute("topCategories", getCategoryStats());

        model.addAttribute("recentOrders", orderService.getRecentOrders(5));
        model.addAttribute("currentUser", currentUser);

        return "admin/dashboard";
    }

    private List<Map<String, Object>> getCategoryStats() {
        List<Category> categories = categoryService.getTopCategories(4);
        List<Map<String, Object>> categoryStats = new ArrayList<>();
        for (Category cat : categories) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("name", cat.getName());
            stat.put("icon", cat.getIcon());
            stat.put("percentage", categoryService.calculateCategoryPercentage(cat));
            categoryStats.add(stat);
        }
        return categoryStats;
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
        model.addAttribute("pendingReviews", reviewService.getPendingReviewsCount());
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

    @GetMapping("/settings")
    public String showSettings(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        return "admin/settings";
    }
}