package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.SecurityUtils;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.dto.WishlistItemResponseDTO;
import com.example.bonsai_shop.product.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistApiController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponseDTO>> getWishlist(@AuthenticationPrincipal Object principal) {
        User user = SecurityUtils.getCurrentUser(principal, userRepository);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isCustomer()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(wishlistService.getWishlistItems(user));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> countWishlist(@AuthenticationPrincipal Object principal) {
        User user = SecurityUtils.getCurrentUser(principal, userRepository);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isCustomer()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(Map.of("count", wishlistService.countWishlistItems(user)));
    }

    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> addToWishlist(
            @RequestBody Map<String, Integer> payload,
            @AuthenticationPrincipal Object principal) {
        User user = SecurityUtils.getCurrentUser(principal, userRepository);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Vui lòng đăng nhập để lưu Wishlist."
            ));
        }
        if (!isCustomer()) {
            return forbiddenWishlistResponse();
        }

        Integer productId = payload.get("productId");
        if (productId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thiếu mã sản phẩm."
            ));
        }
        Map<String, Object> response = new HashMap<>();
        try {
            boolean added = wishlistService.addToWishlist(user, productId);
            response.put("success", true);
            response.put("added", added);
            response.put("message", added ? "Đã thêm vào Wishlist." : "Sản phẩm đã có trong Wishlist.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @RequestBody Map<String, Integer> payload,
            @AuthenticationPrincipal Object principal) {
        User user = SecurityUtils.getCurrentUser(principal, userRepository);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }
        if (!isCustomer()) {
            return forbiddenWishlistResponse();
        }

        Integer productId = payload.get("productId");
        if (productId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thiếu mã sản phẩm."
            ));
        }
        try {
            boolean active = wishlistService.toggleWishlist(user, productId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "active", active,
                    "message", active ? "Đã thêm vào Wishlist." : "Đã xóa khỏi Wishlist."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromWishlist(
            @PathVariable Integer productId,
            @AuthenticationPrincipal Object principal) {
        User user = SecurityUtils.getCurrentUser(principal, userRepository);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isCustomer()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        wishlistService.removeFromWishlist(user, productId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xóa khỏi Wishlist."
        ));
    }

    private ResponseEntity<Map<String, Object>> forbiddenWishlistResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "Chỉ tài khoản khách hàng được dùng Wishlist."
        ));
    }

    private boolean isCustomer() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
    }
}
