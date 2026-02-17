package com.example.petmarket.service;

import com.example.petmarket.dto.ProductDTO;
import com.example.petmarket.entity.*;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final BrandRepository brandRepository;
    private final FileStorageService fileStorageService;

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

    public List<Product> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> getProductsBySubcategoryId(Long subcategoryId) {
        return productRepository.findBySubcategoryId(subcategoryId);
    }

    public List<Product> getProductsByBrand(Long brandId) {
        return productRepository.findByBrandId(brandId);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public Page<Product> getProductsByCategory(Category category, Brand brand,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice,
                                               Pageable pageable) {
        return productRepository.findFilteredProducts(category, brand, minPrice, maxPrice, pageable);
    }

    public List<Product> getProductsByCategory(Category category) {
        return productRepository.findByCategory(category);
    }

    public Map<Integer, Long> getRatingCountsForCategory(String slug) {
        List<Object[]> results = productRepository.countProductsByRatingForCategory(slug);

        return results.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).intValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

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

    public Page<Product> getAllProducts(Pageable pageable) {
        log.debug("Pobieranie wszystkich produktów, strona: {}", pageable.getPageNumber());
        return productRepository.findAll(pageable);
    }

    public List<Product> getAllActiveProducts() {
        log.debug("Pobieranie wszystkich aktywnych produktów");
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    @Transactional
    public Product updateProduct(Long id, ProductDTO dto) {
        log.info("Aktualizacja produktu o ID: {}", id);

        Product product = getProductById(id);
        validateProductDTO(dto);

        if (dto.getSku() != null && !dto.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(dto.getSku())) {
                throw new RuntimeException("Produkt z SKU '" + dto.getSku() + "' już istnieje");
            }
        }

        Subcategory subcategory = subcategoryRepository.findById(dto.getSubcategoryId())
                .orElseThrow(() -> new RuntimeException("Subkategoria nie znaleziona"));

        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Marka nie znaleziona"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountedPrice(dto.getDiscountedPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setStatus(dto.getStatus());
        product.setSubcategory(subcategory);
        product.setCategory(subcategory.getCategory());
        product.setBrand(brand);
        product.setSku(dto.getSku());
        product.setWeight(dto.getWeight() != null ? dto.getWeight() : null);
        product.setDimensions(dto.getDimensions());

        Product updated = productRepository.save(product);
        log.info("Zaktualizowano produkt o ID: {}", id);

        return updated;
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Usuwanie produktu o ID: {}", id);

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
        log.info("Usunięto produkt o ID: {}", id);
    }

    public Page<Product> searchProducts(String query, Pageable pageable) {
        return productRepository.searchProducts(query, null, null, null, false, pageable);
    }

    public Page<Product> filterProducts(
            Long categoryId,
            Long subcategoryId,
            Long brandId,
            ProductStatus status,
            Pageable pageable) {

        log.debug("Filtrowanie produktów - kategoria: {}, subkategoria: {}, marka: {}, status: {}",
                categoryId, subcategoryId, brandId, status);

        return productRepository.filterProducts(categoryId, subcategoryId, brandId, status, pageable);
    }

    @Transactional
    public void changeStatus(Long id, ProductStatus status) {
        Product product = getProductById(id);
        product.setStatus(status);
        productRepository.save(product);
        log.info("Zmieniono status produktu {} na: {}", id, status);
    }


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
        log.info("Szybka aktualizacja produktu {}", id);
    }

    public String exportToCSV(
            String search,
            Long categoryId,
            Long subcategoryId,
            Long brandId,
            ProductStatus status) {

        log.info("Eksportowanie produktów do CSV");
        return "products_export_" + System.currentTimeMillis() + ".csv";
    }

    private void validateProductDTO(ProductDTO dto) {
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Cena musi być większa od 0");
        }

        if (dto.getDiscountedPrice() != null &&
                dto.getDiscountedPrice().compareTo(dto.getPrice()) >= 0) {
            throw new RuntimeException("Cena promocyjna musi być niższa niż cena regularna");
        }

        if (dto.getStockQuantity() == null || dto.getStockQuantity() < 0) {
            throw new RuntimeException("Stan magazynowy nie może być ujemny");
        }
    }
}