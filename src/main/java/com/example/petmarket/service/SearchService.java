package com.example.petmarket.service;

import com.example.petmarket.dto.ProductSearchDto;
import com.example.petmarket.entity.Product;
import com.example.petmarket.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;


    public List<Product> searchProducts(ProductSearchDto searchDto) {
        return productRepository.searchProducts(
                searchDto.getQuery(),
                searchDto.getCategoryId(),
                searchDto.getMinPrice(),
                searchDto.getMaxPrice(),
                searchDto.getAvailableOnly()
        );
    }

    public List<Product> quickSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return productRepository.findByNameContainingIgnoreCase(query.trim());
    }
}