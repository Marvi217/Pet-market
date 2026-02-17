package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IProductQueryService {

    List<Product> getProductsByCategoryId(Long categoryId);

    List<Product> getProductsBySubcategoryId(Long subcategoryId);

    List<Product> getProductsByBrand(Long brandId);

    List<Product> getProductsByCategory(Category category);

    Page<Product> getAllProducts(Pageable pageable);

    List<Product> getAllProductsList();

    List<Product> getAllActiveProducts();

    Page<Product> searchProducts(String query, Pageable pageable);

    Page<Product> filterProducts(Long categoryId, Long subcategoryId, Long brandId,
                                 ProductStatus status, Pageable pageable);

    long getTotalProductsCount();
}