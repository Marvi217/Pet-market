package com.example.petmarket.service.product;

import com.example.petmarket.dto.ProductSearchDto;
import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.User;
import com.example.petmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;
    private final SearchHistoryService searchHistoryService;
    private final SearchSuggestionService searchSuggestionService;

    public List<Product> searchProducts(ProductSearchDto searchDto) {
        return productRepository.searchProducts(
                        searchDto.getQuery(),
                        searchDto.getCategoryId(),
                        searchDto.getMinPrice(),
                        searchDto.getMaxPrice(),
                        searchDto.getAvailableOnly()
                ).stream()
                .filter(p -> p.getCategory() == null || p.getCategory().isActive())
                .filter(p -> p.getSubcategory() == null || p.getSubcategory().isActive())
                .filter(p -> p.getBrand() == null || p.getBrand().isActive())
                .collect(Collectors.toList());
    }

    public List<Product> quickSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return productRepository.findByNameContainingIgnoreCase(query.trim()).stream()
                .filter(p -> p.getStatus() == com.example.petmarket.enums.ProductStatus.ACTIVE)
                .filter(p -> p.getCategory() == null || p.getCategory().isActive())
                .filter(p -> p.getSubcategory() == null || p.getSubcategory().isActive())
                .filter(p -> p.getBrand() == null || p.getBrand().isActive())
                .collect(Collectors.toList());
    }

    public void recordSearch(String query, User user, String sessionId) {
        searchHistoryService.recordSearch(query, user, sessionId);
    }

    public List<SearchSuggestionService.SearchSuggestion> getSuggestions(String query, User user, String sessionId) {
        return searchSuggestionService.getSuggestions(query, user, sessionId);
    }
}