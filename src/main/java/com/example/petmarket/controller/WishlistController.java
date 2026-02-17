package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.User;
import com.example.petmarket.service.product.SearchHistoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WishlistController {

    private final SearchHistoryService.WishlistService wishlistService;
    private final SecurityHelper securityHelper;

    @GetMapping("/account/wishlist")
    public String showWishlist(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login?returnUrl=/account/wishlist";
        }

        try {
            List<Product> wishlistProducts = wishlistService.getUserWishlistProducts(user.getId());
            model.addAttribute("products", wishlistProducts);
            model.addAttribute("user", user);
            return "account/wishlist";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nie udało się załadować listy ulubionych");
            return "redirect:/";
        }
    }

    @PostMapping("/wishlist/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @RequestBody Map<String, Long> request,
            HttpSession session) {

        User user = securityHelper.getCurrentUser(session);
        Long productId = request.get("productId");

        if (user == null) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Musisz być zalogowany",
                    "requireLogin", true
            ));
        }

        try {
            boolean added = wishlistService.toggleProduct(user.getId(), productId);
            Long wishlistCount = wishlistService.getWishlistCount(user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "added", added,
                    "message", added ? "Dodano do ulubionych" : "Usunięto z ulubionych",
                    "wishlistCount", wishlistCount
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Wystąpił błąd: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/wishlist/clear")
    public String clearWishlist(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login?returnUrl=/account/wishlist";
        }

        try {
            wishlistService.clearWishlist(user.getId());
            redirectAttributes.addFlashAttribute("success", "Lista ulubionych została wyczyszczona");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas czyszczenia listy ulubionych");
        }

        return "redirect:/account/wishlist";
    }
}