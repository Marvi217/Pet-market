package com.example.petmarket.controller.admin;

import com.example.petmarket.dto.SubcategoryDTO;
import com.example.petmarket.entity.Category;
import com.example.petmarket.entity.Subcategory;
import com.example.petmarket.service.product.CategoryService;
import com.example.petmarket.service.product.SubcategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin/subcategories")
@RequiredArgsConstructor
public class AdminSubcategoryController {

    private final SubcategoryService subcategoryService;
    private final CategoryService categoryService;

    @GetMapping
    public String listAllSubcategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            Model model) {

        if (categoryId != null) {
            return "redirect:/admin/categories/" + categoryId + "/subcategories";
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Subcategory> subcategories;

        if (search != null && !search.isEmpty()) {
            subcategories = subcategoryService.searchSubcategories(search, pageRequest);
            model.addAttribute("search", search);
        } else {
            subcategories = subcategoryService.getAllSubcategories(pageRequest);
        }

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("subcategories", subcategories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", subcategories.getTotalPages());

        return "admin/subcategories/list";
    }

    @GetMapping("/category/{categoryId}")
    public String listSubcategoriesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            RedirectAttributes redirectAttributes) {

        Category category = categoryService.getCategoryById(categoryId);

        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Kategoria nie została znaleziona");
            return "redirect:/admin/categories";
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
        Page<Subcategory> subcategories = subcategoryService.getSubcategoriesByCategory(categoryId, pageRequest);

        model.addAttribute("category", category);
        model.addAttribute("subcategories", subcategories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", subcategories.getTotalPages());

        return "admin/subcategories/form";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(required = false) Long categoryId,
            Model model,
            RedirectAttributes redirectAttributes) {

        SubcategoryDTO subcategoryDTO = new SubcategoryDTO();
        subcategoryDTO.setActive(true);

        if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            if (category != null) {
                subcategoryDTO.setCategoryId(categoryId);
                model.addAttribute("selectedCategory", category);
            } else {
                redirectAttributes.addFlashAttribute("error", "Kategoria nie została znaleziona");
                return "redirect:/admin/categories";
            }
        }

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("subcategoryDTO", subcategoryDTO);

        return "admin/subcategories/edit";
    }

    @PostMapping
    public String createSubcategory(
            @Valid @ModelAttribute SubcategoryDTO subcategoryDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile imageFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("subcategoryDTO", subcategoryDTO);
            model.addAttribute("categories", categoryService.getAllCategories());

            if (subcategoryDTO.getCategoryId() != null) {
                Category category = categoryService.getCategoryById(subcategoryDTO.getCategoryId());
                model.addAttribute("selectedCategory", category);
            }

            return "admin/subcategories/edit";
        }

        try {
            Subcategory subcategory = subcategoryService.createSubcategory(subcategoryDTO, imageFile);
            redirectAttributes.addFlashAttribute("success",
                    "Subkategoria '" + subcategory.getName() + "' została utworzona pomyślnie");

            return "redirect:/admin/subcategories/category/" + subcategoryDTO.getCategoryId();
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania obrazu: " + e.getMessage());
            return "redirect:/admin/subcategories/new?categoryId=" + subcategoryDTO.getCategoryId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia subkategorii: " + e.getMessage());
            return "redirect:/admin/subcategories/new?categoryId=" + subcategoryDTO.getCategoryId();
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        Subcategory subcategory = subcategoryService.getSubcategoryById(id);

        if (subcategory == null) {
            redirectAttributes.addFlashAttribute("error", "Subkategoria nie została znaleziona");
            return "redirect:/admin/categories";
        }

        SubcategoryDTO subcategoryDTO = getSubcategoryDTO(subcategory);

        model.addAttribute("subcategoryDTO", subcategoryDTO);
        model.addAttribute("subcategory", subcategory);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedCategory", subcategory.getCategory());

        return "admin/subcategories/edit";
    }

    private static SubcategoryDTO getSubcategoryDTO(Subcategory subcategory) {
        SubcategoryDTO subcategoryDTO = new SubcategoryDTO();
        subcategoryDTO.setId(subcategory.getId());
        subcategoryDTO.setName(subcategory.getName());
        subcategoryDTO.setSlug(subcategory.getSlug());
        subcategoryDTO.setIcon(subcategory.getIcon());
        subcategoryDTO.setDescription(subcategory.getDescription());
        subcategoryDTO.setActive(subcategory.isActive());
        subcategoryDTO.setImageUrl(subcategory.getImageUrl());
        subcategoryDTO.setCategoryId(subcategory.getCategory().getId());
        subcategoryDTO.setDisplayOrder(subcategory.getDisplayOrder());
        return subcategoryDTO;
    }

    @PostMapping("/{id}")
    public String updateSubcategory(
            @PathVariable Long id,
            @Valid @ModelAttribute SubcategoryDTO subcategoryDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile imageFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            Subcategory subcategory = subcategoryService.getSubcategoryById(id);
            model.addAttribute("subcategoryDTO", subcategoryDTO);
            model.addAttribute("subcategory", subcategory);
            model.addAttribute("categories", categoryService.getAllCategories());
            if (subcategory != null) {
                model.addAttribute("selectedCategory", subcategory.getCategory());
            }
            return "admin/subcategories/edit";
        }

        try {
            Subcategory subcategory = subcategoryService.updateSubcategory(id, subcategoryDTO, imageFile);
            redirectAttributes.addFlashAttribute("success",
                    "Subkategoria '" + subcategory.getName() + "' została zaktualizowana pomyślnie");

            return "redirect:/admin/subcategories/category/" + subcategoryDTO.getCategoryId();
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania obrazu: " + e.getMessage());
            return "redirect:/admin/subcategories/" + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas aktualizacji subkategorii: " + e.getMessage());
            return "redirect:/admin/subcategories/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteSubcategory(
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId,
            RedirectAttributes redirectAttributes) {

        try {
            Subcategory subcategory = subcategoryService.getSubcategoryById(id);
            Long parentCategoryId = subcategory != null ? subcategory.getCategory().getId() : categoryId;

            subcategoryService.deleteSubcategory(id);
            redirectAttributes.addFlashAttribute("success", "Subkategoria została usunięta pomyślnie");

            if (parentCategoryId != null) {
                return "redirect:/admin/subcategories/category/" + parentCategoryId;
            }
            return "redirect:/admin/categories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można usunąć subkategorii: " + e.getMessage());

            if (categoryId != null) {
                return "redirect:/admin/subcategories/category/" + categoryId;
            }
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/{id}/toggle-active")
    @ResponseBody
    public String toggleActive(@PathVariable Long id) {
        try {
            subcategoryService.toggleActive(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}