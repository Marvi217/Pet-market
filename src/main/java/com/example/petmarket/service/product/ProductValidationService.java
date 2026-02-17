package com.example.petmarket.service.product;

import com.example.petmarket.dto.ProductDTO;
import com.example.petmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductValidationService {

    private final ProductRepository productRepository;

    public void validateProductDTO(ProductDTO dto) {
        validatePrice(dto.getPrice());
        validateDiscountedPrice(dto.getPrice(), dto.getDiscountedPrice());
        validateStockQuantity(dto.getStockQuantity());
    }

    public void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Cena musi być większa od 0");
        }
    }

    public void validateDiscountedPrice(BigDecimal price, BigDecimal discountedPrice) {
        if (discountedPrice != null && discountedPrice.compareTo(price) >= 0) {
            throw new RuntimeException("Cena promocyjna musi być niższa niż cena regularna");
        }
    }

    public void validateStockQuantity(Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new RuntimeException("Stan magazynowy nie może być ujemny");
        }
    }

    public void validateSkuUniqueness(String sku, String currentSku) {
        if (sku != null && !sku.equals(currentSku)) {
            if (productRepository.existsBySku(sku)) {
                throw new RuntimeException("Produkt z SKU '" + sku + "' już istnieje");
            }
        }
    }

    public boolean isSkuUnique(String sku) {
        return !productRepository.existsBySku(sku);
    }
}