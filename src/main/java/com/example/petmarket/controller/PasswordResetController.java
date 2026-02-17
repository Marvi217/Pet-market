package com.example.petmarket.controller;

import com.example.petmarket.entity.User;
import com.example.petmarket.service.interfaces.IPasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final IPasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        try {
            passwordResetService.createPasswordResetToken(email);
            redirectAttributes.addFlashAttribute("success",
                    "Jeśli podany adres email istnieje w naszym systemie, otrzymasz link do resetowania hasła.");
        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Wystąpił błąd podczas wysyłania emaila. Spróbuj ponownie później.");
        }

        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(
            @RequestParam String token,
            Model model,
            RedirectAttributes redirectAttributes) {

        User user = passwordResetService.validatePasswordResetToken(token);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Link do resetowania hasła jest nieprawidłowy lub wygasł. Poproś o nowy link.");
            return "redirect:/forgot-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("userEmail", user.getEmail());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        String validationError = passwordResetService.validatePassword(newPassword, confirmPassword);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/reset-password?token=" + token;
        }

        boolean success = passwordResetService.resetPasswordWithToken(token, newPassword);

        if (success) {
            redirectAttributes.addFlashAttribute("success",
                    "Hasło zostało zmienione pomyślnie. Możesz się teraz zalogować.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Link do resetowania hasła jest nieprawidłowy lub wygasł. Poproś o nowy link.");
            return "redirect:/forgot-password";
        }
    }
}