package com.example.petmarket.service.checkout;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.Promotion;
import com.example.petmarket.repository.PromotionRepository;
import com.example.petmarket.service.promotion.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckoutVoucherService {

    private final PromotionService promotionService;
    private final PromotionRepository promotionRepository;

    public Map<String, Object> validateVoucher(String code, BigDecimal total) {
        Map<String, Object> response = new HashMap<>();

        return promotionService.getPromotionByCode(code)
                .map(promotion -> {
                    if (!promotion.isCurrentlyActive()) {
                        response.put("valid", false);
                        response.put("message", "Ten kod promocyjny wygasł lub jest nieaktywny");
                        return response;
                    }

                    if (!promotion.isApplicableForAmount(total)) {
                        response.put("valid", false);
                        response.put("message", "Minimalna wartość zamówienia: " + promotion.getMinOrderAmount() + " zł");
                        return response;
                    }

                    BigDecimal discount = promotion.calculateDiscount(total);
                    response.put("valid", true);
                    response.put("discount", discount);
                    response.put("promotionId", promotion.getId());
                    response.put("message", "Kod zastosowany! Zniżka: " + discount.setScale(2) + " zł");
                    return response;
                })
                .orElseGet(() -> {
                    response.put("valid", false);
                    response.put("message", "Nieprawidłowy kod rabatowy");
                    return response;
                });
    }

    public BigDecimal applyPromotion(Order order, String voucherCode, BigDecimal subtotal) {
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return promotionService.getPromotionByCode(voucherCode.trim())
                .filter(Promotion::isCurrentlyActive)
                .filter(promotion -> promotion.isApplicableForAmount(subtotal))
                .map(promotion -> {
                    BigDecimal discount = promotion.calculateDiscount(subtotal);
                    order.setPromotion(promotion);
                    order.setPromotionCode(voucherCode.trim());
                    order.setDiscountAmount(discount);
                    promotion.setCurrentUsage(promotion.getCurrentUsage() + 1);
                    promotionRepository.save(promotion);
                    return discount;
                })
                .orElse(BigDecimal.ZERO);
    }
}