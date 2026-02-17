package com.example.petmarket.controller;

import com.example.petmarket.StarHelper;
import com.example.petmarket.entity.*;
import com.example.petmarket.service.product.CategoryService;
import com.example.petmarket.service.product.BrandService;
import com.example.petmarket.service.product.CategoryFilterService;
import com.example.petmarket.service.product.SearchHistoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.*;

@Controller
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryFilterService categoryFilterService;
    private final BrandService brandService;
    private final SearchHistoryService.WishlistService wishlistService;

    public record FilterTag(String label, String value) {}

    @GetMapping("/{slug}")
    public String showCategory(
            @PathVariable String slug,
            @RequestParam(required = false) List<Long> subcategory,
            @RequestParam(required = false) List<Long> brands,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        Category category = categoryService.getCategoryBySlug(slug);
        if (category == null || !category.isActive()) {
            return "redirect:/";
        }

        List<FilterTag> activeFilters = categoryFilterService.buildActiveFilters(subcategory, brands, minPrice, maxPrice);
        Page<Product> productPage = categoryFilterService.getFilteredProducts(category, subcategory, brands, minPrice, maxPrice, sort, page);

        Map<String, Object> filterData = categoryFilterService.getFilterData(slug, category);
        Set<Long> wishlistProductIds = getWishlistProductIds(session);

        model.addAttribute("ratingCounts", filterData.get("ratingCounts"));
        model.addAttribute("category", category);
        model.addAttribute("subCategories", category.getSubcategories());
        model.addAttribute("subcategoryFilters", filterData.get("subcategoryFilters"));
        model.addAttribute("brandFilters", filterData.get("brandFilters"));
        model.addAttribute("activeFilters", activeFilters);
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalProducts", productPage.getTotalElements());
        model.addAttribute("allBrands", brandService.getAllBrands());
        model.addAttribute("wishlistProductIds", wishlistProductIds);
        model.addAttribute("starHelper", new StarHelper());
        model.addAttribute("currentSort", sort != null ? sort : "newest");
        model.addAttribute("selectedSubcategories", subcategory != null ? subcategory : Collections.emptyList());
        model.addAttribute("selectedBrands", brands != null ? brands : Collections.emptyList());
        model.addAttribute("currentMinPrice", minPrice);
        model.addAttribute("currentMaxPrice", maxPrice);

        return "category";
    }

    @GetMapping("/{slug}/filter-counts")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFilterCounts(
            @PathVariable String slug,
            @RequestParam(required = false) List<Long> subcategory,
            @RequestParam(required = false) List<Long> brands,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        Category category = categoryService.getCategoryBySlug(slug);
        if (category == null || !category.isActive()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> counts = categoryFilterService.getFilterCounts(category, slug, subcategory, brands, minPrice, maxPrice);
        return ResponseEntity.ok(counts);
    }

    private Set<Long> getWishlistProductIds(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null ? wishlistService.getUserWishlistProductIds(user.getId()) : new HashSet<>();
    }
}