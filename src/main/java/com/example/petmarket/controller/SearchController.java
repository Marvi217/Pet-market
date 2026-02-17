package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.dto.ProductSearchDto;
import com.example.petmarket.entity.User;
import com.example.petmarket.service.product.CategoryService;
import com.example.petmarket.service.product.SearchService;
import com.example.petmarket.service.product.SearchSuggestionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final CategoryService categoryService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String search(@ModelAttribute ProductSearchDto searchDto, Model model, HttpSession session) {
        User user = securityHelper.getCurrentUser(session);
        searchService.recordSearch(searchDto.getQuery(), user, session.getId());

        model.addAttribute("products", searchService.searchProducts(searchDto));
        model.addAttribute("categories", categoryService.getAllActiveCategories());
        model.addAttribute("searchDto", searchDto);
        return "search-results";
    }

    @GetMapping("/quick")
    public String quickSearch(@RequestParam(required = false, defaultValue = "") String q, Model model, HttpSession session) {
        User user = securityHelper.getCurrentUser(session);
        if (!q.trim().isEmpty()) {
            searchService.recordSearch(q, user, session.getId());
        }

        model.addAttribute("products", searchService.quickSearch(q));
        model.addAttribute("query", q);
        model.addAttribute("categories", categoryService.getAllActiveCategories());
        return "search-results";
    }

    @GetMapping("/api/suggestions")
    @ResponseBody
    public List<SearchSuggestionService.SearchSuggestion> getSuggestions(
            @RequestParam(required = false, defaultValue = "") String q,
            HttpSession session) {
        User user = securityHelper.getCurrentUser(session);
        return searchService.getSuggestions(q, user, session.getId());
    }
}