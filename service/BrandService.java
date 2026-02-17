package com.example.petmarket.service;

import com.example.petmarket.dto.BrandDTO;
import com.example.petmarket.entity.Brand;
import com.example.petmarket.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandService {

    private final BrandRepository brandRepository;
    private final FileStorageService fileStorageService;

    public Optional<Brand> findById(Long id) {
        return brandRepository.findById(id);
    }

    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    public Optional<Brand> getBrandBySlug(String slug) {
        return brandRepository.findBySlug(slug);
    }

    public Page<Brand> getAllBrands(Pageable pageable) {
        return brandRepository.findAll(pageable);
    }

    public List<Brand> getAllActiveBrands() {
        return brandRepository.findByActiveTrue();
    }

    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marka o ID " + id + " nie została znaleziona"));
    }


    @Transactional
    public Brand createBrand(BrandDTO dto, MultipartFile logoFile) throws IOException {
        if (fileStorageService == null) {
            throw new RuntimeException("FileStorageService nie został zainicjalizowany");
        }

        log.info("Tworzenie nowej marki: {}", dto.getName());

        if (brandRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Marka o nazwie '" + dto.getName() + "' już istnieje");
        }

        Brand brand = new Brand();
        brand.setName(dto.getName());
        brand.setDescription(dto.getDescription());
        brand.setActive(dto.isActive());
        brand.setWebsite(dto.getWebsite());
        brand.setCountry(dto.getCountry());

        if (logoFile != null && !logoFile.isEmpty()) {
            String logoUrl = fileStorageService.storeFile(logoFile, "brands");
            brand.setLogoUrl(logoUrl);
        }

        Brand saved = brandRepository.save(brand);
        log.info("Utworzono markę o ID: {}", saved.getId());

        return saved;
    }

    @Transactional
    public Brand updateBrand(Long id, BrandDTO dto, MultipartFile logoFile) throws IOException {
        if (fileStorageService == null) {
            throw new RuntimeException("FileStorageService nie został zainicjalizowany");
        }

        log.info("Aktualizacja marki o ID: {}", id);

        Brand brand = getBrandById(id);

        if (!brand.getName().equals(dto.getName())) {
            if (brandRepository.existsByName(dto.getName())) {
                throw new RuntimeException("Marka o nazwie '" + dto.getName() + "' już istnieje");
            }
        }

        brand.setName(dto.getName());
        brand.setDescription(dto.getDescription());
        brand.setActive(dto.isActive());
        brand.setWebsite(dto.getWebsite());
        brand.setCountry(dto.getCountry());

        if (logoFile != null && !logoFile.isEmpty()) {
            if (brand.getLogoUrl() != null) {
                try {
                    fileStorageService.deleteFile(brand.getLogoUrl());
                } catch (Exception e) {
                    log.warn("Nie udało się usunąć starego logo: {}", brand.getLogoUrl(), e);
                }
            }

            String logoUrl = fileStorageService.storeFile(logoFile, "brands");
            brand.setLogoUrl(logoUrl);
        }

        Brand updated = brandRepository.save(brand);
        log.info("Zaktualizowano markę o ID: {}", id);

        return updated;
    }

    @Transactional
    public void deleteBrand(Long id) {
        log.info("Usuwanie marki o ID: {}", id);

        Brand brand = getBrandById(id);

        if (brandRepository.hasProducts(id)) {
            throw new RuntimeException(
                    "Nie można usunąć marki, która zawiera produkty. " +
                            "Najpierw usuń lub przenieś wszystkie produkty tej marki."
            );
        }

        if (fileStorageService != null && brand.getLogoUrl() != null) {
            try {
                fileStorageService.deleteFile(brand.getLogoUrl());
            } catch (Exception e) {
                log.warn("Nie udało się usunąć logo: {}", brand.getLogoUrl(), e);
            }
        }

        brandRepository.delete(brand);
        log.info("Usunięto markę o ID: {}", id);
    }

    public Page<Brand> searchBrands(String query, Pageable pageable) {
        return brandRepository.searchBrands(query, pageable);
    }

    @Transactional
    public void toggleActive(Long id) {
        Brand brand = getBrandById(id);
        brand.setActive(!brand.isActive());
        brandRepository.save(brand);
        log.info("Zmieniono status marki {} na: {}", id, brand.isActive());
    }

    public long getTotalBrandsCount() {
        return brandRepository.count();
    }

    public long getActiveBrandsCount() {
        return brandRepository.countByActiveTrue();
    }
}