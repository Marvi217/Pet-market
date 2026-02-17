package com.example.petmarket.config;

import com.example.petmarket.entity.Cart;
import com.example.petmarket.entity.CartItem;
import com.example.petmarket.entity.User;
import com.example.petmarket.entity.UserCart;
import com.example.petmarket.repository.UserCartRepository;
import com.example.petmarket.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserCartRepository userCartRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);

        if (user != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Cart sessionCart = (Cart) session.getAttribute("cart");
                if (sessionCart != null && !sessionCart.isEmpty()) {
                    UserCart userCart = userCartRepository.findByUser(user)
                            .orElseGet(() -> {
                                UserCart newCart = new UserCart(user);
                                return userCartRepository.save(newCart);
                            });

                    for (CartItem item : sessionCart.getItems()) {
                        userCart.addItem(item.getProduct(), item.getQuantity());
                    }
                    userCartRepository.save(userCart);
                    session.removeAttribute("cart");
                }
            }
        }

        if (user != null && user.isAdmin()) {
            response.sendRedirect("/admin/main/dashboard");
        } else {
            response.sendRedirect("/");
        }
    }
}