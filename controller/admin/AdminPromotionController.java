package com.example.petmarket.controller.admin;

import com.example.petmarket.dto.PromotionDTO;
import com.example.petmarket.entity.Promotion;
import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.PromotionType;
import com.example.petmarket.service.PromotionService;
import com.example.petmarket.service.ProductService;
import com.example.petmarket.service.CategoryService;
import com.example.petmarket.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;

    @GetMapping
    public String listPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) PromotionType type,
            Model model) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startDate").descending());
        Page<Promotion> promotions;

        if (status != null) {
            promotions = switch (status) {
                case "active" -> promotionService.getActivePromotions(pageRequest);
                case "upcoming" -> promotionService.getUpcomingPromotions(pageRequest);
                case "expired" -> promotionService.getExpiredPromotions(pageRequest);
                default -> promotionService.getAllPromotions(pageRequest);
            };
        } else if (type != null) {
            promotions = promotionService.getPromotionsByType(type, pageRequest);
        } else {
            promotions = promotionService.getAllPromotions(pageRequest);
        }

        model.addAttribute("promotions", promotions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", promotions.getTotalPages());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);
        model.addAttribute("promotionTypes", PromotionType.values());

        return "admin/promotions/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        PromotionDTO promotionDTO = new PromotionDTO();
        promotionDTO.setStartDate(LocalDate.now());
        promotionDTO.setEndDate(LocalDate.now().plusDays(30));
        promotionDTO.setActive(true);

        model.addAttribute("promotionDTO", promotionDTO);
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());

        return "admin/promotions/form";
    }

    @PostMapping
    public String createPromotion(
            @Valid @ModelAttribute PromotionDTO promotionDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (promotionDTO.getEndDate() != null &&
                promotionDTO.getStartDate().isAfter(promotionDTO.getEndDate())) {
            result.rejectValue("endDate", "error.promotionDTO",
                    "Data zakończenia musi być późniejsza niż data rozpoczęcia");
        }

        if (result.hasErrors()) {
            model.addAttribute("promotionTypes", PromotionType.values());
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("brands", brandService.getAllBrands());
            return "admin/promotions/form";
        }

        try {
            Promotion promotion = promotionService.createPromotion(promotionDTO);
            redirectAttributes.addFlashAttribute("success",
                    "Promocja '" + promotion.getName() + "' została utworzona pomyślnie");
            return "redirect:/admin/promotions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia promocji: " + e.getMessage());
            return "redirect:/admin/promotions/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Promotion promotion = promotionService.getPromotionById(id);

        if (promotion == null) {
            redirectAttributes.addFlashAttribute("error", "Promocja nie została znaleziona");
            return "redirect:/admin/promotions";
        }

        PromotionDTO promotionDTO = new PromotionDTO();
        promotionDTO.setId(promotion.getId());
        promotionDTO.setName(promotion.getName());
        promotionDTO.setDescription(promotion.getDescription());
        promotionDTO.setType(promotion.getType());
        promotionDTO.setDiscountPercentage(promotion.getDiscountPercentage());
        promotionDTO.setDiscountAmount(promotion.getDiscountAmount());
        promotionDTO.setStartDate(promotion.getStartDate());
        promotionDTO.setEndDate(promotion.getEndDate());
        promotionDTO.setActive(promotion.isActive());
        promotionDTO.setCode(promotion.getCode());
        promotionDTO.setMinOrderAmount(promotion.getMinOrderAmount());
        promotionDTO.setMaxUsage(promotion.getMaxUsage());
        promotionDTO.setProductIds(promotion.getProducts().stream()
                .map(Product::getId)
                .toList());

        model.addAttribute("promotionDTO", promotionDTO);
        model.addAttribute("promotion", promotion);
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());

        return "admin/promotions/form";
    }

    @PostMapping("/{id}")
    public String updatePromotion(
            @PathVariable Long id,
            @Valid @ModelAttribute PromotionDTO promotionDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (promotionDTO.getEndDate() != null &&
                promotionDTO.getStartDate().isAfter(promotionDTO.getEndDate())) {
            result.rejectValue("endDate", "error.promotionDTO",
                    "Data zakończenia musi być późniejsza niż data rozpoczęcia");
        }

        if (result.hasErrors()) {
            Promotion promotion = promotionService.getPromotionById(id);
            model.addAttribute("promotion", promotion);
            model.addAttribute("promotionTypes", PromotionType.values());
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("brands", brandService.getAllBrands());
            return "admin/promotions/form";
        }

        try {
            Promotion promotion = promotionService.updatePromotion(id, promotionDTO);
            redirectAttributes.addFlashAttribute("success",
                    "Promocja '" + promotion.getName() + "' została zaktualizowana pomyślnie");
            return "redirect:/admin/promotions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas aktualizacji promocji: " + e.getMessage());
            return "redirect:/admin/promotions/" + id + "/edit";
        }
    }

    @GetMapping("/{id}")
    public String viewPromotion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Promotion promotion = promotionService.getPromotionById(id);

        if (promotion == null) {
            redirectAttributes.addFlashAttribute("error", "Promocja nie została znaleziona");
            return "redirect:/admin/promotions";
        }

        model.addAttribute("promotion", promotion);
        model.addAttribute("statistics", promotionService.getPromotionStatistics(id));

        return "admin/promotions/view";
    }

    @PostMapping("/{id}/delete")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Promotion promotion = promotionService.getPromotionById(id);
            String promotionName = promotion.getName();
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("success",
                    "Promocja '" + promotionName + "' została usunięta pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można usunąć promocji: " + e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/toggle-active")
    @ResponseBody
    public String toggleActive(@PathVariable Long id) {
        try {
            promotionService.toggleActive(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/{id}/duplicate")
    public String duplicatePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Promotion newPromotion = promotionService.duplicatePromotion(id);
            redirectAttributes.addFlashAttribute("success",
                    "Promocja została zduplikowana jako '" + newPromotion.getName() + "'");
            return "redirect:/admin/promotions/" + newPromotion.getId() + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas duplikowania promocji: " + e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    @GetMapping("/validate-code")
    @ResponseBody
    public boolean validateCode(
            @RequestParam String code,
            @RequestParam(required = false) Long excludeId) {
        return promotionService.isCodeUnique(code, excludeId);
    }

    @GetMapping("/category-products/{categoryId}")
    @ResponseBody
    public List<Map<String, Object>> getCategoryProducts(@PathVariable Long categoryId) {
        List<Product> products = productService.getProductsByCategoryId(categoryId);
        return products.stream()
                .map(product -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", product.getId());
                    map.put("name", product.getName());
                    map.put("price", product.getPrice());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/subcategory-products/{subcategoryId}")
    @ResponseBody
    public List<Map<String, Object>> getSubcategoryProducts(@PathVariable Long subcategoryId) {
        List<Product> products = productService.getProductsBySubcategoryId(subcategoryId);
        return products.stream()
                .map(product -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", product.getId());
                    map.put("name", product.getName());
                    map.put("price", product.getPrice());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/brand-products/{brandId}")
    @ResponseBody
    public List<Map<String, Object>> getBrandProducts(@PathVariable Long brandId) {
        List<Product> products = productService.getProductsByBrand(brandId);
        return products.stream()
                .map(product -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", product.getId());
                    map.put("name", product.getName());
                    map.put("price", product.getPrice());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/apply-to-products")
    public String applyToProducts(
            @PathVariable Long id,
            @RequestParam List<Long> productIds,
            RedirectAttributes redirectAttributes) {
        try {
            promotionService.applyPromotionToProducts(id, productIds);
            redirectAttributes.addFlashAttribute("success",
                    "Promocja została zastosowana do " + productIds.size() + " produktów");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas stosowania promocji: " + e.getMessage());
        }
        return "redirect:/admin/promotions/" + id;
    }

    @PostMapping("/{id}/remove-from-products")
    public String removeFromProducts(
            @PathVariable Long id,
            @RequestParam List<Long> productIds,
            RedirectAttributes redirectAttributes) {
        try {
            promotionService.removePromotionFromProducts(id, productIds);
            redirectAttributes.addFlashAttribute("success",
                    "Promocja została usunięta z " + productIds.size() + " produktów");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas usuwania promocji: " + e.getMessage());
        }
        return "redirect:/admin/promotions/" + id;
    }
}