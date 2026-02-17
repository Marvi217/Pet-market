package com.example.petmarket.service.product;

import com.example.petmarket.controller.CategoryController.FilterTag;
import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Product;
import com.example.petmarket.service.interfaces.IProductFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryFilterService {

    private final IProductFilterService productService;

    public List<FilterTag> buildActiveFilters(List<Long> subcategory, List<Long> brands, Double minPrice, Double maxPrice) {
        List<FilterTag> activeFilters = new ArrayList<>();

        if (subcategory != null && !subcategory.isEmpty()) {
            activeFilters.add(new FilterTag("Subkategorie: " + subcategory.size(), "subcategory"));
        }
        if (brands != null && !brands.isEmpty()) {
            activeFilters.add(new FilterTag("Marki: " + brands.size(), "brands"));
        }
        if (minPrice != null) {
            activeFilters.add(new FilterTag("Cena od: " + minPrice + " zł", "minPrice"));
        }
        if (maxPrice != null) {
            activeFilters.add(new FilterTag("Cena do: " + maxPrice + " zł", "maxPrice"));
        }

        return activeFilters;
    }

    public Page<Product> getFilteredProducts(
            Category category,
            List<Long> subcategory,
            List<Long> brands,
            Double minPrice,
            Double maxPrice,
            String sort,
            int page) {

        Sort sortOrder = buildSortOrder(sort);
        Pageable pageable = PageRequest.of(page, 20, sortOrder);

        BigDecimal min = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal max = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;
        List<Long> subcategoryIds = subcategory != null && !subcategory.isEmpty() ? subcategory : null;
        List<Long> brandIds = brands != null && !brands.isEmpty() ? brands : null;

        return productService.getProductsByCategoryAdvanced(category, subcategoryIds, brandIds, min, max, pageable);
    }

    public Map<String, Object> getFilterData(String slug, Category category) {
        Map<String, Object> filterData = new HashMap<>();
        filterData.put("ratingCounts", productService.getRatingCountsForCategory(slug));
        filterData.put("subcategoryFilters", productService.getSubcategoryFilters(slug));
        filterData.put("brandFilters", productService.getBrandsWithCountByCategory(category));
        return filterData;
    }

    public Map<String, Object> getFilterCounts(
            Category category,
            String slug,
            List<Long> subcategory,
            List<Long> brands,
            Double minPrice,
            Double maxPrice) {

        BigDecimal min = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal max = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;
        List<Long> subcategoryIds = subcategory != null && !subcategory.isEmpty() ? subcategory : null;
        List<Long> brandIds = brands != null && !brands.isEmpty() ? brands : null;

        List<Map<String, Object>> brandCounts = productService.getBrandsWithCountByCategoryFiltered(category, subcategoryIds, min, max);
        List<Map<String, Object>> subcategoryCounts = productService.getSubcategoryCountsFiltered(slug, brandIds, min, max);

        Map<String, Object> result = new HashMap<>();
        result.put("brandCounts", brandCounts);
        result.put("subcategoryCounts", subcategoryCounts);

        return result;
    }

    private Sort buildSortOrder(String sort) {
        return switch (sort != null ? sort : "newest") {
            case "price-asc" -> Sort.by("price").ascending();
            case "price-desc" -> Sort.by("price").descending();
            case "popularity" -> Sort.by("stockQuantity").descending();
            case "rating" -> Sort.by("rating").descending();
            case "name-asc" -> Sort.by("name").ascending();
            case "name-desc" -> Sort.by("name").descending();
            default -> Sort.by("id").descending();
        };
    }
}