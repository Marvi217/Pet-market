package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.User;
import com.example.petmarket.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;
    private final SecurityHelper securityHelper;

    @GetMapping("/account/wishlist")
    public String showWishlist(HttpSession session, Model model) {
        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login?returnUrl=/wishlist";
        }

        try {
            List<Product> wishlistProducts = wishlistService.getUserWishlistProducts(user.getId());

            model.addAttribute("products", wishlistProducts);
            model.addAttribute("user", user);

            return "account/wishlist";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Nie udało się załadować listy ulubionych");
            return "error/error";
        }
    }

    @ResponseBody
    @PostMapping("wishlist/toggle")
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @RequestBody Map<String, Long> request,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Long productId = request.get("productId");

        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Musisz być zalogowany");
            response.put("requireLogin", true);
            return ResponseEntity.ok(response);
        }

        try {
            boolean added = wishlistService.toggleProduct(user.getId(), productId);

            response.put("success", true);
            response.put("added", added);
            response.put("message", added ? "Dodano do ulubionych" : "Usunięto z ulubionych");
            response.put("wishlistCount", wishlistService.getWishlistCount(user.getId()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Wystąpił błąd: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/clear")
    public String clearWishlist(HttpSession session) {
        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        try {
            wishlistService.clearWishlist(user.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/wishlist";
    }
}