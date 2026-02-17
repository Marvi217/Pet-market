package com.example.petmarket.service.order;

import com.example.petmarket.entity.Order;
import com.example.petmarket.entity.OrderItem;
import com.example.petmarket.entity.Product;
import com.example.petmarket.entity.Promotion;
import com.example.petmarket.enums.OrderStatus;
import com.example.petmarket.service.stock.StockManager;
import org.springframework.stereotype.Component;

@Component
public class OrderCancellationService {

    private final OrderStatusManager orderStatusManager;
    private final StockManager stockManager;

    public OrderCancellationService(OrderStatusManager orderStatusManager, StockManager stockManager) {
        this.orderStatusManager = orderStatusManager;
        this.stockManager = stockManager;
    }

    public void cancel(Order order, String reason) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Zamówienie jest już anulowane.");
        }

        orderStatusManager.changeStatus(order, OrderStatus.CANCELLED);
        order.setCancellationReason(reason);

        restoreStock(order);
        restorePromotionUsage(order);
    }

    private void restoreStock(Order order) {
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    stockManager.increaseStock(product, item.getQuantity());
                }
            }
        }
    }

    private void restorePromotionUsage(Order order) {
        Promotion promotion = order.getPromotion();
        if (promotion != null && promotion.getCurrentUsage() > 0) {
            promotion.setCurrentUsage(promotion.getCurrentUsage() - 1);
        }
    }
}