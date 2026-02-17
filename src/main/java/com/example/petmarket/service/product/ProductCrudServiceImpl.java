package com.example.petmarket.service.product;

import com.example.petmarket.dto.ProductDTO;
import com.example.petmarket.entity.Brand;
import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Subcategory;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.BrandRepository;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.repository.SubcategoryRepository;
import com.example.petmarket.service.FileStorageService;
import com.example.petmarket.service.interfaces.IProductCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCrudServiceImpl implements IProductCrudService {

    private final ProductRepository productRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final BrandRepository brandRepository;
    private final FileStorageService fileStorageService;
    private final ProductValidationService productValidationService;

    @Override
    @Transactional
    public Product save(Product product) {
        if (product.getStockQuantity() != null && product.getStockQuantity() <= 0) {
            product.setStatus(ProductStatus.SOLDOUT);
        } else if (product.getStatus() == ProductStatus.SOLDOUT && product.getStockQuantity() > 0) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        if (product.getWeight() == null) product.setWeight(BigDecimal.ZERO);
        if (product.getPrice() == null) product.setPrice(BigDecimal.ZERO);

        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductDTO dto) {
        log.info("Aktualizacja produktu o ID: {}", id);

        Product product = getProductById(id);
        productValidationService.validateProductDTO(dto);
        productValidationService.validateSkuUniqueness(dto.getSku(), product.getSku());

        Subcategory subcategory = subcategoryRepository.findById(dto.getSubcategoryId())
                .orElseThrow(() -> new RuntimeException("Subkategoria nie znaleziona"));

        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Marka nie znaleziona"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountedPrice(dto.getDiscountedPrice());
        product.setStockQuantity(dto.getStockQuantity());
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }
        product.setSubcategory(subcategory);
        product.setCategory(subcategory.getCategory());
        product.setBrand(brand);
        product.setSku(dto.getSku());
        product.setWeight(dto.getWeight() != null ? dto.getWeight() : null);
        product.setDimensions(dto.getDimensions());
        product.setIngredients(dto.getIngredients());

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);

        if (productRepository.isProductInOrders(id)) {
            throw new RuntimeException(
                    "Nie można usunąć produktu, który był już zamówiony. " +
                            "Możesz zmienić jego status na DISCONTINUED."
            );
        }

        if (product.getImages() != null) {
            product.getImages().forEach(image -> {
                try {
                    fileStorageService.deleteFile(image.getImageUrl());
                } catch (Exception e) {
                    log.warn("Nie udało się usunąć obrazu: {}", image.getImageUrl(), e);
                }
            });
        }

        productRepository.delete(product);
    }

    @Override
    @Transactional
    public void changeStatus(Long id, ProductStatus status) {
        Product product = getProductById(id);
        product.setStatus(status);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void quickUpdate(Long id, BigDecimal price, BigDecimal discountedPrice, Integer stockQuantity) {
        Product product = getProductById(id);

        if (price != null) {
            product.setPrice(price);
        }

        if (discountedPrice != null) {
            product.setDiscountedPrice(discountedPrice);
        }

        if (stockQuantity != null) {
            int oldStock = product.getStockQuantity();
            product.setStockQuantity(stockQuantity);

            if (oldStock == 0 && stockQuantity > 0) {
                product.setStatus(ProductStatus.ACTIVE);
            } else if (stockQuantity == 0) {
                product.setStatus(ProductStatus.SOLDOUT);
            }
        }

        productRepository.save(product);
    }
}