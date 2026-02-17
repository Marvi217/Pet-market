package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.User;
import com.example.petmarket.entity.UserAddress;
import com.example.petmarket.service.UserAddressService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/account/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final UserAddressService addressService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String showAddresses(HttpSession session, Model model) {
        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login?returnUrl=/account/addresses";
        }

        List<UserAddress> addresses = addressService.getUserAddresses(user);

        model.addAttribute("addresses", addresses);
        model.addAttribute("user", user);

        return "account/addresses";
    }

    @PostMapping("/save")
    public String saveAddress(
            @ModelAttribute UserAddress address,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login?returnUrl=/account/addresses";
        }

        try {
            addressService.saveAddress(address, user);

            if (address.getId() == null) {
                redirectAttributes.addFlashAttribute("success", "Adres został dodany pomyślnie");
            } else {
                redirectAttributes.addFlashAttribute("success", "Adres został zaktualizowany");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }

        return "redirect:/account/addresses";
    }

    @PostMapping("/{id}/set-default")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setAsDefault(
            @PathVariable Long id,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Musisz być zalogowany");
            return ResponseEntity.ok(response);
        }

        try {
            addressService.setAsDefault(id, user);
            response.put("success", true);
            response.put("message", "Adres został ustawiony jako domyślny");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/delete")
    public String deleteAddress(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login?returnUrl=/account/addresses";
        }

        try {
            addressService.deleteAddress(id, user);
            redirectAttributes.addFlashAttribute("success", "Adres został usunięty");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }

        return "redirect:/account/addresses";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<UserAddress> getAddress(
            @PathVariable Long id,
            HttpSession session) {

        User user = securityHelper.getCurrentUser(session);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return addressService.getAddressById(id, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}