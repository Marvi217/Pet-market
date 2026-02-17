package com.example.petmarket.controller.admin;

import com.example.petmarket.dto.CategoryDTO;
import com.example.petmarket.entity.Category;
import com.example.petmarket.service.product.CategoryService;
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
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Category> categories = search != null && !search.isEmpty()
                ? categoryService.searchCategories(search, pageRequest)
                : categoryService.getAllCategories(pageRequest);

        if (search != null && !search.isEmpty()) {
            model.addAttribute("search", search);
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
            log.error("Błąd podczas przesyłania obrazu: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania obrazu: " + e.getMessage());
            return "redirect:/admin/categories/new";
        } catch (Exception e) {
            log.error("Błąd podczas tworzenia kategorii: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia kategorii: " + e.getMessage());
            return "redirect:/admin/categories/new";
        }

        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryService.getCategoryById(id);
            CategoryDTO categoryDTO = categoryService.convertToDTO(category);

            model.addAttribute("categoryDTO", categoryDTO);
            model.addAttribute("category", category);

            return "admin/categories/form";
        } catch (Exception e) {
            log.error("Błąd podczas pobierania kategorii: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Kategoria nie została znaleziona");
            return "redirect:/admin/categories";
        }
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
            try {
                Category category = categoryService.getCategoryById(id);
                model.addAttribute("categoryDTO", categoryDTO);
                model.addAttribute("category", category);
            } catch (Exception e) {
                log.error("Błąd podczas pobierania kategorii: {}", e.getMessage());
            }
            return "admin/categories/form";
        }

        try {
            Category category = categoryService.updateCategory(id, categoryDTO, imageFile);
            redirectAttributes.addFlashAttribute("success",
                    "Kategoria '" + category.getName() + "' została zaktualizowana pomyślnie");
        } catch (IOException e) {
            log.error("Błąd podczas przesyłania obrazu: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania obrazu: " + e.getMessage());
            return "redirect:/admin/categories/" + id + "/edit";
        } catch (Exception e) {
            log.error("Błąd podczas aktualizacji kategorii: {}", e.getMessage());
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
            log.error("Błąd podczas usuwania kategorii: {}", e.getMessage());
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
            log.error("Błąd podczas zmiany statusu: {}", e.getMessage());
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/{id}/subcategories")
    public String redirectToSubcategories(@PathVariable Long id) {
        return "redirect:/admin/subcategories/category/" + id;
    }
}