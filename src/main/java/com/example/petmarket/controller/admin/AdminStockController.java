package com.example.petmarket.controller.admin;

import com.example.petmarket.entity.Product;
import com.example.petmarket.enums.ProductStatus;
import com.example.petmarket.service.stock.StockService;
import com.example.petmarket.service.interfaces.IProductCrudService;
import com.example.petmarket.service.interfaces.IProductQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin/stock")
@RequiredArgsConstructor
@Slf4j
public class AdminStockController {

    private final IProductCrudService productCrudService;
    private final IProductQueryService productQueryService;
    private final StockService stockService;

    @GetMapping
    public String listStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter,
            Model model) {

        Sort sort = Sort.by(Sort.Direction.ASC, "stockQuantity");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Product> products;

        if ("low".equals(filter)) {
            products = stockService.getLowStockProducts(pageRequest);
        } else if ("out".equals(filter)) {
            products = productQueryService.filterProducts(null, null, null, ProductStatus.SOLDOUT, pageRequest);
        } else {
            products = productQueryService.getAllProducts(pageRequest);
        }

        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalItems", products.getTotalElements());
        model.addAttribute("filter", filter);

        model.addAttribute("totalProducts", productQueryService.getTotalProductsCount());
        model.addAttribute("lowStockCount", stockService.getLowStockProductsCount());
        model.addAttribute("outOfStockCount", stockService.getOutOfStockProductsCount());

        return "admin/stock";
    }

    @PostMapping("/{id}/increase")
    @ResponseBody
    public Map<String, Object> increaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        try {
            if (quantity == null || quantity <= 0) {
                return Map.of("success", false, "message", "Ilość musi być większa od 0");
            }
            stockService.increaseStock(id, quantity);
            Product product = productCrudService.getProductById(id);
            return Map.of(
                    "success", true,
                    "message", "Zamówiono " + quantity + " szt.",
                    "newStock", product.getStockQuantity(),
                    "status", product.getStatus().name()
            );
        } catch (Exception e) {
            log.error("Błąd podczas zwiększania ilości produktu {}: {}", id, e.getMessage(), e);
            return Map.of("success", false, "message", "Błąd: " + e.getMessage());
        }
    }
}