package com.example.petmarket.controller;

import com.example.petmarket.dto.UserRegistrationDto;
import com.example.petmarket.service.user.UserRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserRegistration userRegistrationService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute("user") UserRegistrationDto dto,
            RedirectAttributes redirectAttributes) {

        try {
            userRegistrationService.register(dto);
            redirectAttributes.addFlashAttribute("success", "Sprawdź email aby aktywować konto");
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas rejestracji");
            return "redirect:/register";
        }
    }

    @GetMapping("/activate")
    public String activateAccount(
            @RequestParam("code") String code,
            RedirectAttributes redirectAttributes) {

        try {
            userRegistrationService.activateUser(code);
            redirectAttributes.addFlashAttribute("success", "Konto zostało aktywowane");
            return "redirect:/login?activated=true";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login?activation_error=true";
        }
    }
}