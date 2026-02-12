package com.example.petmarket;

import com.example.petmarket.entity.User;
import com.example.petmarket.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityHelper {

    private final UserRepository userRepository;

    public User getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user != null) {
            return user;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String email = authentication.getName();
        user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            session.setAttribute("user", user);
        }

        return user;
    }
}