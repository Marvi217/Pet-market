package com.example.petmarket.service;

import com.example.petmarket.dto.CategoryDTO;
import com.example.petmarket.entity.Category;
import com.example.petmarket.repository.CategoryRepository;
import com.example.petmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final ProductRepository productRepository;

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<Category> getTopCategories(int limit) {
        return categoryRepository.findTopCategoriesByProductCount(PageRequest.of(0, limit));
    }

    public double calculateCategoryPercentage(Category category) {
        long totalProducts = productRepository.count();
        if (totalProducts == 0) return 0.0;
        long categoryProducts = productRepository.countByCategory(category);
        return Math.round((double) categoryProducts / totalProducts * 100);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Category not found: " + slug));
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + id));
    }

    public Page<Category> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    @Transactional
    public Category createCategory(CategoryDTO dto, MultipartFile imageFile) throws IOException {
        log.info("Tworzenie nowej kategorii: {}", dto.getName());

        if (categoryRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Kategoria o nazwie '" + dto.getName() + "' już istnieje");
        }

        if (categoryRepository.existsBySlug(dto.getSlug())) {
            throw new RuntimeException("Kategoria o slug '" + dto.getSlug() + "' już istnieje");
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setActive(dto.isActive());

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(imageFile, "categories");
            category.setImageUrl(imageUrl);
        }

        Category saved = categoryRepository.save(category);
        log.info("Utworzono kategorię o ID: {}", saved.getId());

        return saved;
    }

    @Transactional
    public Category updateCategory(Long id, CategoryDTO dto, MultipartFile imageFile) throws IOException {
        if (fileStorageService == null) {
            throw new RuntimeException("FileStorageService nie został zainicjalizowany");
        }

        log.info("Aktualizacja kategorii o ID: {}", id);

        Category category = getCategoryById(id);

        if (!category.getName().equals(dto.getName())) {
            if (categoryRepository.existsByName(dto.getName())) {
                throw new RuntimeException("Kategoria o nazwie '" + dto.getName() + "' już istnieje");
            }
        }

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setActive(dto.isActive());

        if (imageFile != null && !imageFile.isEmpty()) {
            if (category.getImageUrl() != null) {
                try {
                    fileStorageService.deleteFile(category.getImageUrl());
                } catch (Exception e) {
                    log.warn("Nie udało się usunąć starego obrazu: {}", category.getImageUrl(), e);
                }
            }

            String imageUrl = fileStorageService.storeFile(imageFile, "categories");
            category.setImageUrl(imageUrl);
        }

        Category updated = categoryRepository.save(category);
        log.info("Zaktualizowano kategorię o ID: {}", id);

        return updated;
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Usuwanie kategorii o ID: {}", id);

        Category category = getCategoryById(id);

        if (categoryRepository.hasSubcategories(id)) {
            throw new RuntimeException(
                    "Nie można usunąć kategorii, która zawiera subkategorie. " +
                            "Najpierw usuń wszystkie subkategorie z tej kategorii."
            );
        }

        if (fileStorageService != null && category.getImageUrl() != null) {
            try {
                fileStorageService.deleteFile(category.getImageUrl());
            } catch (Exception e) {
                log.warn("Nie udało się usunąć obrazu: {}", category.getImageUrl(), e);
            }
        }

        categoryRepository.delete(category);
        log.info("Usunięto kategorię o ID: {}", id);
    }

    public Page<Category> searchCategories(String query, Pageable pageable) {
        return categoryRepository.searchCategories(query, pageable);
    }

    @Transactional
    public void toggleActive(Long id) {
        Category category = getCategoryById(id);
        category.setActive(!category.isActive());
        categoryRepository.save(category);
        log.info("Zmieniono status kategorii {} na: {}", id, category.isActive());
    }
}