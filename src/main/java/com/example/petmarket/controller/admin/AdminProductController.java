package com.example.petmarket.controller.admin;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.dto.ProductDTO;
import com.example.petmarket.dto.SubcategoryDTO;
import com.example.petmarket.entity.*;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.BrandRepository;
import com.example.petmarket.repository.CategoryRepository;
import com.example.petmarket.repository.SubcategoryRepository;
import com.example.petmarket.service.*;
import jakarta.servlet.http.HttpSession;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SubcategoryService subcategoryService;
    private final BrandService brandService;
    private final ProductImageService productImageService;
    private final SecurityHelper securityHelper;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final BrandRepository brandRepository;

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            Model model) {

        Sort sort = Sort.by(sortDir != null && sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "name");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Product> products;

        if (search != null && !search.isEmpty()) {
            products = productService.searchProducts(search, pageRequest);
        } else if (categoryId != null || subcategoryId != null || brandId != null || status != null) {
            products = productService.filterProducts(categoryId, subcategoryId, brandId, status, pageRequest);
        } else {
            products = productService.getAllProducts(pageRequest);
        }

        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalItems", products.getTotalElements());

        model.addAttribute("categories", categoryService.getAllActiveCategories());
        model.addAttribute("brands", brandService.getAllActiveBrands());
        model.addAttribute("statuses", ProductStatus.values());

        model.addAttribute("search", search);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSubcategoryId", subcategoryId);
        model.addAttribute("selectedBrandId", brandId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "admin/products";
    }

    @GetMapping("/subcategories-by-category/{categoryId}")
    @ResponseBody
    public List<SubcategoryDTO> getSubcategoriesByCategory(@PathVariable Long categoryId) {
        List<Subcategory> subcategories = subcategoryService.getActiveSubcategoriesByCategory(categoryId);

        return subcategories.stream()
                .map(sub -> {
                    SubcategoryDTO dto = new SubcategoryDTO();
                    dto.setId(sub.getId());
                    dto.setName(sub.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) {
        User currentUser = securityHelper.getCurrentUser(session);
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/";
        }

        ProductDTO productDTO = new ProductDTO();
        productDTO.setStatus(ProductStatus.ACTIVE);

        model.addAttribute("productDTO", productDTO);
        model.addAttribute("categories", categoryService.getAllActiveCategories());
        model.addAttribute("brands", brandService.getAllActiveBrands());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("subcategories", subcategoryService.getAllActiveSubcategories());

        return "admin/products/form";
    }

    @PostMapping
    public String createProduct(
            @Valid @ModelAttribute ProductDTO productDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile[] images,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllActiveCategories());
            model.addAttribute("brands", brandService.getAllActiveBrands());
            model.addAttribute("statuses", ProductStatus.values());
            return "admin/products/form";
        }

        try {
            Product product = new Product();
            product.setName(productDTO.getName());
            product.setDescription(productDTO.getDescription());
            product.setPrice(productDTO.getPrice());
            product.setDiscountedPrice(productDTO.getDiscountedPrice());
            product.setStockQuantity(productDTO.getStockQuantity());
            product.setStatus(productDTO.getStatus() != null ? productDTO.getStatus() : ProductStatus.ACTIVE);
            product.setSku(productDTO.getSku());
            product.setWeight(productDTO.getWeight());
            product.setDimensions(productDTO.getDimensions());

            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Kategoria nie znaleziona"));
            Subcategory subcategory = subcategoryRepository.findById(productDTO.getSubcategoryId())
                    .orElseThrow(() -> new RuntimeException("Subkategoria nie znaleziona"));
            Brand brand = brandRepository.findById(productDTO.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Marka nie znaleziona"));

            product.setCategory(category);
            product.setSubcategory(subcategory);
            product.setBrand(brand);

            Product saved = productService.save(product);

            if (images != null) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        productImageService.addImage(saved.getId(), image);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success",
                    "Produkt '" + saved.getName() + "' został utworzony pomyślnie");
            return "redirect:/admin/products";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia produktu: " + e.getMessage());
            return "redirect:/admin/products/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.getProductById(id);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Produkt nie został znaleziony");
            return "redirect:/admin/products";
        }

        ProductDTO productDTO = getProductDTO(product);

        model.addAttribute("productDTO", productDTO);
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllActiveCategories());
        model.addAttribute("brands", brandService.getAllActiveBrands());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("subcategories", subcategoryService.getActiveSubcategoriesByCategory(product.getSubcategory().getCategory().getId()));

        return "admin/products/form";
    }

    private static ProductDTO getProductDTO(Product product) {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(product.getId());
        productDTO.setName(product.getName());
        productDTO.setDescription(product.getDescription());
        productDTO.setPrice(product.getPrice());
        productDTO.setDiscountedPrice(product.getDiscountedPrice());
        productDTO.setStockQuantity(product.getStockQuantity());
        productDTO.setStatus(product.getStatus());
        productDTO.setCategoryId(product.getSubcategory().getCategory().getId());
        productDTO.setSubcategoryId(product.getSubcategory().getId());
        productDTO.setBrandId(product.getBrand().getId());
        productDTO.setSku(product.getSku());
        productDTO.setWeight(product.getWeight());
        productDTO.setDimensions(product.getDimensions());
        return productDTO;
    }

    @PostMapping("/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute ProductDTO productDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile[] images,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            Product product = productService.getProductById(id);
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.getAllActiveCategories());
            model.addAttribute("brands", brandService.getAllActiveBrands());
            model.addAttribute("statuses", ProductStatus.values());
            return "admin/products/form";
        }

        try {
            Product product = productService.updateProduct(id, productDTO);

            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        productImageService.addImage(product.getId(), image);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success",
                    "Produkt '" + product.getName() + "' został zaktualizowany pomyślnie");
            return "redirect:/admin/products";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas aktualizacji produktu: " + e.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        }
    }

    @GetMapping("/{id}/images")
    public String manageImages(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.getProductById(id);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Produkt nie został znaleziony");
            return "redirect:/admin/products";
        }

        model.addAttribute("product", product);
        model.addAttribute("images", productImageService.getProductImages(id));

        return "admin/products/images";
    }

    @PostMapping("/{productId}/images/{imageId}/delete")
    public String deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {
        try {
            productImageService.deleteImage(imageId);
            redirectAttributes.addFlashAttribute("success", "Obraz został usunięty pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas usuwania obrazu: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/images";
    }

    @PostMapping("/{productId}/images/{imageId}/set-main")
    public String setMainImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {
        try {
            productImageService.setMainImage(productId, imageId);
            redirectAttributes.addFlashAttribute("success", "Główny obraz został ustawiony pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas ustawiania głównego obrazu: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/images";
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.getProductById(id);
            String productName = product.getName();
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success",
                    "Produkt '" + productName + "' został usunięty pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można usunąć produktu: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/change-status")
    @ResponseBody
    public String changeStatus(@PathVariable Long id, @RequestParam ProductStatus status) {
        try {
            productService.changeStatus(id, status);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/{id}/quick-update")
    @ResponseBody
    public String quickUpdate(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) BigDecimal discountedPrice,
            @RequestParam(required = false) Integer stockQuantity) {
        try {
            productService.quickUpdate(id, price, discountedPrice, stockQuantity);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/export")
    public String exportProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            String filename = productService.exportToCSV(search, categoryId, subcategoryId, brandId, status);
            redirectAttributes.addFlashAttribute("success",
                    "Produkty zostały wyeksportowane do pliku: " + filename);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas eksportu: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
}