package com.example.petmarket.controller.admin;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentStatus;
import com.example.petmarket.service.order.OrderReportService;
import com.example.petmarket.service.interfaces.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final IOrderCrudService orderCrudService;
    private final IOrderQueryService orderQueryService;
    private final IOrderStatusService orderStatusService;
    private final IOrderTrackingService orderTrackingService;
    private final IOrderUserService orderUserService;
    private final OrderReportService orderReportService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String search,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orders;

        if (search != null && !search.isEmpty()) {
            orders = orderQueryService.searchOrders(search, pageRequest);
            model.addAttribute("search", search);
        } else if (status != null) {
            orders = orderQueryService.findByStatus(status, pageRequest);
            model.addAttribute("statusFilter", status);
        } else if (paymentStatus != null) {
            orders = orderQueryService.findByPaymentStatus(paymentStatus, pageRequest);
            model.addAttribute("paymentStatusFilter", paymentStatus);
        } else {
            orders = orderQueryService.getAllOrders(pageRequest);
        }

        model.addAttribute("pendingOrdersCount", orderQueryService.getOrdersByStatus(OrderStatus.PENDING).size());
        model.addAttribute("processingOrdersCount", orderQueryService.getOrdersByStatus(OrderStatus.PROCESSING).size());
        model.addAttribute("shippedOrdersCount", orderQueryService.getOrdersByStatus(OrderStatus.SHIPPED).size());
        model.addAttribute("completedOrdersCount", orderQueryService.getOrdersByStatus(OrderStatus.DELIVERED).size());

        model.addAttribute("orders", orders);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pendingOrders", orderQueryService.getOrdersByStatus(OrderStatus.PENDING).size());

        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());

        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String viewOrder(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        Order order = orderCrudService.getOrderById(id);

        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Zamówienie nie zostało znalezione");
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());

        return "admin/orders/view";
    }

    @PostMapping("/{id}/status")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        try {
            orderStatusService.updateOrderStatus(id, status, currentUser);
            redirectAttributes.addFlashAttribute("success", "Status zamówienia został zaktualizowany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji statusu: " + e.getMessage());
        }

        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/{id}/edit")
    public String editOrderForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = securityHelper.getCurrentUser(session);
        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        Order order = orderCrudService.getOrderById(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Zamówienie nie istnieje");
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("currentUser", currentUser);

        return "admin/orders/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateOrder(@PathVariable Long id, @ModelAttribute Order orderDetails, RedirectAttributes redirectAttributes) {
        try {
            orderCrudService.updateOrderDetails(id, orderDetails);
            redirectAttributes.addFlashAttribute("success", "Zamówienie zostało zaktualizowane");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas zapisu: " + e.getMessage());
            return "redirect:/admin/orders/" + id + "/edit";
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/payment-status")
    public String updatePaymentStatus(
            @PathVariable Long id){
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        try {
            orderUserService.adminCancelOrder(id, reason);
            redirectAttributes.addFlashAttribute("success", "Zamówienie zostało anulowane");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas anulowania zamówienia: " + e.getMessage());
        }

        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/tracking")
    public String addTrackingNumber(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam(required = false) DeliveryMethod carrier,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        try {
            orderTrackingService.addTrackingNumber(id, trackingNumber, carrier);
            orderStatusService.updateOrderStatus(id, OrderStatus.SHIPPED, currentUser);
            redirectAttributes.addFlashAttribute("success", "Numer przesyłki został dodany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas dodawania numeru przesyłki: " + e.getMessage());
        }

        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/generate-tracking")
    public String generateTrackingNumber(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = securityHelper.getCurrentUser(session);

        if (currentUser == null || !currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Brak dostępu");
            return "redirect:/";
        }

        try {
            String trackingNumber = orderTrackingService.generateAndSetTrackingNumber(id);
            redirectAttributes.addFlashAttribute("success", "Wygenerowano numer przesyłki: " + trackingNumber + ". Status zmieniony na: Wysłane");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas generowania numeru przesyłki: " + e.getMessage());
        }

        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {

        try {
            byte[] csvContent = orderReportService.exportToCSV(status, dateFrom, dateTo);
            String fileName = "zamowienia_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvContent);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}