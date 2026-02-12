package com.example.petmarket.controller.admin;

import com.example.petmarket.dto.UserDTO;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;
import com.example.petmarket.service.UserService;
import com.example.petmarket.service.OrderService;
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

    private final UserService userService;
    private final OrderService orderService;

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
            users = userService.searchUsers(search, pageRequest);
        } else if (role != null || active != null) {
            users = userService.filterUsers(role, active, pageRequest);
        } else {
            users = userService.getAllUsers(pageRequest);
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
        User user = userService.getUserById(id);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Użytkownik nie został znaleziony");
            return "redirect:/admin/users";
        }

        model.addAttribute("user", user);
        model.addAttribute("orders", orderService.getUserOrders(id, PageRequest.of(0, 10)));
        model.addAttribute("orderStats", orderService.getUserOrderStatistics(id));

        return "admin/users/view";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        UserDTO userDTO = new UserDTO();
        userDTO.setActive(true);
        userDTO.setRole(UserRole.USER);

        model.addAttribute("userDTO", userDTO);
        model.addAttribute("roles", UserRole.values());

        return "admin/users/form";
    }

    @PostMapping
    public String createUser(
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (userService.existsByEmail(userDTO.getEmail())) {
            result.rejectValue("email", "error.userDTO",
                    "Użytkownik z tym adresem email już istnieje");
        }

        if (result.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            return "admin/users/form";
        }

        try {
            User user = userService.createUser(userDTO);
            redirectAttributes.addFlashAttribute("success",
                    "Użytkownik '" + user.getEmail() + "' został utworzony pomyślnie");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas tworzenia użytkownika: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(id);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Użytkownik nie został znaleziony");
            return "redirect:/admin/users";
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setPhone(user.getPhone());
        userDTO.setRole(user.getRole());
        userDTO.setActive(user.isActive());

        model.addAttribute("userDTO", userDTO);
        model.addAttribute("user", user);
        model.addAttribute("roles", UserRole.values());

        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        User existingUser = userService.getUserById(id);
        if (!existingUser.getEmail().equals(userDTO.getEmail()) &&
                userService.existsByEmail(userDTO.getEmail())) {
            result.rejectValue("email", "error.userDTO",
                    "Użytkownik z tym adresem email już istnieje");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", existingUser);
            model.addAttribute("roles", UserRole.values());
            return "admin/users/form";
        }

        try {
            User user = userService.updateUser(id, userDTO);
            redirectAttributes.addFlashAttribute("success",
                    "Użytkownik '" + user.getEmail() + "' został zaktualizowany pomyślnie");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas aktualizacji użytkownika: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
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
            userService.changePassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "Hasło zostało zmienione pomyślnie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas zmiany hasła: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/toggle-active")
    @ResponseBody
    public String toggleActive(@PathVariable Long id) {
        try {
            userService.toggleActive(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/{id}/change-role")
    public String changeRole(
            @PathVariable Long id,
            @RequestParam UserRole role,
            RedirectAttributes redirectAttributes) {
        try {
            userService.changeRole(id, role);
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
            User user = userService.getUserById(id);
            String userEmail = user.getEmail();
            userService.deleteUser(id);
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
        byte[] content = userService.generateUsersCsv();
        String filename = "uzytkownicy_" + System.currentTimeMillis() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(content.length)
                .body(content);
    }
}