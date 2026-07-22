package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.CartItem;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.dto.CartItemResponseDTO;
import com.example.bonsai_shop.product.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<CartItemResponseDTO>> getCart(@AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        List<CartItem> items = cartService.getCartItems(user.getUserId());
        List<CartItemResponseDTO> dtoList = items.stream().map(item -> CartItemResponseDTO.builder()
                .cartItemId(item.getCartItemId())
                .productId(item.getProduct().getProductId())
                .productName(item.getProduct().getProductName())
                .productImage(item.getProduct().getFirstImageUrl())
                .price(item.getProduct().getPrice())
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestBody Map<String, Integer> payload,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập trước khi thêm vào giỏ hàng.");
            return ResponseEntity.status(401).body(response);
        }

        User user = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Tài khoản không khả dụng.");
            return ResponseEntity.badRequest().body(response);
        }

        Integer productId = payload.get("productId");
        try {
            boolean success = cartService.addToCart(user.getUserId(), productId, user);
            response.put("success", success);
            response.put("message", "Đã thêm tác phẩm vào giỏ hàng thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @PathVariable Integer productId,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        cartService.removeFromCart(user.getUserId(), productId);
        response.put("success", true);
        response.put("message", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return ResponseEntity.ok(response);
    }
}
