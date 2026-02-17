package com.example.petmarket.service.product;

import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.service.interfaces.IProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements IProductQueryService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Product> getProductsBySubcategoryId(Long subcategoryId) {
        return productRepository.findBySubcategoryId(subcategoryId);
    }

    @Override
    public List<Product> getProductsByBrand(Long brandId) {
        return productRepository.findByBrandId(brandId);
    }

    @Override
    public List<Product> getProductsByCategory(Category category) {
        return productRepository.findByCategory(category).stream()
                .filter(p -> p.getStatus() != ProductStatus.DRAFT)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public List<Product> getAllProductsList() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getAllActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    @Override
    public Page<Product> searchProducts(String query, Pageable pageable) {
        return productRepository.searchProducts(query, null, null, null, false, pageable);
    }

    @Override
    public Page<Product> filterProducts(
            Long categoryId,
            Long subcategoryId,
            Long brandId,
            ProductStatus status,
            Pageable pageable) {
        return productRepository.filterProducts(categoryId, subcategoryId, brandId, status, pageable);
    }

    @Override
    public long getTotalProductsCount() {
        return productRepository.count();
    }
}