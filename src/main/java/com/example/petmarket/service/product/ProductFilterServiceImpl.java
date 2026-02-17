package com.example.petmarket.service.product;

import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Product;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.service.interfaces.IProductFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductFilterServiceImpl implements IProductFilterService {

    private final ProductRepository productRepository;

    @Override
    public Page<Product> getProductsByCategoryAdvanced(Category category,
                                                       List<Long> subcategoryIds,
                                                       List<Long> brandIds,
                                                       BigDecimal minPrice,
                                                       BigDecimal maxPrice,
                                                       Pageable pageable) {
        return productRepository.findFilteredProductsAdvanced(category, subcategoryIds, brandIds, minPrice, maxPrice, pageable);
    }

    @Override
    public List<Map<String, Object>> getBrandsWithCountByCategory(Category category) {
        List<Object[]> results = productRepository.findBrandsWithCountByCategory(category);
        return getMapsProducts(results);
    }

    @Override
    public List<Map<String, Object>> getBrandsWithCountByCategoryFiltered(
            Category category, List<Long> subcategoryIds, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Object[]> results = productRepository.findBrandsWithCountByCategoryFiltered(
                category, subcategoryIds, minPrice, maxPrice);
        return getMapsProducts(results);
    }

    @Override
    public List<Map<String, Object>> getSubcategoryCountsFiltered(
            String categorySlug, List<Long> brandIds, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Object[]> results = productRepository.findSubcategoryCountsFiltered(
                categorySlug, brandIds, minPrice, maxPrice);
        return results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0]);
            map.put("name", row[1]);
            map.put("productCount", ((Number) row[2]).longValue());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<Integer, Long> getRatingCountsForCategory(String slug) {
        List<Object[]> results = productRepository.countProductsByRatingForCategory(slug);

        return results.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).intValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

    @Override
    public List<Map<String, Object>> getSubcategoryFilters(String parentSlug) {
        List<Object[]> results = productRepository.findSubcategoriesByCategorySlug(parentSlug);
        return results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", row[0]);
            map.put("count", ((Number) row[1]).longValue());
            map.put("slug", row[2]);
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getMapsProducts(List<Object[]> results) {
        return results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0]);
            map.put("name", row[1]);
            map.put("count", ((Number) row[2]).longValue());
            return map;
        }).collect(Collectors.toList());
    }
}