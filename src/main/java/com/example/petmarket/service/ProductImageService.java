package com.example.petmarket.service;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.ProductImage;
import com.example.petmarket.repository.ProductImageRepository;
import com.example.petmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public List<ProductImage> getProductImages(Long productId) {
        log.debug("Pobieranie obrazów dla produktu: {}", productId);
        return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
    }

    public ProductImage getImageById(Long imageId) {
        return productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Obraz o ID " + imageId + " nie został znaleziony"));
    }

    @Transactional
    public void addImage(Long productId, MultipartFile imageFile) throws IOException {
        log.info("Dodawanie obrazu do produktu: {}", productId);

        if (imageFile == null || imageFile.isEmpty()) {
            throw new RuntimeException("Plik obrazu jest pusty");
        }

        if (!fileStorageService.isValidImageFile(imageFile)) {
            throw new RuntimeException("Nieprawidłowy format pliku. Obsługiwane formaty: JPG, PNG, GIF, WEBP");
        }

        long maxSize = 5 * 1024 * 1024;
        if (!fileStorageService.isValidFileSize(imageFile, maxSize)) {
            throw new RuntimeException("Plik jest za duży. Maksymalny rozmiar: 5MB");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produkt nie znaleziony"));

        String imageUrl = fileStorageService.storeFile(imageFile, "products");

        int nextOrder = productImageRepository.getMaxDisplayOrder(productId).orElse(0) + 1;

        ProductImage productImage = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .isMain(product.getImages().isEmpty())
                .displayOrder(nextOrder)
                .altText(product.getName())
                .title(product.getName())
                .mimeType(imageFile.getContentType())
                .fileSize(imageFile.getSize())
                .build();

        ProductImage saved = productImageRepository.save(productImage);

        if (product.getImages().isEmpty() || product.getImageUrl() == null) {
            product.setImageUrl(imageUrl);
            productRepository.save(product);
        }

        log.info("Dodano obraz {} do produktu {}", saved.getId(), productId);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        log.info("Usuwanie obrazu: {}", imageId);

        ProductImage image = getImageById(imageId);
        Product product = image.getProduct();
        boolean wasMain = image.isMain();

        try {
            fileStorageService.deleteFile(image.getImageUrl());
        } catch (Exception e) {
            log.warn("Nie udało się usunąć pliku: {}", image.getImageUrl(), e);
        }

        productImageRepository.delete(image);

        if (wasMain) {
            List<ProductImage> remainingImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
            if (!remainingImages.isEmpty()) {
                ProductImage newMain = remainingImages.get(0);
                newMain.setMain(true);
                productImageRepository.save(newMain);

                product.setImageUrl(newMain.getImageUrl());
                productRepository.save(product);
            } else {
                product.setImageUrl(null);
                productRepository.save(product);
            }
        }

        log.info("Usunięto obraz: {}", imageId);
    }

    @Transactional
    public void setMainImage(Long productId, Long imageId) {
        log.info("Ustawianie głównego obrazu {} dla produktu {}", imageId, productId);

        ProductImage image = getImageById(imageId);
        if (!image.getProduct().getId().equals(productId)) {
            throw new RuntimeException("Obraz nie należy do tego produktu");
        }

        productImageRepository.clearMainFlag(productId);

        image.setMain(true);
        productImageRepository.save(image);

        Product product = image.getProduct();
        product.setImageUrl(image.getImageUrl());
        productRepository.save(product);

        log.info("Ustawiono obraz {} jako główny dla produktu {}", imageId, productId);
    }
}