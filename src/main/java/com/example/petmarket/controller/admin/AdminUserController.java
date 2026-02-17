package com.example.petmarket.controller.admin;

import com.example.petmarket.dto.UserDTO;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;
import com.example.petmarket.service.order.OrderStatisticsService;
import com.example.petmarket.service.product.ReviewService;
import com.example.petmarket.service.export.UserExportService;
import com.example.petmarket.service.interfaces.*;
import com.example.petmarket.service.user.PasswordOperations;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final IUserCrudService userCrudService;
    private final IUserQueryService userQueryService;
    private final IUserAdminService userAdminService;
    private final PasswordOperations passwordService;
    private final IOrderQueryService orderQueryService;
    private final OrderStatisticsService orderStatisticsService;
    private final ReviewService reviewService;
    private final UserExportService userExportService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            Model model) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> users;

        if (search != null && !search.isEmpty()) {
            users = userQueryService.searchUsers(search, pageRequest);
        } else if (role != null || active != null) {
            users = userQueryService.filterUsers(role, active, pageRequest);
        } else {
            users = userQueryService.getAllUsers(pageRequest);
        }

        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedActive", active);

        return "admin/users/list";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userQueryService.getUserById(id);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Użytkownik nie został znaleziony");
            return "redirect:/admin/users";
        }

        model.addAttribute("user", user);
        model.addAttribute("orders", orderQueryService.getUserOrders(id, PageRequest.of(0, 100, Sort.by("orderDate").descending())));
        model.addAttribute("orderStats", orderStatisticsService.getUserOrderStatistics(id));
        model.addAttribute("reviews", reviewService.getReviewsByUser(id, PageRequest.of(0, 100, Sort.by("createdAt").descending())));

        return "admin/users/view";
    }

    @PostMapping
    public String createUser(
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (userQueryService.existsByEmail(userDTO.getEmail())) {
            result.rejectValue("email", "error.userDTO",
                    "Użytkownik z tym adresem email już istnieje");
        }

        if (result.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            return "admin/users/form";
        }

        try {
            User user = userCrudService.createUser(userDTO);
            redirectAttributes.addFlashAttribute("success",
                    "Użytkownik '" + user.getEmail() + "' został utworzony pomyślnie");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia użytkownika: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @PostMapping("/{id}/change-password")
    public String changePassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Hasła nie są identyczne");
            return "redirect:/admin/users/" + id;
        }

        try {
            passwordService.changePassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "Hasło zostało zmienione pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas zmiany hasła: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userQueryService.getUserById(id);
            boolean wasActive = user.isActive();
            userAdminService.toggleActive(id);
            String status = wasActive ? "zablokowany" : "odblokowany";
            redirectAttributes.addFlashAttribute("success",
                    "Użytkownik '" + user.getEmail() + "' został " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas zmiany statusu: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/change-password-redirect")
    public String changePasswordAndRedirectToList(
            @PathVariable Long id,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Hasła nie są identyczne");
            return "redirect:/admin/users";
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Hasło musi mieć co najmniej 6 znaków");
            return "redirect:/admin/users";
        }

        try {
            User user = userQueryService.getUserById(id);
            passwordService.changePassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success",
                    "Hasło dla użytkownika '" + user.getEmail() + "' zostało zmienione pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas zmiany hasła: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/send-reset-email")
    public String sendPasswordResetEmail(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userQueryService.getUserById(id);
            passwordService.sendPasswordResetEmailByUserId(id);
            redirectAttributes.addFlashAttribute("success",
                    "Email z linkiem do resetowania hasła został wysłany do użytkownika '" + user.getEmail() + "'");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas wysyłania emaila: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/change-role")
    public String changeRole(
            @PathVariable Long id,
            @RequestParam UserRole role,
            RedirectAttributes redirectAttributes) {
        try {
            userAdminService.changeRole(id, role);
            redirectAttributes.addFlashAttribute("success",
                    "Rola użytkownika została zmieniona na: " + role.getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas zmiany roli: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userQueryService.getUserById(id);
            String userEmail = user.getEmail();
            userCrudService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success",
                    "Użytkownik '" + userEmail + "' został usunięty pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można usunąć użytkownika: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers() {
        byte[] content = userExportService.generateUsersCsv();
        String filename = "uzytkownicy_" + System.currentTimeMillis() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(content.length)
                .body(content);
    }
}