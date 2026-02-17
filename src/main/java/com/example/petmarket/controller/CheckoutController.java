package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.dto.CartViewModel;
import com.example.petmarket.dto.CheckoutForm;
import com.example.petmarket.entity.*;
import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.enums.PaymentMethod;
import com.example.petmarket.service.checkout.CheckoutCartService;
import com.example.petmarket.service.checkout.CheckoutPaymentService;
import com.example.petmarket.service.checkout.CheckoutService;
import com.example.petmarket.service.user.UserAddressService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("199");

    private final CheckoutService checkoutService;
    private final CheckoutPaymentService checkoutPaymentService;
    private final UserAddressService addressService;
    private final SecurityHelper securityHelper;

    @GetMapping
    public String showCheckout(HttpSession session, Model model) {
        CheckoutCartService.CartData cartData = checkoutService.getCartData(session);
        if (cartData.isEmpty()) {
            return "redirect:/cart";
        }

        User currentUser = securityHelper.getCurrentUser(session);
        boolean isGuest = currentUser == null;

        if (!isGuest) {
            List<UserAddress> savedAddresses = addressService.getUserAddresses(currentUser);
            UserAddress defaultAddress = addressService.getDefaultAddress(currentUser).orElse(null);
            model.addAttribute("user", currentUser);
            model.addAttribute("savedAddresses", savedAddresses);
            model.addAttribute("defaultAddress", defaultAddress);
        }

        model.addAttribute("cartItems", cartData.items());
        model.addAttribute("total", cartData.total());
        model.addAttribute("isGuest", isGuest);
        model.addAttribute("checkoutForm", new CheckoutForm());
        model.addAttribute("cart", new CartViewModel(cartData.items(), cartData.total()));
        model.addAttribute("deliveryMethods", DeliveryMethod.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());

        Map<String, BigDecimal> deliveryPrices = new HashMap<>();
        for (DeliveryMethod method : DeliveryMethod.values()) {
            deliveryPrices.put(method.name().toLowerCase(), method.getPrice());
        }
        model.addAttribute("deliveryPrices", deliveryPrices);
        model.addAttribute("freeShippingThreshold", FREE_SHIPPING_THRESHOLD);

        return "checkout";
    }

    @GetMapping("/validate-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal total) {

        Map<String, Object> response = checkoutService.validateVoucher(code, total);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process")
    public String processCheckout(
            @RequestParam(required = false) String email,
            @RequestParam(required = false, name = "shippingName") String name,
            @RequestParam(required = false, name = "shippingPhone") String phone,
            @RequestParam(required = false, name = "shippingStreet") String street,
            @RequestParam(required = false, name = "shippingCity") String city,
            @RequestParam(required = false, name = "shippingZipCode") String zipCode,
            @RequestParam(defaultValue = "Poland", name = "shippingCountry") String country,
            @RequestParam String deliveryMethod,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) Long addressId,
            @RequestParam(required = false) Boolean saveAddress,
            @RequestParam(required = false) String voucherCode,
            @RequestParam(required = false) String addressLabel,
            @RequestParam(required = false) String inpostLockerName,
            @RequestParam(required = false) String inpostLockerAddress,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        CheckoutCartService.CartData cartData = checkoutService.getCartData(session);

        if (cartData.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Koszyk jest pusty");
            return "redirect:/cart";
        }

        try {
            CheckoutService.CheckoutRequest request = CheckoutService.CheckoutRequest.builder()
                    .email(email)
                    .name(name)
                    .phone(phone)
                    .street(street)
                    .city(city)
                    .zipCode(zipCode)
                    .country(country)
                    .deliveryMethod(deliveryMethod)
                    .paymentMethod(paymentMethod)
                    .addressId(addressId)
                    .saveAddress(saveAddress)
                    .voucherCode(voucherCode)
                    .addressLabel(addressLabel)
                    .inpostLockerName(inpostLockerName)
                    .inpostLockerAddress(inpostLockerAddress)
                    .build();

            Order order = checkoutService.processCheckout(request, cartData, session);

            return "redirect:/checkout/payment?orderNumber=" + order.getOrderNumber();

        } catch (Exception e) {
            log.error("Błąd podczas składania zamówienia: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas składania zamówienia: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/payment")
    public String showPaymentGateway(
            @RequestParam String orderNumber,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            Order order = checkoutPaymentService.getOrderForPayment(orderNumber, session);
            model.addAttribute("order", order);
            return "payment-gateway";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/payment/simulate")
    public String simulatePayment(
            @RequestParam String orderNumber,
            @RequestParam String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            return checkoutPaymentService.processPayment(orderNumber, status, session, redirectAttributes);
        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania płatności: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas przetwarzania płatności: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/confirmation")
    public String orderConfirmation(Model model) {
        return "order-success";
    }
}