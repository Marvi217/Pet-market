package com.example.petmarket.service.checkout;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutAuthService {

    private final SecurityHelper securityHelper;

    public boolean isAuthorizedForOrder(Order order, HttpSession session) {
        User currentUser = securityHelper.getCurrentUser(session);

        if (order.getUser() != null) {
            return currentUser == null || !currentUser.getId().equals(order.getUser().getId());
        }

        return order.getGuestEmail() == null || currentUser != null;
    }
}