package com.example.petmarket.controller.admin;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.dto.ProductDTO;
import com.example.petmarket.dto.SubcategoryDTO;
import com.example.petmarket.entity.*;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.repository.BrandRepository;
import com.example.petmarket.repository.CategoryRepository;
import com.example.petmarket.repository.SubcategoryRepository;
import com.example.petmarket.service.stock.StockService;
import com.example.petmarket.service.export.ProductExportService;
import com.example.petmarket.service.export.ProductExporter;
import com.example.petmarket.service.interfaces.*;
import com.example.petmarket.service.product.BrandService;
import com.example.petmarket.service.product.CategoryService;
import com.example.petmarket.service.product.ProductImageService;
import com.example.petmarket.service.product.SubcategoryService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final IProductCrudService productCrudService;
    private final IProductQueryService productQueryService;
    private final StockService stockService;
    private final CategoryService categoryService;
    private final SubcategoryService subcategoryService;
    private final BrandService brandService;
    private final ProductImageService productImageService;
    private final SecurityHelper securityHelper;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductExportService productExportService;

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
            products = productQueryService.searchProducts(search, pageRequest);
        } else if (categoryId != null || subcategoryId != null || brandId != null || status != null) {
            products = productQueryService.filterProducts(categoryId, subcategoryId, brandId, status, pageRequest);
        } else {
            products = productQueryService.getAllProducts(pageRequest);
        }

        System.out.println("Products found: " + products.getTotalElements());

        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalItems", products.getTotalElements());

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());
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

        List<Subcategory> subcategories = subcategoryService.getSubcategoriesByCategory(categoryId);

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
        System.out.println("=== NEW PRODUCT FORM CALLED ===");

        User currentUser = securityHelper.getCurrentUser(session);
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/";
        }

        ProductDTO productDTO = new ProductDTO();
        productDTO.setStatus(ProductStatus.ACTIVE);

        model.addAttribute("productDTO", productDTO);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("subcategories", subcategoryService.getAllSubcategories());

        return "admin/products/form";
    }

    @PostMapping("/new")
    public String createProduct(
            @Valid @ModelAttribute ProductDTO productDTO,
            BindingResult result,
            @RequestParam(required = false) MultipartFile[] images,
            RedirectAttributes redirectAttributes,
            Model model) {
        System.out.println("=== NEW PRODUCT ===");
        try {
            if (result.hasErrors()) {
                result.getAllErrors().forEach(error ->
                        log.warn("Validation error: {}", error.getDefaultMessage())
                );

                model.addAttribute("categories", categoryService.getAllCategories());
                model.addAttribute("brands", brandService.getAllBrands());
                model.addAttribute("statuses", ProductStatus.values());
                model.addAttribute("subcategories",
                        productDTO.getCategoryId() != null
                                ? subcategoryService.getSubcategoriesByCategory(productDTO.getCategoryId())
                                : subcategoryService.getAllSubcategories());
                return "admin/products/form";
            }

            Product product = new Product();
            product.setName(productDTO.getName());
            product.setDescription(productDTO.getDescription());
            product.setPrice(productDTO.getPrice());
            product.setStockQuantity(productDTO.getStockQuantity());
            product.setStatus(productDTO.getStatus() != null ? productDTO.getStatus() : ProductStatus.ACTIVE);
            product.setWeight(productDTO.getWeight());
            product.setDimensions(productDTO.getDimensions());
            product.setIngredients(productDTO.getIngredients());

            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Kategoria nie znaleziona"));
            Subcategory subcategory = subcategoryRepository.findById(productDTO.getSubcategoryId())
                    .orElseThrow(() -> new RuntimeException("Subkategoria nie znaleziona"));
            Brand brand = brandRepository.findById(productDTO.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Marka nie znaleziona"));

            product.setCategory(category);
            product.setSubcategory(subcategory);
            product.setBrand(brand);

            Product saved = productCrudService.save(product);

            int uploadedCount = 0;
            int failedCount = 0;
            if (images != null) {
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        try {
                            productImageService.addImage(saved.getId(), image);
                            uploadedCount++;
                        } catch (Exception imageError) {
                            log.warn("Error uploading image for product {}: {}", saved.getId(), imageError.getMessage());
                            failedCount++;
                        }
                    }
                }
            }

            StringBuilder message = new StringBuilder("Produkt '" + saved.getName() + "' został utworzony pomyślnie");
            if (uploadedCount > 0) {
                message.append(". Dodano ").append(uploadedCount).append(" zdjęć");
            }
            if (failedCount > 0) {
                message.append(". Nie udało się dodać ").append(failedCount).append(" zdjęć");
            }

            redirectAttributes.addFlashAttribute("success", message.toString());
            return "redirect:/admin/products";

        } catch (Exception e) {
            log.error("Error creating product: {}", e.getMessage(), e);

            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia produktu: " + e.getMessage());
            return "redirect:/admin/products/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product product = productCrudService.getProductById(id);

            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Produkt nie został znaleziony");
                return "redirect:/admin/products";
            }

            ProductDTO productDTO = getProductDTO(product);

            model.addAttribute("productDTO", productDTO);
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("brands", brandService.getAllBrands());
            model.addAttribute("statuses", ProductStatus.values());
            model.addAttribute("subcategories", subcategoryService.getSubcategoriesByCategory(product.getSubcategory().getCategory().getId()));

            return "admin/products/edit";
        } catch (Exception e) {
            log.warn("Product not found: {}", id);
            redirectAttributes.addFlashAttribute("error", "Produkt o ID " + id + " nie został znaleziony");
            return "redirect:/admin/products";
        }
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
        productDTO.setIngredients(product.getIngredients());
        return productDTO;
    }

    @GetMapping("/{id}")
    public String redirectToEdit(@PathVariable Long id) {
        return "redirect:/admin/products/" + id + "/edit";
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
            Product product = productCrudService.getProductById(id);
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("brands", brandService.getAllBrands());
            model.addAttribute("statuses", ProductStatus.values());
            model.addAttribute("subcategories",
                    productDTO.getCategoryId() != null
                            ? subcategoryService.getSubcategoriesByCategory(productDTO.getCategoryId())
                            : subcategoryService.getAllSubcategories());
            return "admin/products/edit";
        }

        try {
            Product product = productCrudService.updateProduct(id, productDTO);

            int uploadedCount = 0;
            int failedCount = 0;
            if (images != null) {
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        try {
                            productImageService.addImage(product.getId(), image);
                            uploadedCount++;
                        } catch (Exception imageError) {
                            log.warn("Error uploading image for product {}: {}", product.getId(), imageError.getMessage());
                            failedCount++;
                        }
                    }
                }
            }

            StringBuilder message = new StringBuilder("Produkt '" + product.getName() + "' został zaktualizowany pomyślnie");
            if (uploadedCount > 0) {
                message.append(". Dodano ").append(uploadedCount).append(" zdjęć");
            }
            if (failedCount > 0) {
                message.append(". Nie udało się dodać ").append(failedCount).append(" zdjęć");
            }

            redirectAttributes.addFlashAttribute("success", message.toString());
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("Error updating product {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas aktualizacji produktu: " + e.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        }
    }

    @GetMapping("/{id}/images")
    public String manageImages(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productCrudService.getProductById(id);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Produkt nie został znaleziony");
            return "redirect:/admin/products";
        }

        model.addAttribute("product", product);
        model.addAttribute("images", productImageService.getProductImages(id));

        return "admin/products/images";
    }

    @PostMapping("/{id}/images/upload")
    public String uploadImages(
            @PathVariable Long id,
            @RequestParam("images") MultipartFile[] images,
            RedirectAttributes redirectAttributes) {
        try {
            int uploadedCount = 0;
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    productImageService.addImage(id, image);
                    uploadedCount++;
                }
            }
            if (uploadedCount > 0) {
                redirectAttributes.addFlashAttribute("success",
                        "Pomyślnie przesłano " + uploadedCount + " zdjęć");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas przesyłania zdjęć: " + e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/images";
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

    @PostMapping("/{productId}/images/{imageId}/move-up")
    public String moveImageUp(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {
        try {
            productImageService.moveUp(imageId);
            redirectAttributes.addFlashAttribute("success", "Kolejność zdjęcia została zmieniona");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas zmiany kolejności: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/images";
    }

    @PostMapping("/{productId}/images/{imageId}/move-down")
    public String moveImageDown(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {
        try {
            productImageService.moveDown(imageId);
            redirectAttributes.addFlashAttribute("success", "Kolejność zdjęcia została zmieniona");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas zmiany kolejności: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/images";
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Product product = productCrudService.getProductById(id);
            String productName = product.getName();
            productCrudService.deleteProduct(id);
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
            productCrudService.changeStatus(id, status);
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
            productCrudService.quickUpdate(id, price, discountedPrice, stockQuantity);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/{id}/increase-stock")
    @ResponseBody
    public String increaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        try {
            if (quantity == null || quantity <= 0) {
                return "error: Ilość musi być większa od 0";
            }
            stockService.increaseStock(id, quantity);
            return "success";
        } catch (Exception e) {
            log.error("Błąd podczas dodawnia produktut {}: {}", id, e.getMessage(), e);
            return "error: Nie udało się zaktualizować stanu magazynowego";
        }
    }

    @GetMapping("/export")
    public void exportProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "CSV") String format,
            HttpServletResponse response) {
        try {
            ProductExporter exporter = productExportService.getExporter(format);
            response.setContentType(exporter.getContentType());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"products_export_" + System.currentTimeMillis() + exporter.getFileExtension() + "\"");

            productExportService.exportProducts(format, search, categoryId, subcategoryId, brandId, status, response.getWriter());
        } catch (Exception e) {
            log.error("Error exporting products to {}: {}", format, e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Błąd podczas eksportu produktów");
            } catch (java.io.IOException ignored) {
            }
        }
    }
}