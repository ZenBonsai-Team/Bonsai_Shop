package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.Cart;
import com.example.bonsai_shop.entity.CartItem;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.CartItemRepository;
import com.example.bonsai_shop.product.repository.CartRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByCustomerUserId(user.getUserId())
                .orElseGet(() -> cartRepository.save(Cart.builder().customer(user).build()));
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Integer userId) {
        Optional<Cart> cartOpt = cartRepository.findByCustomerUserId(userId);
        return cartOpt.map(cart -> cartItemRepository.findByCartCartId(cart.getCartId()))
                .orElse(Collections.emptyList());
    }

    @Transactional
    public boolean addToCart(Integer userId, Integer productId, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));

        if (!"AVAILABLE".equalsIgnoreCase(product.getProductStatus())) {
            throw new IllegalStateException("Sản phẩm đã được bán hoặc giữ chỗ!");
        }

        Cart cart = getOrCreateCart(user);

        Optional<CartItem> existingItemOpt = cartItemRepository
                .findByCartCartIdAndProductProductId(cart.getCartId(), productId);

        if (existingItemOpt.isPresent()) {
            return true; // Đã có trong giỏ, không thêm trùng
        }

        CartItem newItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .build();
        cartItemRepository.save(newItem);
        return true;
    }

    @Transactional
    public void removeFromCart(Integer userId, Integer productId) {
        Optional<Cart> cartOpt = cartRepository.findByCustomerUserId(userId);
        cartOpt.ifPresent(cart -> 
            cartItemRepository.deleteByCartCartIdAndProductProductId(cart.getCartId(), productId)
        );
    }

    @Transactional
    public void clearCart(Integer userId) {
        Optional<Cart> cartOpt = cartRepository.findByCustomerUserId(userId);
        cartOpt.ifPresent(cart -> cartItemRepository.deleteByCartCartId(cart.getCartId()));
    }
}
