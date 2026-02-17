package com.example.petmarket.controller.admin;

import com.example.petmarket.dto.CategoryDTO;
import com.example.petmarket.entity.Category;
import com.example.petmarket.service.CategoryService;
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
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Category> categories;

        if (search != null && !search.isEmpty()) {
            categories = categoryService.searchCategories(search, pageRequest);
            model.addAttribute("search", search);
        } else {
            categories = categoryService.getAllCategories(pageRequest);
        }

        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categories.getTotalPages());

        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setActive(true);
        model.addAttribute("categoryDTO", categoryDTO);
        return "admin/categories/form";
    }

    @PostMapping
    public String createCategory(
            @Valid @ModelAttribute CategoryDTO categoryDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile imageFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("categoryDTO", categoryDTO);
            return "admin/categories/form";
        }

        try {
            Category category = categoryService.createCategory(categoryDTO, imageFile);
            redirectAttributes.addFlashAttribute("success",
                    "Kategoria '" + category.getName() + "' została utworzona pomyślnie");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania obrazu: " + e.getMessage());
            return "redirect:/admin/categories/new";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia kategorii: " + e.getMessage());
            return "redirect:/admin/categories/new";
        }

        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id);

        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Kategoria nie została znaleziona");
            return "redirect:/admin/categories";
        }

        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(category.getId());
        categoryDTO.setName(category.getName());
        categoryDTO.setSlug(category.getSlug());
        categoryDTO.setIcon(category.getIcon());
        categoryDTO.setDescription(category.getDescription());
        categoryDTO.setActive(category.isActive());
        categoryDTO.setImageUrl(category.getImageUrl());

        model.addAttribute("categoryDTO", categoryDTO);
        model.addAttribute("category", category);

        return "admin/categories/form";
    }

    @PostMapping("/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute CategoryDTO categoryDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile imageFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("categoryDTO", categoryDTO);
            model.addAttribute("category", categoryService.getCategoryById(id));
            return "admin/categories/form";
        }

        try {
            Category category = categoryService.updateCategory(id, categoryDTO, imageFile);
            redirectAttributes.addFlashAttribute("success",
                    "Kategoria '" + category.getName() + "' została zaktualizowana pomyślnie");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania obrazu: " + e.getMessage());
            return "redirect:/admin/categories/" + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas aktualizacji kategorii: " + e.getMessage());
            return "redirect:/admin/categories/" + id + "/edit";
        }

        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Kategoria została usunięta pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można usunąć kategorii: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle-active")
    @ResponseBody
    public String toggleActive(@PathVariable Long id) {
        try {
            categoryService.toggleActive(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/{id}/subcategories")
    public String redirectToSubcategories(@PathVariable Long id) {
        return "redirect:/admin/subcategories/category/" + id;
    }
}