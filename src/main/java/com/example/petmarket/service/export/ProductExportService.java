package com.example.petmarket.service.export;

import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductExportService {

    private static final int MAX_EXPORT_RECORDS = 10000;

    private final ProductRepository productRepository;
    private final Map<String, ProductExporter> exporters;

    public ProductExportService(ProductRepository productRepository, List<ProductExporter> exporterList) {
        this.productRepository = productRepository;
        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(
                        e -> e.getFormat().toUpperCase(),
                        Function.identity()
                ));
    }
    public void exportProducts(
            String format,
            String search,
            Long categoryId,
            Long subcategoryId,
            Long brandId,
            ProductStatus status,
            Writer writer) {

        ProductExporter exporter = getExporter(format);
        List<Product> products = fetchProducts(search, categoryId, subcategoryId, brandId, status);
        exporter.export(products, writer);
    }

    public ProductExporter getExporter(String format) {
        ProductExporter exporter = exporters.get(format.toUpperCase());
        if (exporter == null) {
            throw new IllegalArgumentException("Nieobsługiwany format eksportu: " + format);
        }
        return exporter;
    }

    public List<String> getSupportedFormats() {
        return List.copyOf(exporters.keySet());
    }

    private List<Product> fetchProducts(
            String search,
            Long categoryId,
            Long subcategoryId,
            Long brandId,
            ProductStatus status) {

        if (search != null && !search.isEmpty()) {
            return productRepository.searchProducts(search, null, null, null, false,
                    PageRequest.of(0, MAX_EXPORT_RECORDS)).getContent();
        } else if (categoryId != null || subcategoryId != null || brandId != null || status != null) {
            return productRepository.filterProducts(categoryId, subcategoryId, brandId, status,
                    PageRequest.of(0, MAX_EXPORT_RECORDS)).getContent();
        } else {
            return productRepository.findAll(PageRequest.of(0, MAX_EXPORT_RECORDS)).getContent();
        }
    }
}