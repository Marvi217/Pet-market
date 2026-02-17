package com.example.petmarket.service.product;

import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.SearchHistory;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchSuggestionService {

    private final SearchHistoryService searchHistoryService;
    private final CategoryService categoryService;
    private final ProductRepository productRepository;

    public record SearchSuggestion(String name, String type, String url, String icon) {}

    public List<SearchSuggestion> getSuggestions(String query, User user, String sessionId) {
        String prefix = query.trim().toLowerCase();
        List<SearchSuggestion> suggestions = new ArrayList<>();
        Set<String> addedQueries = new HashSet<>();

        addHistorySuggestions(suggestions, addedQueries, user, sessionId, prefix);
        addPopularSearchSuggestions(suggestions, addedQueries, prefix);
        addCategorySuggestions(suggestions, addedQueries, prefix);
        addProductSuggestions(suggestions, addedQueries, prefix);

        return suggestions.stream().limit(10).collect(Collectors.toList());
    }

    private void addHistorySuggestions(List<SearchSuggestion> suggestions, Set<String> addedQueries,
                                       User user, String sessionId, String prefix) {
        List<SearchHistory> historyList;
        if (user != null) {
            historyList = searchHistoryService.getUserSearchHistory(user, prefix, 3);
        } else if (sessionId != null) {
            historyList = searchHistoryService.getSessionSearchHistory(sessionId, prefix, 3);
        } else {
            return;
        }

        for (SearchHistory history : historyList) {
            if (addedQueries.add(history.getQuery().toLowerCase())) {
                suggestions.add(new SearchSuggestion(
                        history.getQuery(),
                        "HISTORIA",
                        "/search/quick?q=" + history.getQuery(),
                        "history"
                ));
            }
        }
    }

    private void addPopularSearchSuggestions(List<SearchSuggestion> suggestions, Set<String> addedQueries, String prefix) {
        List<Object[]> popularSearches = searchHistoryService.getPopularSearches(prefix, 5);
        for (Object[] result : popularSearches) {
            String popularQuery = (String) result[0];
            if (addedQueries.add(popularQuery.toLowerCase())) {
                suggestions.add(new SearchSuggestion(
                        popularQuery,
                        "POPULARNE",
                        "/search/quick?q=" + popularQuery,
                        "popular"
                ));
            }
            if (suggestions.size() >= 8) break;
        }
    }

    private void addCategorySuggestions(List<SearchSuggestion> suggestions, Set<String> addedQueries, String prefix) {
        List<Category> categories = categoryService.getAllActiveCategories();
        for (Category category : categories) {
            if (category.getName().toLowerCase().contains(prefix) &&
                    addedQueries.add("cat:" + category.getSlug())) {
                suggestions.add(new SearchSuggestion(
                        category.getName(),
                        "KATEGORIA",
                        "/category/" + category.getSlug(),
                        "category"
                ));
            }
            if (suggestions.size() >= 10) break;
        }
    }

    private void addProductSuggestions(List<SearchSuggestion> suggestions, Set<String> addedQueries, String prefix) {
        List<Product> products = productRepository.findByNameContainingIgnoreCase(prefix);
        for (Product product : products.stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .filter(p -> p.getCategory() == null || p.getCategory().isActive())
                .filter(p -> p.getSubcategory() == null || p.getSubcategory().isActive())
                .filter(p -> p.getBrand() == null || p.getBrand().isActive())
                .limit(5).toList()) {
            if (addedQueries.add("prod:" + product.getId())) {
                String priceStr = product.getCurrentPrice() != null ?
                        String.format("%.2f zł", product.getCurrentPrice()) : "";
                suggestions.add(new SearchSuggestion(
                        product.getName(),
                        priceStr,
                        "/product/" + product.getId(),
                        "product"
                ));
            }
            if (suggestions.size() >= 12) break;
        }
    }
}