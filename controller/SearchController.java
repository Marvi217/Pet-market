package com.example.petmarket.controller;

import com.example.petmarket.dto.ProductSearchDto;
import com.example.petmarket.service.CategoryService;
import com.example.petmarket.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;
    private final CategoryService categoryService;

    public SearchController(SearchService searchService, CategoryService categoryService) {
        this.searchService = searchService;
        this.categoryService = categoryService;
    }
    public static class SearchSuggestion {
        public String name;
        public String type;
        public String url;

        public SearchSuggestion(String name, String type, String url) {
            this.name = name;
            this.type = type;
            this.url = url;
        }
    }

    @GetMapping
    public String search(@ModelAttribute ProductSearchDto searchDto, Model model) {
        model.addAttribute("products", searchService.searchProducts(searchDto));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("searchDto", searchDto);
        return "search-results";
    }

    @GetMapping("/quick")
    public String quickSearch(@RequestParam String q, Model model) {
        model.addAttribute("products", searchService.quickSearch(q));
        model.addAttribute("query", q);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "search-results";
    }

    @GetMapping("/api/suggestions")
    @ResponseBody
    public List<SearchSuggestion> getSuggestions(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) return List.of();

        List<SearchSuggestion> suggestions = new ArrayList<>();

        categoryService.getAllCategories().stream()
                .filter(c -> c.getName().toLowerCase().contains(q.toLowerCase()))
                .limit(3)
                .forEach(c -> {
                    String url = "/search/quick?q=" + c.getName();
                    suggestions.add(new SearchSuggestion(c.getName(), "KATEGORIA", url));
                });

        searchService.quickSearch(q).stream()
                .limit(5)
                .forEach(p -> {
                    String url = "/product/" + p.getId();
                    suggestions.add(new SearchSuggestion(p.getName(), "PRODUKT", url));
                });

        return suggestions;
    }
}