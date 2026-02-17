package com.example.petmarket.service.stock;

import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final ProductRepository productRepository;

    @Transactional
    public void increaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produkt o id: " + productId + " nie istnieję"));

        product.increaseStock(quantity);

        if (product.getStatus() == ProductStatus.SOLDOUT && product.getStockQuantity() > 0) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        productRepository.save(product);
    }

    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produkt o id: " + productId + " nie istnieję"));

        product.decreaseStock(quantity);

        if (product.getStockQuantity() <= 0) {
            product.setStatus(ProductStatus.SOLDOUT);
        }

        productRepository.save(product);
    }

    @Transactional
    public void updateStock(Long productId, int newQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produkt o id: " + productId + " nie istnieję"));

        int oldStock = product.getStockQuantity();
        product.setStockQuantity(newQuantity);

        if (oldStock == 0 && newQuantity > 0) {
            product.setStatus(ProductStatus.ACTIVE);
        } else if (newQuantity == 0) {
            product.setStatus(ProductStatus.SOLDOUT);
        }

        productRepository.save(product);
    }

    public Page<Product> getLowStockProducts(Pageable pageable) {
        return productRepository.findLowStockProductsPage(pageable);
    }

    public long getLowStockProductsCount() {
        return productRepository.countLowStockProducts();
    }

    public long getOutOfStockProductsCount() {
        return productRepository.countByStockQuantity(0);
    }

}