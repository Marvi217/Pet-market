package com.example.petmarket.controller;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.*;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentMethod;
import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.repository.UserRepository;
import com.example.petmarket.service.OrderService;
import com.example.petmarket.service.UserAddressService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;
    private final UserAddressService addressService;

    @GetMapping
    public String showCheckout(HttpSession session, Model model) {
        Cart cart = getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isGuest = auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal());

        List<UserAddress> savedAddresses;
        if (!isGuest) {
            User user = securityHelper.getCurrentUser(session);
            savedAddresses = addressService.getUserAddresses(user);
            model.addAttribute("user", user);
            model.addAttribute("savedAddresses", savedAddresses);
        }

        model.addAttribute("cartItems", cart.getItems());
        model.addAttribute("total", cart.getTotal());
        model.addAttribute("isGuest", isGuest);

        return "checkout";
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
            @RequestParam(required = false) String addressLabel,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Cart cart = getCart(session);

        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Koszyk jest pusty");
            return "redirect:/cart";
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = null;

            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
            }

            Order order = new Order();
            order.setUser(currentUser);
            order.setOrderNumber(generateOrderNumber());
            order.setOrderDate(LocalDateTime.now());
            order.setStatus(OrderStatus.PENDING);

            if (currentUser != null) {
                order.setGuestName(currentUser.getFullName());
                order.setGuestEmail(currentUser.getEmail());
                order.setGuestPhone(phone != null ? phone : "");
            } else {
                order.setGuestName(name);
                order.setGuestEmail(email);
                order.setGuestPhone(phone);
            }

            Address address = null;

            if (addressId != null && currentUser != null) {
                UserAddress userAddress = addressService.getAddressById(addressId, currentUser)
                        .orElseThrow(() -> new IllegalArgumentException("Wybrany adres nie istnieje"));

                address = new Address();
                address.setStreet(userAddress.getStreet());
                address.setCity(userAddress.getCity());
                address.setZipCode(userAddress.getZipCode());
                address.setCountry(userAddress.getCountry());
            }
            else if (street != null && !street.isEmpty()) {
                address = new Address();
                address.setStreet(street);
                address.setCity(city);
                address.setZipCode(zipCode);
                address.setCountry(country != null ? country : "Poland");

                if (Boolean.TRUE.equals(saveAddress) && currentUser != null) {
                    try {
                        UserAddress newUserAddress = new UserAddress();
                        newUserAddress.setUser(currentUser);
                        newUserAddress.setLabel(addressLabel != null && !addressLabel.isEmpty()
                                ? addressLabel
                                : "Adres z zamówienia " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                        newUserAddress.setName(currentUser.getFullName());
                        newUserAddress.setStreet(street);
                        newUserAddress.setCity(city);
                        newUserAddress.setZipCode(zipCode);
                        newUserAddress.setCountry(country != null ? country : "Poland");
                        newUserAddress.setPhoneNumber(phone);

                        addressService.saveAddress(newUserAddress, currentUser);

                    } catch (Exception e) {
                        System.err.println("⚠ Nie udało się zapisać adresu: " + e.getMessage());
                    }
                }
            }

            order.setShippingAddress(address);

            DeliveryMethod delivery;
            try {
                delivery = DeliveryMethod.valueOf(deliveryMethod);
            } catch (IllegalArgumentException e) {
                delivery = DeliveryMethod.COURIER;
            }
            order.setDeliveryMethod(delivery);

            BigDecimal deliveryCost = calculateDeliveryCost(delivery, cart.getTotal());
            order.setDeliveryCost(deliveryCost);

            try {
                order.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));
            } catch (IllegalArgumentException e) {
                order.setPaymentMethod(PaymentMethod.CARD);
            }

            List<OrderItem> orderItems = new ArrayList<>();
            for (var cartItem : cart.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());

                BigDecimal price = cartItem.getProduct().getPrice();
                if (price == null) {
                    price = BigDecimal.ZERO;
                }
                orderItem.setPrice(price);
                orderItems.add(orderItem);
            }
            order.setItems(orderItems);

            order.setSubtotal(cart.getTotal());
            order.setTotalAmount(cart.getTotal().add(deliveryCost));

            orderService.save(order);

            cart.clear();

            redirectAttributes.addFlashAttribute("orderNumber", order.getOrderNumber());
            redirectAttributes.addFlashAttribute("success", "Zamówienie zostało złożone!");
            return "redirect:/checkout/confirmation";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Błąd podczas składania zamówienia: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/confirmation")
    public String orderConfirmation(Model model) {
        return "order-success";
    }

    private Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    private BigDecimal calculateDeliveryCost(DeliveryMethod method, BigDecimal cartTotal) {
        if (cartTotal.compareTo(new BigDecimal("199")) >= 0) {
            return BigDecimal.ZERO;
        }
        return method.getPrice();
    }
}