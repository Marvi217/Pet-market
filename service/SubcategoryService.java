package com.example.petmarket.service;

import com.example.petmarket.dto.SubcategoryDTO;
import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Subcategory;
import com.example.petmarket.repository.CategoryRepository;
import com.example.petmarket.repository.SubcategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    public Optional<Subcategory> findById(Long id) {
        return subcategoryRepository.findById(id);
    }

    public Page<Subcategory> getAllSubcategories(Pageable pageable) {
        log.debug("Pobieranie wszystkich subkategorii, strona: {}", pageable.getPageNumber());
        return subcategoryRepository.findAll(pageable);
    }

    public List<Subcategory> getAllActiveSubcategories() {
        log.debug("Pobieranie wszystkich aktywnych subkategorii");
        return subcategoryRepository.findByActiveTrue();
    }

    public Subcategory getSubcategoryById(Long id) {
        log.debug("Pobieranie subkategorii o ID: {}", id);
        return subcategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subkategoria o ID " + id + " nie została znaleziona"));
    }

    public Page<Subcategory> getSubcategoriesByCategory(Long categoryId, Pageable pageable) {
        log.debug("Pobieranie subkategorii dla kategorii: {}", categoryId);
        return subcategoryRepository.findByCategoryId(categoryId, pageable);
    }

    public List<Subcategory> getActiveSubcategoriesByCategory(Long categoryId) {
        log.debug("Pobieranie aktywnych subkategorii dla kategorii: {}", categoryId);
        return subcategoryRepository.findByCategoryIdAndActiveTrue(categoryId);
    }

    @Transactional
    public Subcategory createSubcategory(SubcategoryDTO dto, MultipartFile imageFile) throws IOException {
        log.info("Tworzenie nowej subkategorii: {}", dto.getName());

        validateSubcategoryDTO(dto);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kategoria nie znaleziona"));

        if (subcategoryRepository.existsByNameAndCategoryId(dto.getName(), dto.getCategoryId())) {
            throw new RuntimeException("Subkategoria o nazwie '" + dto.getName() +
                    "' już istnieje w tej kategorii");
        }

        Subcategory subcategory = new Subcategory();
        subcategory.setName(dto.getName());
        subcategory.setDescription(dto.getDescription());
        subcategory.setCategory(category);
        subcategory.setActive(dto.isActive());

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(imageFile, "categories");
            subcategory.setImageUrl(imageUrl);
        }

        Subcategory saved = subcategoryRepository.save(subcategory);
        log.info("Utworzono subkategorię o ID: {}", saved.getId());

        return saved;
    }

    @Transactional
    public Subcategory updateSubcategory(Long id, SubcategoryDTO dto, MultipartFile imageFile) throws IOException {
        log.info("Aktualizacja subkategorii o ID: {}", id);

        Subcategory subcategory = getSubcategoryById(id);
        validateSubcategoryDTO(dto);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kategoria nie znaleziona"));

        if (!subcategory.getName().equals(dto.getName())) {
            if (subcategoryRepository.existsByNameAndCategoryId(dto.getName(), dto.getCategoryId())) {
                throw new RuntimeException("Subkategoria o nazwie '" + dto.getName() +
                        "' już istnieje w tej kategorii");
            }
        }

        subcategory.setName(dto.getName());
        subcategory.setDescription(dto.getDescription());
        subcategory.setCategory(category);
        subcategory.setActive(dto.isActive());

        if (imageFile != null && !imageFile.isEmpty()) {
            if (subcategory.getImageUrl() != null) {
                try {
                    fileStorageService.deleteFile(subcategory.getImageUrl());
                } catch (Exception e) {
                    log.warn("Nie udało się usunąć starego obrazu: {}", subcategory.getImageUrl(), e);
                }
            }

            String imageUrl = fileStorageService.storeFile(imageFile, "categories");
            subcategory.setImageUrl(imageUrl);
        }

        Subcategory updated = subcategoryRepository.save(subcategory);
        log.info("Zaktualizowano subkategorię o ID: {}", id);

        return updated;
    }

    @Transactional
    public void deleteSubcategory(Long id) {
        log.info("Usuwanie subkategorii o ID: {}", id);

        Subcategory subcategory = getSubcategoryById(id);

        if (subcategoryRepository.hasProducts(id)) {
            throw new RuntimeException(
                    "Nie można usunąć subkategorii, która zawiera produkty. " +
                            "Najpierw przenieś lub usuń wszystkie produkty z tej subkategorii."
            );
        }

        if (subcategory.getImageUrl() != null) {
            try {
                fileStorageService.deleteFile(subcategory.getImageUrl());
            } catch (Exception e) {
                log.warn("Nie udało się usunąć obrazu: {}", subcategory.getImageUrl(), e);
            }
        }

        subcategoryRepository.delete(subcategory);
        log.info("Usunięto subkategorię o ID: {}", id);
    }

    public Page<Subcategory> searchSubcategories(String query, Pageable pageable) {
        log.debug("Wyszukiwanie subkategorii: {}", query);
        return subcategoryRepository.searchSubcategories(query, pageable);
    }

    @Transactional
    public void toggleActive(Long id) {
        log.info("Przełączanie statusu aktywności dla subkategorii ID: {}", id);

        Subcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subkategoria o ID " + id + " nie została znaleziona"));

        subcategory.setActive(!subcategory.isActive());
        subcategoryRepository.save(subcategory);

        log.info("Status subkategorii ID {} zmieniony na: {}", id, subcategory.isActive());
    }

    private void validateSubcategoryDTO(SubcategoryDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("Nazwa subkategorii jest wymagana");
        }

        if (dto.getName().length() < 2 || dto.getName().length() > 100) {
            throw new RuntimeException("Nazwa subkategorii musi mieć od 2 do 100 znaków");
        }

        if (dto.getCategoryId() == null) {
            throw new RuntimeException("Kategoria nadrzędna jest wymagana");
        }
    }
}