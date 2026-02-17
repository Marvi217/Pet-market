package com.example.petmarket.service.product;

import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.ProductImage;
import com.example.petmarket.repository.ProductImageRepository;
import com.example.petmarket.repository.ProductRepository;
import com.example.petmarket.service.FileStorageService;
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
        return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
    }

    public ProductImage getImageById(Long imageId) {
        return productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Obraz o ID " + imageId + " nie został znaleziony"));
    }

    @Transactional
    public ProductImage addImage(Long productId, MultipartFile imageFile) throws IOException {
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

        return saved;
    }

    @Transactional
    public void deleteImage(Long imageId) {
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

    }

    @Transactional
    public void setMainImage(Long productId, Long imageId) {
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
    }

    @Transactional
    public void moveUp(Long imageId) {
        ProductImage image = getImageById(imageId);
        int currentOrder = image.getDisplayOrder();
        Long productId = image.getProduct().getId();

        if (currentOrder > 1) {
            productImageRepository.findByProductIdAndDisplayOrder(productId, currentOrder - 1)
                    .ifPresent(prevImage -> {
                        prevImage.setDisplayOrder(currentOrder);
                        image.setDisplayOrder(currentOrder - 1);
                        productImageRepository.save(prevImage);
                        productImageRepository.save(image);
                    });

            updateMainImageByOrder(productId);
        }
    }

    @Transactional
    public void moveDown(Long imageId) {
        ProductImage image = getImageById(imageId);
        int currentOrder = image.getDisplayOrder();
        Long productId = image.getProduct().getId();

        productImageRepository.findByProductIdAndDisplayOrder(productId, currentOrder + 1)
                .ifPresent(nextImage -> {
                    nextImage.setDisplayOrder(currentOrder);
                    image.setDisplayOrder(currentOrder + 1);
                    productImageRepository.save(nextImage);
                    productImageRepository.save(image);
                });

        updateMainImageByOrder(productId);
    }

    @Transactional
    public void updateMainImageByOrder(Long productId) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        if (images.isEmpty()) {
            return;
        }

        productImageRepository.clearMainFlag(productId);

        ProductImage firstImage = images.get(0);
        firstImage.setMain(true);
        productImageRepository.save(firstImage);

        Product product = firstImage.getProduct();
        product.setImageUrl(firstImage.getImageUrl());
        productRepository.save(product);
    }
}