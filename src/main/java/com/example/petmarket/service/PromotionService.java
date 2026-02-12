package com.example.petmarket.service;

import com.example.petmarket.dto.PromotionDTO;
import com.example.petmarket.entity.*;
import com.example.petmarket.enums.PromotionType;
import com.example.petmarket.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;

    public Page<Promotion> getAllPromotions(Pageable pageable) {
        log.debug("Pobieranie wszystkich promocji, strona: {}", pageable.getPageNumber());
        return promotionRepository.findAll(pageable);
    }

    public Promotion getPromotionById(Long id) {
        log.debug("Pobieranie promocji o ID: {}", id);
        return promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promocja o ID " + id + " nie została znaleziona"));
    }

    @Transactional
    public Promotion createPromotion(PromotionDTO dto) {
        log.info("Tworzenie nowej promocji: {}", dto.getName());

        validatePromotionDTO(dto);

        if (dto.getCode() != null && !dto.getCode().isEmpty()) {
            if (promotionRepository.existsByCode(dto.getCode())) {
                throw new RuntimeException("Kod promocyjny '" + dto.getCode() + "' już istnieje");
            }
        }

        Promotion promotion = Promotion.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .discountPercentage(dto.getDiscountPercentage())
                .discountAmount(dto.getDiscountAmount())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .active(dto.isActive())
                .code(dto.getCode())
                .minOrderAmount(dto.getMinOrderAmount())
                .maxUsage(dto.getMaxUsage())
                .currentUsage(0)
                .priority(0)
                .stackable(false)
                .featured(false)
                .build();

        if (dto.getProductIds() != null && !dto.getProductIds().isEmpty()) {
            Set<Product> products = new HashSet<>(
                    productRepository.findAllById(dto.getProductIds())
            );
            promotion.setProducts(products);
        }

        Promotion saved = promotionRepository.save(promotion);
        log.info("Utworzono promocję o ID: {}", saved.getId());

        return saved;
    }

    @Transactional
    public Promotion updatePromotion(Long id, PromotionDTO dto) {
        log.info("Aktualizacja promocji o ID: {}", id);

        Promotion promotion = getPromotionById(id);
        validatePromotionDTO(dto);

        if (dto.getCode() != null && !dto.getCode().isEmpty()) {
            if (promotionRepository.existsByCodeAndIdNot(dto.getCode(), id)) {
                throw new RuntimeException("Kod promocyjny '" + dto.getCode() + "' już istnieje");
            }
        }

        promotion.setName(dto.getName());
        promotion.setDescription(dto.getDescription());
        promotion.setType(dto.getType());
        promotion.setDiscountPercentage(dto.getDiscountPercentage());
        promotion.setDiscountAmount(dto.getDiscountAmount());
        promotion.setStartDate(dto.getStartDate());
        promotion.setEndDate(dto.getEndDate());
        promotion.setActive(dto.isActive());
        promotion.setCode(dto.getCode());
        promotion.setMinOrderAmount(dto.getMinOrderAmount());
        promotion.setMaxUsage(dto.getMaxUsage());

        if (dto.getProductIds() != null) {
            promotion.getProducts().clear();
            if (!dto.getProductIds().isEmpty()) {
                Set<Product> products = new HashSet<>(
                        productRepository.findAllById(dto.getProductIds())
                );
                promotion.setProducts(products);
            }
        }

        Promotion updated = promotionRepository.save(promotion);
        log.info("Zaktualizowano promocję o ID: {}", id);

        return updated;
    }

    @Transactional
    public void deletePromotion(Long id) {
        log.info("Usuwanie promocji o ID: {}", id);

        Promotion promotion = getPromotionById(id);

        if (!promotion.getOrders().isEmpty()) {
            throw new RuntimeException(
                    "Nie można usunąć promocji, która była użyta w zamówieniach. " +
                            "Możesz ją zdezaktywować zamiast usuwać."
            );
        }

        promotion.getProducts().forEach(product ->
                product.getPromotions().remove(promotion)
        );
        promotion.getProducts().clear();

        promotionRepository.delete(promotion);
        log.info("Usunięto promocję o ID: {}", id);
    }

    @Transactional
    public void toggleActive(Long id) {
        Promotion promotion = getPromotionById(id);
        promotion.setActive(!promotion.isActive());
        promotionRepository.save(promotion);
        log.info("Zmieniono status promocji {} na: {}", id, promotion.isActive());
    }

    public Page<Promotion> getActivePromotions(Pageable pageable) {
        LocalDate now = LocalDate.now();
        return promotionRepository.findActivePromotionsForDate(now, pageable);
    }

    public Page<Promotion> getUpcomingPromotions(Pageable pageable) {
        LocalDate now = LocalDate.now();
        return promotionRepository.findUpcomingPromotions(now, pageable);
    }

    public Page<Promotion> getExpiredPromotions(Pageable pageable) {
        LocalDate now = LocalDate.now();
        return promotionRepository.findExpiredPromotions(now, pageable);
    }

    public Page<Promotion> getPromotionsByType(PromotionType type, Pageable pageable) {
        return promotionRepository.findByType(type, pageable);
    }

    @Transactional
    public void applyPromotionToProducts(Long promotionId, List<Long> productIds) {
        log.info("Stosowanie promocji {} do {} produktów", promotionId, productIds.size());

        Promotion promotion = getPromotionById(promotionId);
        List<Product> products = productRepository.findAllById(productIds);

        products.forEach(promotion::addProduct);

        promotionRepository.save(promotion);
        log.info("Zastosowano promocję do produktów");
    }

    @Transactional
    public void removePromotionFromProducts(Long promotionId, List<Long> productIds) {
        log.info("Usuwanie promocji {} z {} produktów", promotionId, productIds.size());

        Promotion promotion = getPromotionById(promotionId);
        List<Product> products = productRepository.findAllById(productIds);

        products.forEach(promotion::removeProduct);

        promotionRepository.save(promotion);
        log.info("Usunięto promocję z produktów");
    }


    public Map<String, Object> getPromotionStatistics(Long promotionId) {
        Promotion promotion = getPromotionById(promotionId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsage", promotion.getCurrentUsage());
        stats.put("maxUsage", promotion.getMaxUsage());
        stats.put("usagePercentage",
                promotion.getMaxUsage() != null ?
                        (promotion.getCurrentUsage() * 100.0 / promotion.getMaxUsage()) : null
        );
        stats.put("isActive", promotion.isCurrentlyActive());
        stats.put("daysRemaining",
                promotion.getEndDate() != null ?
                        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), promotion.getEndDate()) : null
        );
        stats.put("productsCount", promotion.getProducts().size());
        stats.put("ordersCount", promotion.getOrders().size());

        BigDecimal totalDiscount = promotion.getOrders().stream()
                .map(Order::getDiscountAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalDiscountAmount", totalDiscount);

        return stats;
    }

    @Transactional
    public Promotion duplicatePromotion(Long id) {
        log.info("Duplikowanie promocji o ID: {}", id);

        Promotion original = getPromotionById(id);

        Promotion duplicate = Promotion.builder()
                .name(original.getName() + " (kopia)")
                .description(original.getDescription())
                .type(original.getType())
                .discountPercentage(original.getDiscountPercentage())
                .discountAmount(original.getDiscountAmount())
                .startDate(LocalDate.now())
                .endDate(original.getEndDate())
                .active(false)
                .code(null)
                .minOrderAmount(original.getMinOrderAmount())
                .maxUsage(original.getMaxUsage())
                .currentUsage(0)
                .maxUsagePerUser(original.getMaxUsagePerUser())
                .stackable(original.isStackable())
                .priority(original.getPriority())
                .buyQuantity(original.getBuyQuantity())
                .getQuantity(original.getGetQuantity())
                .imageUrl(original.getImageUrl())
                .featured(false)
                .build();

        duplicate.setProducts(new HashSet<>(original.getProducts()));
        duplicate.setCategories(new HashSet<>(original.getCategories()));

        Promotion saved = promotionRepository.save(duplicate);
        log.info("Zduplikowano promocję, nowe ID: {}", saved.getId());

        return saved;
    }

    public boolean isCodeUnique(String code, Long excludeId) {
        if (code == null || code.isEmpty()) {
            return true;
        }

        if (excludeId != null) {
            return !promotionRepository.existsByCodeAndIdNot(code, excludeId);
        } else {
            return !promotionRepository.existsByCode(code);
        }
    }

    private void validatePromotionDTO(PromotionDTO dto) {
        switch (dto.getType()) {
            case PERCENTAGE_DISCOUNT:
                if (dto.getDiscountPercentage() == null ||
                        dto.getDiscountPercentage().compareTo(BigDecimal.ZERO) <= 0 ||
                        dto.getDiscountPercentage().compareTo(new BigDecimal("100")) > 0) {
                    throw new RuntimeException("Procent zniżki musi być między 0 a 100");
                }
                break;

            case FIXED_AMOUNT_DISCOUNT:
                if (dto.getDiscountAmount() == null ||
                        dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Kwota zniżki musi być większa od 0");
                }
                break;

            default:
                break;
        }

        if (dto.getEndDate() != null && dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new RuntimeException("Data zakończenia musi być późniejsza niż data rozpoczęcia");
        }
    }
}