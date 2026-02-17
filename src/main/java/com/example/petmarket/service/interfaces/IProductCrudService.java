package com.example.petmarket.service.interfaces;

import com.example.petmarket.dto.ProductDTO;
import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.ProductStatus;

import java.math.BigDecimal;

public interface IProductCrudService {

    Product save(Product product);

    Product getProductById(Long id);

    Product updateProduct(Long id, ProductDTO dto);

    void deleteProduct(Long id);

    void changeStatus(Long id, ProductStatus status);

    void quickUpdate(Long id, BigDecimal price, BigDecimal discountedPrice, Integer stockQuantity);
}