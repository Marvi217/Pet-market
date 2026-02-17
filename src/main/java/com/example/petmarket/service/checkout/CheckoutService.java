package com.example.petmarket.service.checkout;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.*;
import com.example.petmarket.enums.DeliveryMethod;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.enums.PaymentMethod;
import com.example.petmarket.service.stock.StockService;
import com.example.petmarket.service.interfaces.IOrderCrudService;
import jakarta.servlet.http.HttpSession;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CheckoutService {

    private final IOrderCrudService orderCrudService;
    private final SecurityHelper securityHelper;
    private final StockService stockService;
    private final CheckoutCartService cartService;
    private final CheckoutVoucherService voucherService;
    private final CheckoutAddressService addressService;
    private final CheckoutDeliveryMapper deliveryMapper;

    @Builder
    public record CheckoutRequest(
            String email, String name, String phone,
            String street, String city, String zipCode, String country,
            String deliveryMethod, String paymentMethod,
            Long addressId, Boolean saveAddress, String voucherCode,
            String addressLabel, String inpostLockerName, String inpostLockerAddress
    ) {}

    public CheckoutCartService.CartData getCartData(HttpSession session) {
        return cartService.getCartData(session);
    }

    public void clearCart(HttpSession session) {
        cartService.clearCart(session);
    }

    public Map<String, Object> validateVoucher(String code, BigDecimal total) {
        return voucherService.validateVoucher(code, total);
    }

    public Order processCheckout(CheckoutRequest request, CheckoutCartService.CartData cartData, HttpSession session) {
        User currentUser = securityHelper.getCurrentUser(session);

        Order order = createBaseOrder(currentUser, request);

        CheckoutAddressService.CheckoutRequest addressRequest = mapToAddressRequest(request);
        Address shippingAddress = addressService.buildShippingAddress(addressRequest, currentUser, order);
        order.setShippingAddress(shippingAddress);

        DeliveryMethod delivery = deliveryMapper.mapDeliveryMethod(request.deliveryMethod());
        order.setDeliveryMethod(delivery);
        order.setDeliveryCost(deliveryMapper.calculateDeliveryCost(delivery, cartData.total()));

        PaymentMethod payment = deliveryMapper.mapPaymentMethod(request.paymentMethod());
        order.setPaymentMethod(payment);

        List<OrderItem> orderItems = createOrderItems(order, cartData.items());
        order.setItems(orderItems);

        BigDecimal originalSubtotal = BigDecimal.ZERO;
        BigDecimal autoDiscount = BigDecimal.ZERO;
        Set<Promotion> usedAutoPromotions = new HashSet<>();

        for (CartItem item : cartData.items()) {
            Product product = item.getProduct();
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            BigDecimal originalTotal = product.getPrice().multiply(qty);
            BigDecimal actualTotal = item.getSubtotal();

            originalSubtotal = originalSubtotal.add(originalTotal);

            BigDecimal saving = originalTotal.subtract(actualTotal);
            if (saving.compareTo(BigDecimal.ZERO) > 0) {
                autoDiscount = autoDiscount.add(saving);
                Promotion autoPromo = product.getCurrentPromotion();
                if (autoPromo != null) {
                    usedAutoPromotions.add(autoPromo);
                }
            }
        }

        order.setSubtotal(originalSubtotal);

        BigDecimal voucherDiscount = voucherService.applyPromotion(order, request.voucherCode(), cartData.total());

        order.setDiscountAmount(autoDiscount.add(voucherDiscount));

        BigDecimal totalAmount = originalSubtotal
                .subtract(autoDiscount)
                .subtract(voucherDiscount)
                .add(order.getDeliveryCost())
                .add(payment.getPrice());
        order.setTotalAmount(totalAmount.max(BigDecimal.ZERO));

        for (Promotion promo : usedAutoPromotions) {
            promo.setCurrentUsage(promo.getCurrentUsage() + 1);
        }

        orderCrudService.save(order);

        updateStock(cartData.items());
        clearCart(session);

        log.info("=== NEW ORDER === Order Number: {}, Customer: {} ({}), Delivery: {}, Address Saved: {}",
                order.getOrderNumber(),
                currentUser != null ? currentUser.getFullName() : order.getGuestName(),
                currentUser != null ? "Logged User" : "Guest",
                delivery,
                Boolean.TRUE.equals(request.saveAddress()) && currentUser != null ? "Yes" : "No");

        return order;
    }

    private CheckoutAddressService.CheckoutRequest mapToAddressRequest(CheckoutRequest request) {
        return new CheckoutAddressService.CheckoutRequest(
                request.email(), request.name(), request.phone(),
                request.street(), request.city(), request.zipCode(), request.country(),
                request.deliveryMethod(), request.paymentMethod(),
                request.addressId(), request.saveAddress(), request.voucherCode(),
                request.addressLabel(), request.inpostLockerName(), request.inpostLockerAddress()
        );
    }

    private Order createBaseOrder(User currentUser, CheckoutRequest request) {
        Order order = new Order();
        order.setUser(currentUser);
        order.setOrderNumber(generateOrderNumber());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        if (currentUser != null) {
            order.setGuestName(currentUser.getFullName());
            order.setGuestEmail(currentUser.getEmail());
            order.setGuestPhone(request.phone() != null ? request.phone() : "");
        } else {
            order.setGuestName(request.name());
            order.setGuestEmail(request.email());
            order.setGuestPhone(request.phone());
        }

        return order;
    }

    private List<OrderItem> createOrderItems(Order order, List<CartItem> cartItems) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());

            BigDecimal price = cartItem.getProduct().getCurrentPrice();
            orderItem.setPrice(price != null ? price : BigDecimal.ZERO);
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private void updateStock(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            stockService.decreaseStock(cartItem.getProduct().getId(), cartItem.getQuantity());
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }
}