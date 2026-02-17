package com.example.petmarket.controller;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Cart;
import com.example.petmarket.service.cart.CartOperationService;
import com.example.petmarket.service.interfaces.IProductCrudService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final IProductCrudService productService;
    private final CartOperationService cartOperationService;
    private final MessageSource messageSource;

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    @GetMapping
    public String showCart(HttpSession session, Model model) {
        Cart displayCart = cartOperationService.getDisplayCart(session);
        model.addAttribute("cart", displayCart);
        model.addAttribute("cartItems", displayCart.getItems());
        model.addAttribute("total", displayCart.getTotal());
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            Product product = productService.getProductById(productId);

            if (product == null) {
                redirectAttributes.addFlashAttribute("error", getMessage("cart.error.product.not.found"));
                return "redirect:/";
            }

            if (!product.isAvailable()) {
                redirectAttributes.addFlashAttribute("error", getMessage("cart.error.product.unavailable"));
                return "redirect:/product/" + productId;
            }

            int currentInCart = cartOperationService.getCurrentCartQuantity(productId, session);
            int totalRequested = currentInCart + quantity;

            if (totalRequested > product.getStockQuantity()) {
                int canAdd = product.getStockQuantity() - currentInCart;
                if (canAdd <= 0) {
                    redirectAttributes.addFlashAttribute("error", getMessage("cart.error.stock.max", product.getStockQuantity()));
                } else {
                    redirectAttributes.addFlashAttribute("error", getMessage("cart.error.stock.limit", canAdd, product.getStockQuantity()));
                }
                return "redirect:/product/" + productId;
            }

            cartOperationService.addProductToCart(product, quantity, session);
            redirectAttributes.addFlashAttribute("success", getMessage("cart.success.added"));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", getMessage("cart.error.adding"));
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @PostMapping("/add-async")
    @ResponseBody
    public ResponseEntity<?> addToCartAsync(
            @RequestParam Long productId,
            @RequestParam int quantity,
            HttpSession session) {

        try {
            Product product = productService.getProductById(productId);

            if (product == null) {
                return ResponseEntity.status(404).body(getMessage("cart.error.product.not.found"));
            }

            if (!product.isAvailable()) {
                return ResponseEntity.status(400).body(getMessage("cart.error.product.unavailable"));
            }

            int currentInCart = cartOperationService.getCurrentCartQuantity(productId, session);
            int totalRequested = currentInCart + quantity;

            if (totalRequested > product.getStockQuantity()) {
                int canAdd = product.getStockQuantity() - currentInCart;
                if (canAdd <= 0) {
                    return ResponseEntity.status(400).body(getMessage("cart.error.stock.max", product.getStockQuantity()));
                } else {
                    return ResponseEntity.status(400).body(getMessage("cart.error.stock.limit", canAdd, product.getStockQuantity()));
                }
            }

            cartOperationService.addProductToCart(product, quantity, session);
            int totalItems = cartOperationService.getTotalItems(session);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "totalItems", totalItems
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Błąd: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCart(
            @RequestParam Long productId,
            @RequestParam(required = false) String action,
            HttpSession session) {

        try {
            Product product = productService.getProductById(productId);
            int stockQuantity = product.getStockQuantity();

            cartOperationService.executeCartOperation(session, (userCart, sessionCart) -> {
                var items = userCart != null ? userCart.getItems() : sessionCart.getItems();
                cartOperationService.updateCartItemQuantity(items, productId, action, stockQuantity);
            });

            return ResponseEntity.ok(Map.of("success", true, "message", "Koszyk zaktualizowany"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Wystąpił błąd: " + e.getMessage()));
        }
    }

    @GetMapping("/remove/{productId}")
    public String removeFromCart(
            @PathVariable Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            cartOperationService.removeProductFromCart(productId, session);
            redirectAttributes.addFlashAttribute("success", "Produkt usunięty z koszyka");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/remove/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCartAjax(
            @PathVariable Long productId,
            HttpSession session) {

        try {
            cartOperationService.removeProductFromCart(productId, session);
            return ResponseEntity.ok(Map.of("success", true, "message", "Produkt usunięty"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            cartOperationService.clearCart(session);
            redirectAttributes.addFlashAttribute("success", "Koszyk został wyczyszczony");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear-async")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearCartAsync(HttpSession session) {
        try {
            cartOperationService.clearCart(session);
            return ResponseEntity.ok(Map.of("success", true, "message", "Koszyk został wyczyszczony"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Wystąpił błąd: " + e.getMessage()));
        }
    }

    @GetMapping("/summary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCartSummary(HttpSession session) {
        Map<String, Object> summary = cartOperationService.extractCartData(session, (userCart, sessionCart) -> {
            if (userCart != null) {
                return Map.of(
                        "subtotal", userCart.getTotal(),
                        "itemCount", userCart.getTotalItems(),
                        "items", userCart.getItems().size()
                );
            } else {
                return Map.of(
                        "subtotal", sessionCart.getTotal(),
                        "itemCount", sessionCart.getTotalItems(),
                        "items", sessionCart.getItems().size()
                );
            }
        });

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getCartCount(HttpSession session) {
        int count = cartOperationService.getTotalItems(session);
        return ResponseEntity.ok(Map.of("count", count));
    }
}