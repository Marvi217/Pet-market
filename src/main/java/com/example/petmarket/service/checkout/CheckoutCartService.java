package com.example.petmarket.service.checkout;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.Cart;
import com.example.petmarket.entity.CartItem;
import com.example.petmarket.entity.User;
import com.example.petmarket.entity.UserCart;
import com.example.petmarket.repository.UserCartRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutCartService {

    private final UserCartRepository userCartRepository;
    private final SecurityHelper securityHelper;

    public record CartData(BigDecimal total, List<CartItem> items, boolean isEmpty) {}

    public CartData getCartData(HttpSession session) {
        User user = securityHelper.getCurrentUser(session);

        if (user != null) {
            UserCart userCart = userCartRepository.findByUser(user).orElse(null);
            if (userCart == null || userCart.isEmpty()) {
                return new CartData(BigDecimal.ZERO, new ArrayList<>(), true);
            }
            List<CartItem> items = userCart.getItems().stream()
                    .map(item -> new CartItem(item.getProduct(), item.getQuantity()))
                    .collect(Collectors.toList());
            return new CartData(userCart.getTotal(), items, false);
        } else {
            Cart cart = getSessionCart(session);
            return new CartData(cart.getTotal(), cart.getItems(), cart.isEmpty());
        }
    }

    public void clearCart(HttpSession session) {
        User user = securityHelper.getCurrentUser(session);

        if (user != null) {
            userCartRepository.findByUser(user).ifPresent(userCart -> {
                userCart.clear();
                userCartRepository.save(userCart);
            });
        } else {
            Cart cart = getSessionCart(session);
            cart.clear();
        }
    }

    private Cart getSessionCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }
}