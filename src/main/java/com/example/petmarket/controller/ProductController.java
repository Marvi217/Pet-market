package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.StarHelper;
import com.example.petmarket.entity.User;
import com.example.petmarket.service.product.ProductDisplayService;
import com.example.petmarket.service.product.ReviewSubmissionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductDisplayService productDisplayService;
    private final ReviewSubmissionService reviewSubmissionService;
    private final SecurityHelper securityHelper;

    @GetMapping("/{id}")
    public String showProduct(@PathVariable Long id, Model model, HttpSession session) {
        try {
            User currentUser = securityHelper.getCurrentUser(session);
            ProductDisplayService.ProductDisplayData displayData = productDisplayService.getProductDisplayData(id, currentUser);

            model.addAttribute("starHelper", new StarHelper());
            model.addAttribute("product", displayData.product());
            model.addAttribute("reviews", displayData.reviews());
            model.addAttribute("relatedProducts", displayData.relatedProducts());
            model.addAttribute("canReview", displayData.canReview());
            model.addAttribute("hasReviewed", displayData.hasReviewed());
            model.addAttribute("reviewCount", displayData.reviews().size());
            model.addAttribute("ratingDistribution", displayData.ratingDistribution());
            model.addAttribute("totalReviews", displayData.reviews().size());
            model.addAttribute("isInWishlist", displayData.isInWishlist());

            return "product";
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
    }

    @PostMapping("/{id}/review")
    public String addReview(
            @PathVariable Long id,
            @RequestParam int rating,
            @RequestParam(required = false) String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Musisz być zalogowany, aby dodać opinię");
            return "redirect:/login?returnUrl=/product/" + id;
        }

        try {
            reviewSubmissionService.submitReview(id, currentUser, rating, comment);
            redirectAttributes.addFlashAttribute("success", "Dziękujemy za opinię! Twoja recenzja została opublikowana.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas dodawania opinii: " + e.getMessage());
        }

        return "redirect:/product/" + id;
    }
}