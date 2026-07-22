package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.CartItem;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.CartItemRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Integer userId) {
        return cartItemRepository.findByUserUserId(userId);
    }

    @Transactional
    public boolean addToCart(Integer userId, Integer productId, Integer quantity, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));

        if (!"AVAILABLE".equalsIgnoreCase(product.getProductStatus())) {
            throw new IllegalStateException("Sản phẩm đã được bán hoặc giữ chỗ!");
        }

        // Tác phẩm Bonsai độc bản chỉ cho phép số lượng tối đa là 1
        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserUserIdAndProductProductId(userId, productId);
        if (existingItemOpt.isPresent()) {
            return true; // Đã có trong giỏ, không cần làm gì thêm
        }

        CartItem newItem = CartItem.builder()
                .user(user)
                .product(product)
                .quantity(1) // Khóa cứng số lượng 1 cho tác phẩm Bonsai độc bản
                .build();
        cartItemRepository.save(newItem);
        return true;
    }

    @Transactional
    public void removeFromCart(Integer userId, Integer productId) {
        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserUserIdAndProductProductId(userId, productId);
        existingItemOpt.ifPresent(cartItemRepository::delete);
    }

    @Transactional
    public void clearCart(Integer userId) {
        cartItemRepository.deleteByUserUserId(userId);
    }
}
