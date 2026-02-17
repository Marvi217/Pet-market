package com.example.petmarket.controller;

import com.example.petmarket.StarHelper;
import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Review;
import com.example.petmarket.repository.ReviewRepository;
import com.example.petmarket.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/product")

public class ProductController {

    private final ProductService productService;
    private final ReviewRepository reviewRepository;

    public ProductController(ProductService productService, ReviewRepository reviewRepository) {
        this.productService = productService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/{id}")
    public String showProduct(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);

        List<Review> reviews = reviewRepository.findByProductOrderByCreatedAtDesc(product);

        List<Product> relatedProducts = productService.getProductsByCategory(product.getCategory())
                .stream()
                .filter(p -> !p.getId().equals(id))
                .limit(4)
                .toList();

        model.addAttribute("starHelper", new StarHelper());
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("relatedProducts", relatedProducts);

        return "product";
    }
}