package com.example.petmarket.service.cart;

import com.example.petmarket.SecurityHelper;
import com.example.petmarket.entity.*;
import com.example.petmarket.repository.UserCartRepository;
import com.example.petmarket.service.interfaces.ICartItem;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartOperationService {

    private final UserCartRepository userCartRepository;
    private final SecurityHelper securityHelper;

    @FunctionalInterface
    public interface CartOperation {
        void execute(UserCart userCart, Cart sessionCart);
    }

    @FunctionalInterface
    public interface CartDataExtractor<T> {
        T extract(UserCart userCart, Cart sessionCart);
    }

    public Cart getSessionCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    public UserCart getUserCart(User user) {
        return userCartRepository.findByUser(user)
                .orElseGet(() -> {
                    UserCart newCart = new UserCart(user);
                    return userCartRepository.save(newCart);
                });
    }

    public void executeCartOperation(HttpSession session, CartOperation operation) {
        User user = securityHelper.getCurrentUser(session);

        if (user != null) {
            UserCart userCart = getUserCart(user);
            operation.execute(userCart, null);
            userCartRepository.save(userCart);
        } else {
            Cart cart = getSessionCart(session);
            operation.execute(null, cart);
        }
    }

    public <T> T extractCartData(HttpSession session, CartDataExtractor<T> extractor) {
        User user = securityHelper.getCurrentUser(session);

        if (user != null) {
            UserCart userCart = getUserCart(user);
            return extractor.extract(userCart, null);
        } else {
            Cart cart = getSessionCart(session);
            return extractor.extract(null, cart);
        }
    }

    public int getCurrentCartQuantity(Long productId, HttpSession session) {
        return extractCartData(session, (userCart, sessionCart) -> {
            if (userCart != null) {
                return userCart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(productId))
                        .findFirst()
                        .map(UserCartItem::getQuantity)
                        .orElse(0);
            } else {
                return sessionCart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(productId))
                        .findFirst()
                        .map(CartItem::getQuantity)
                        .orElse(0);
            }
        });
    }

    public void updateCartItemQuantity(List<? extends ICartItem> cartItems, Long productId, String action, int quantity) {
        for (ICartItem item : cartItems) {
            if (item.getProduct().getId().equals(productId)) {
                switch (action) {
                    case "increase" -> item.setQuantity(item.getQuantity() + 1);
                    case "decrease" -> item.setQuantity(Math.max(1, item.getQuantity() - 1));
                    case "set" -> item.setQuantity(Math.max(1, quantity));
                }
                break;
            }
        }
    }

    public void addProductToCart(Product product, int quantity, HttpSession session) {
        executeCartOperation(session, (userCart, sessionCart) -> {
            if (userCart != null) {
                userCart.addItem(product, quantity);
            } else {
                sessionCart.addItem(product, quantity);
            }
        });
    }

    public void removeProductFromCart(Long productId, HttpSession session) {
        executeCartOperation(session, (userCart, sessionCart) -> {
            if (userCart != null) {
                userCart.removeItem(productId);
            } else {
                sessionCart.removeItem(productId);
            }
        });
    }

    public void clearCart(HttpSession session) {
        executeCartOperation(session, (userCart, sessionCart) -> {
            if (userCart != null) {
                userCart.clear();
            } else {
                sessionCart.clear();
            }
        });
    }

    public Cart getDisplayCart(HttpSession session) {
        return extractCartData(session, (userCart, sessionCart) -> {
            if (userCart != null) {
                Cart cart = new Cart();
                userCart.getItems().forEach(item -> cart.addItem(item.getProduct(), item.getQuantity()));
                return cart;
            } else {
                return sessionCart;
            }
        });
    }

    public int getTotalItems(HttpSession session) {
        return extractCartData(session, (userCart, sessionCart) ->
                userCart != null ? userCart.getTotalItems() : sessionCart.getTotalItems()
        );
    }
}
