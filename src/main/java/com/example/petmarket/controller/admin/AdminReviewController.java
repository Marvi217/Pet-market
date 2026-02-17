package com.example.petmarket.controller.admin;

import com.example.petmarket.entity.Review;
import com.example.petmarket.service.product.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public String listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            Model model) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviews;


        if (minRating != null || maxRating != null) {
            reviews = reviewService.getReviewsByRatingRange(minRating, maxRating, pageRequest);
        } else {
            reviews = reviewService.getAllReviews(pageRequest);
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviews.getTotalPages());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedMinRating", minRating);

        return "admin/reviews/list";
    }

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.deleteReview(id);
            redirectAttributes.addFlashAttribute("success", "Opinia została usunięta");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można usunąć opinii: " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }
}