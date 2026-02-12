package com.example.petmarket.controller;

import com.example.petmarket.StarHelper;
import com.example.petmarket.entity.*;
import com.example.petmarket.service.CategoryService;
import com.example.petmarket.service.ProductService;
import com.example.petmarket.service.BrandService;
import com.example.petmarket.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final BrandService brandService;
    private final WishlistService wishlistService;

    public record FilterTag(String label, String value) {}

    @GetMapping("/{slug}")
    public String showCategory(
            @PathVariable String slug,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        Category category = categoryService.getCategoryBySlug(slug);
        if (category == null) return "redirect:/";

        List<FilterTag> activeFilters = new ArrayList<>();
        Brand brandObject = (brand != null && !brand.isEmpty())
                ? brandService.getBrandBySlug(brand).orElse(null)
                : null;

        if (brandObject != null) {
            activeFilters.add(new FilterTag("Marka: " + brandObject.getName(), brand));
        }
        if (minPrice != null) {
            activeFilters.add(new FilterTag("Cena od: " + minPrice + " zł", "minPrice"));
        }
        if (maxPrice != null) {
            activeFilters.add(new FilterTag("Cena do: " + maxPrice + " zł", "maxPrice"));
        }

        Sort sortOrder;
        switch (sort != null ? sort : "newest") {
            case "price-asc" -> sortOrder = Sort.by("price").ascending();
            case "price-desc" -> sortOrder = Sort.by("price").descending();
            case "popularity" -> sortOrder = Sort.by("stockQuantity").descending();
            case "rating" -> sortOrder = Sort.by("rating").descending();
            default -> sortOrder = Sort.by("id").descending();
        }

        Pageable pageable = PageRequest.of(page, 20, sortOrder);

        java.math.BigDecimal min = (minPrice != null) ? java.math.BigDecimal.valueOf(minPrice) : null;
        java.math.BigDecimal max = (maxPrice != null) ? java.math.BigDecimal.valueOf(maxPrice) : null;

        Page<Product> productPage = productService.getProductsByCategory(category, brandObject, min, max, pageable);

        Map<Integer, Long> ratingCounts = productService.getRatingCountsForCategory(slug);

        Set<Long> wishlistProductIds = new HashSet<>();
        User user = (User) session.getAttribute("user");
        if (user != null) {
            wishlistProductIds = wishlistService.getUserWishlistProductIds(user.getId());
        }

        model.addAttribute("ratingCounts", ratingCounts);
        model.addAttribute("category", category);
        model.addAttribute("subCategories", category.getSubcategories());
        model.addAttribute("activeFilters", activeFilters);
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalProducts", productPage.getTotalElements());
        model.addAttribute("allBrands", brandService.getAllBrands());
        model.addAttribute("wishlistProductIds", wishlistProductIds);
        model.addAttribute("starHelper", new StarHelper());

        return "category";
    }
}