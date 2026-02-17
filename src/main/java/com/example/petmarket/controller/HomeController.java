package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.User;
import com.example.petmarket.service.product.CategoryService;
import com.example.petmarket.service.interfaces.IProductQueryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CategoryService categoryService;
    private final IProductQueryService productService;
    private final SecurityHelper securityHelper;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        User currentUser = securityHelper.getCurrentUser(session);
        if (currentUser != null && currentUser.isAdmin()) {
            return "redirect:/admin/main/dashboard";
        }

        model.addAttribute("categories", categoryService.getAllActiveCategories());
        model.addAttribute("products", productService.getAllActiveProducts());

        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        return "index";
    }
}