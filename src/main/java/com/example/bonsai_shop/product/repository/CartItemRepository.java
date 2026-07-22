package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartCartId(Integer cartId);
    Optional<CartItem> findByCartCartIdAndProductProductId(Integer cartId, Integer productId);
    void deleteByCartCartId(Integer cartId);
    void deleteByCartCartIdAndProductProductId(Integer cartId, Integer productId);
}
