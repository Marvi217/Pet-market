package com.example.petmarket.service.stock;

import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.exception.InsufficientStockException;
import org.springframework.stereotype.Component;

@Component
public class StockManager {


    public void increaseStock(Product product, int quantity) {
        product.setStockQuantity(product.getStockQuantity() + quantity);

        if (product.getStockQuantity() > 0 && product.getStatus() == ProductStatus.SOLDOUT) {
            product.setStatus(ProductStatus.ACTIVE);
        }
    }

    public boolean isAvailable(Product product) {
        return product.getStatus() == ProductStatus.ACTIVE &&
                product.getStockQuantity() != null &&
                product.getStockQuantity() > 0;
    }

    public String getUnavailableMessage(Product product) {
        if (product.getStatus() == ProductStatus.SOLDOUT) {
            return "Wyprzedany";
        }
        if (product.getStatus() == ProductStatus.ACTIVE &&
                (product.getStockQuantity() == null || product.getStockQuantity() <= 0)) {
            return "Wyprzedany";
        }
        return "Niedostępny";
    }

    public void validateStock(Product product, int requestedQuantity) {
        if (!isAvailable(product)) {
            throw new InsufficientStockException("Produkt " + product.getName() + " jest niedostępny");
        }

        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                    "Niewystarczająca ilość produktu " + product.getName() +
                            ". Dostępne: " + product.getStockQuantity());
        }
    }
}