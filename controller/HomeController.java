package com.example.petmarket.controller;

import com.example.petmarket.entity.User;
import com.example.petmarket.repository.UserRepository;
import com.example.petmarket.service.CategoryService;
import com.example.petmarket.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final UserRepository userRepository;

    public HomeController(CategoryService categoryService, ProductService productService, UserRepository userRepository) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        model.addAttribute("categories", categoryService.getAllActiveCategories());
        model.addAttribute("products", productService.getAllActiveProducts());
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            model.addAttribute("currentUser", user);
        }
        return "index";
    }
}