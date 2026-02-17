package com.example.petmarket.controller.admin;

import com.example.petmarket.dto.BrandDTO;
import com.example.petmarket.entity.Brand;
import com.example.petmarket.service.product.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/brands")
@RequiredArgsConstructor
@Slf4j
public class AdminBrandController {

    private final BrandService brandService;

    @GetMapping
    public String listBrands(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model) {

        Page<Brand> brandPage = keyword != null && !keyword.isEmpty()
                ? brandService.searchBrands(keyword, PageRequest.of(page, size, Sort.by("name").ascending()))
                : brandService.getAllBrands(PageRequest.of(page, size, Sort.by("id").descending()));

        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("keyword", keyword);
        }

        model.addAttribute("brands", brandPage);
        model.addAttribute("totalBrands", brandService.getTotalBrandsCount());
        model.addAttribute("activeBrands", brandService.getActiveBrandsCount());

        return "admin/brands/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        BrandDTO brandDTO = new BrandDTO();
        brandDTO.setActive(true);
        model.addAttribute("brandDTO", brandDTO);
        return "admin/brands/form";
    }

    @PostMapping("/new")
    public String createBrand(
            @Valid @ModelAttribute BrandDTO brandDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile logoFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("brandDTO", brandDTO);
            return "admin/brands/form";
        }

        try {
            Brand brand = brandService.createBrand(brandDTO, logoFile);
            redirectAttributes.addFlashAttribute("success",
                    "Marka '" + brand.getName() + "' została utworzona pomyślnie");
        } catch (IOException e) {
            log.error("Błąd podczas przesyłania logo: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania logo: " + e.getMessage());
            return "redirect:/admin/brands/new";
        } catch (Exception e) {
            log.error("Błąd podczas tworzenia marki: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia marki: " + e.getMessage());
            return "redirect:/admin/brands/new";
        }

        return "redirect:/admin/brands";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Brand brand = brandService.getBrandById(id);
            BrandDTO brandDTO = brandService.convertToDTO(brand);

            model.addAttribute("brandDTO", brandDTO);
            model.addAttribute("brand", brand);

            return "admin/brands/edit";
        } catch (Exception e) {
            log.error("Błąd podczas pobierania marki: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Marka nie została znaleziona");
            return "redirect:/admin/brands";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateBrand(
            @PathVariable Long id,
            @Valid @ModelAttribute BrandDTO brandDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile logoFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            try {
                Brand brand = brandService.getBrandById(id);
                model.addAttribute("brandDTO", brandDTO);
                model.addAttribute("brand", brand);
            } catch (Exception e) {
                log.error("Błąd podczas pobierania marki: {}", e.getMessage());
            }
            return "admin/brands/edit";
        }

        try {
            Brand brand = brandService.updateBrand(id, brandDTO, logoFile);
            redirectAttributes.addFlashAttribute("success",
                    "Marka '" + brand.getName() + "' została zaktualizowana pomyślnie");
        } catch (IOException e) {
            log.error("Błąd podczas przesyłania logo: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania logo: " + e.getMessage());
            return "redirect:/admin/brands/edit/" + id;
        } catch (Exception e) {
            log.error("Błąd podczas aktualizacji marki: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas aktualizacji marki: " + e.getMessage());
            return "redirect:/admin/brands/edit/" + id;
        }

        return "redirect:/admin/brands";
    }

    @PostMapping("/delete/{id}")
    public String deleteBrand(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            brandService.deleteBrand(id);
            redirectAttributes.addFlashAttribute("success", "Marka została pomyślnie usunięta");
        } catch (Exception e) {
            log.error("Błąd podczas usuwania marki: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nie można usunąć marki: " + e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    @PostMapping("/toggle/{id}")
    public String toggleBrandStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            brandService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", "Status marki został zmieniony");
        } catch (Exception e) {
            log.error("Błąd podczas zmiany statusu: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Błąd podczas zmiany statusu");
        }
        return "redirect:/admin/brands";
    }
}