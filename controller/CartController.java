package com.example.petmarket.controller;

import com.example.petmarket.entity.Cart;
import com.example.petmarket.entity.Product;
import com.example.petmarket.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ProductService productService;

    private Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @GetMapping
    public String showCart(HttpSession session, Model model) {
        Cart cart = getCart(session);
        model.addAttribute("cartItems", cart.getItems());
        model.addAttribute("total", cart.getTotal());
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
                redirectAttributes.addFlashAttribute("error", "Produkt nie został znaleziony");
                return "redirect:/";
            }

            if (!product.isAvailable()) {
                redirectAttributes.addFlashAttribute("error", "Produkt jest niedostępny");
                return "redirect:/product/" + productId;
            }

            Cart cart = getCart(session);
            cart.addItem(product, quantity);

            redirectAttributes.addFlashAttribute("success", "Produkt dodany do koszyka!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas dodawania produktu");
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCart(
            @RequestParam Long productId,
            @RequestParam(required = false) String action,
            HttpSession session) {

        try {
            Cart cart = getCart(session);

            if ("increase".equals(action)) {
                cart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(productId))
                        .findFirst()
                        .ifPresent(item -> {
                            int newQty = item.getQuantity() + 1;
                            if (newQty <= 99) {
                                item.setQuantity(newQty);
                            }
                        });
            } else if ("decrease".equals(action)) {
                cart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(productId))
                        .findFirst()
                        .ifPresent(item -> {
                            int newQty = item.getQuantity() - 1;
                            if (newQty > 0) {
                                item.setQuantity(newQty);
                            }
                        });
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Koszyk zaktualizowany"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Wystąpił błąd: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/remove/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCartAjax(
            @PathVariable Long productId,
            HttpSession session) {

        try {
            Cart cart = getCart(session);
            cart.removeItem(productId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Produkt usunięty"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/remove/{productId}")
    public String removeFromCart(
            @PathVariable Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Cart cart = getCart(session);
        cart.removeItem(productId);

        redirectAttributes.addFlashAttribute("success", "Produkt usunięty z koszyka");
        return "redirect:/cart";
    }

    @GetMapping("/summary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCartSummary(HttpSession session) {
        Cart cart = getCart(session);

        return ResponseEntity.ok(Map.of(
                "subtotal", cart.getTotal(),
                "itemCount", cart.getTotalItems(),
                "items", cart.getItems().size()
        ));
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getCartCount(HttpSession session) {
        Cart cart = getCart(session);
        return ResponseEntity.ok(Map.of("count", cart.getTotalItems()));
    }

    @PostMapping("/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
        Cart cart = getCart(session);
        cart.clear();

        redirectAttributes.addFlashAttribute("success", "Koszyk został wyczyszczony");
        return "redirect:/cart";
    }

    @PostMapping("/add-async")
    @ResponseBody
    public ResponseEntity<?> addToCartAsync(
            @RequestParam Long productId,
            @RequestParam int quantity,
            HttpSession session) {

        try {
            Cart cart = getCart(session);
            Product product = productService.getProductById(productId);

            if (product == null) {
                return ResponseEntity.status(404).body("Nie znaleziono produktu");
            }

            cart.addItem(product, quantity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalItems", cart.getTotalItems());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Błąd: " + e.getMessage());
        }
    }
}