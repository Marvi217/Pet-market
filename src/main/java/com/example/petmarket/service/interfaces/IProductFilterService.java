package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IProductFilterService {

    Page<Product> getProductsByCategoryAdvanced(Category category,
                                                List<Long> subcategoryIds,
                                                List<Long> brandIds,
                                                BigDecimal minPrice,
                                                BigDecimal maxPrice,
                                                Pageable pageable);

    List<Map<String, Object>> getBrandsWithCountByCategory(Category category);

    List<Map<String, Object>> getBrandsWithCountByCategoryFiltered(
            Category category, List<Long> subcategoryIds, BigDecimal minPrice, BigDecimal maxPrice);

    List<Map<String, Object>> getSubcategoryCountsFiltered(
            String categorySlug, List<Long> brandIds, BigDecimal minPrice, BigDecimal maxPrice);

    Map<Integer, Long> getRatingCountsForCategory(String slug);

    List<Map<String, Object>> getSubcategoryFilters(String parentSlug);
}