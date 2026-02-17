package com.example.petmarket.service.checkout;

import com.example.petmarket.entity.Address;
import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.User;
import com.example.petmarket.entity.UserAddress;
import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.service.user.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutAddressService {

    private static final String DEFAULT_COUNTRY = "Poland";

    private final UserAddressService addressService;

    public Address buildShippingAddress(CheckoutRequest request, User currentUser, Order order) {
        DeliveryMethod delivery = mapDeliveryMethod(request.deliveryMethod());

        if (delivery == DeliveryMethod.LOCKER && request.inpostLockerAddress() != null) {
            return createInpostAddress(request, order);
        } else if (request.addressId() != null && currentUser != null) {
            return createAddressFromSaved(request.addressId(), currentUser);
        } else if (request.street() != null && !request.street().isEmpty()) {
            return createNewAddress(request, currentUser);
        }

        return null;
    }

    private Address createInpostAddress(CheckoutRequest request, Order order) {
        Address address = new Address();
        String lockerAddress = request.inpostLockerAddress();

        if (lockerAddress.contains(",")) {
            String[] parts = lockerAddress.split(",", 2);
            address.setStreet(parts[0].trim());
            address.setCity(parts.length > 1 ? parts[1].trim() : "");
        } else {
            address.setStreet(lockerAddress);
            address.setCity("");
        }

        address.setZipCode("");
        address.setCountry(DEFAULT_COUNTRY);

        if (request.inpostLockerName() != null && !request.inpostLockerName().isEmpty()) {
            order.setNotes((order.getNotes() != null ? order.getNotes() + "\n" : "") +
                    "Paczkomat InPost: " + request.inpostLockerName());
        }

        return address;
    }

    private Address createAddressFromSaved(Long addressId, User currentUser) {
        UserAddress userAddress = addressService.getAddressById(addressId, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Wybrany adres nie istnieje"));

        Address address = new Address();
        address.setStreet(userAddress.getStreet());
        address.setCity(userAddress.getCity());
        address.setZipCode(userAddress.getZipCode());
        address.setCountry(userAddress.getCountry());

        return address;
    }

    private Address createNewAddress(CheckoutRequest request, User currentUser) {
        Address address = new Address();
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setZipCode(request.zipCode());
        address.setCountry(request.country() != null ? request.country() : DEFAULT_COUNTRY);

        if (Boolean.TRUE.equals(request.saveAddress()) && currentUser != null) {
            saveUserAddress(request, currentUser);
        }

        return address;
    }

    private void saveUserAddress(CheckoutRequest request, User currentUser) {
        try {
            UserAddress newUserAddress = new UserAddress();
            newUserAddress.setUser(currentUser);
            newUserAddress.setLabel(request.addressLabel() != null && !request.addressLabel().isEmpty()
                    ? request.addressLabel()
                    : "Adres z zamówienia " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            newUserAddress.setName(currentUser.getFullName());
            newUserAddress.setStreet(request.street());
            newUserAddress.setCity(request.city());
            newUserAddress.setZipCode(request.zipCode());
            newUserAddress.setCountry(request.country() != null ? request.country() : DEFAULT_COUNTRY);
            newUserAddress.setPhoneNumber(request.phone());

            UserAddress savedAddress = addressService.saveAddress(newUserAddress, currentUser);

            log.info("Adres został zapisany dla przyszłych zamówień - ID: {}, Label: {}, Is Default: {}",
                    savedAddress.getId(), savedAddress.getLabel(), savedAddress.isDefault());
        } catch (Exception e) {
            log.error("Nie udało się zapisać adresu: {}", e.getMessage(), e);
        }
    }

    private DeliveryMethod mapDeliveryMethod(String method) {
        if (method == null) return DeliveryMethod.COURIER;
        return switch (method.toLowerCase()) {
            case "inpost" -> DeliveryMethod.LOCKER;
            case "pickup" -> DeliveryMethod.PICKUP;
            default -> DeliveryMethod.COURIER;
        };
    }

    public record CheckoutRequest(
            String email, String name, String phone,
            String street, String city, String zipCode, String country,
            String deliveryMethod, String paymentMethod,
            Long addressId, Boolean saveAddress, String voucherCode,
            String addressLabel, String inpostLockerName, String inpostLockerAddress
    ) {}
}