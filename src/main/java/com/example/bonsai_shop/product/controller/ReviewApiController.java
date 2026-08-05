package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.config.SecurityUtils;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Review;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    /**
     * GET /api/reviews/{productId} - Get approved reviews for a product (public)
     */
    @GetMapping("/{productId}")
    public ResponseEntity<List<Review>> getProductReviews(@PathVariable Integer productId) {
        return ResponseEntity.ok(reviewService.getApprovedReviews(productId));
    }

    /**
     * GET /api/reviews/{productId}/can-review - Check if current user can review this product
     */
    @GetMapping("/{productId}/can-review")
    public ResponseEntity<Map<String, Boolean>> canReview(
            @PathVariable Integer productId,
            @AuthenticationPrincipal Object principal) {
        User user = SecurityUtils.getCurrentUser(principal, userRepository);
        if (user == null) {
            return ResponseEntity.ok(Map.of("canReview", false));
        }
        boolean canReview = reviewService.canCustomerReview(user.getUserId(), productId);
        return ResponseEntity.ok(Map.of("canReview", canReview));
    }

    /**
     * POST /api/reviews/{productId} - Submit a new review (Customer only)
     */
    @PostMapping("/{productId}")
    public ResponseEntity<?> submitReview(
            @PathVariable Integer productId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Object principal) {
        User user = SecurityUtils.getCurrentUser(principal, userRepository);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        try {
            Integer rating = Integer.valueOf(body.get("rating").toString());
            String comment = body.getOrDefault("comment", "").toString();

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Đánh giá phải từ 1 đến 5 sao"));
            }
            if (!reviewService.canCustomerReview(user.getUserId(), productId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn không thể đánh giá sản phẩm này"));
            }

            Review saved = reviewService.submitReview(user.getUserId(), productId, rating, comment);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cảm ơn bạn đã đánh giá! Đánh giá của bạn đang chờ kiểm duyệt.",
                    "reviewId", saved.getReviewId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/reviews/{reviewId}/approve - Moderator approves a review
     */
    @PutMapping("/{reviewId}/approve")
    public ResponseEntity<?> approveReview(@PathVariable Integer reviewId) {
        try {
            Review updated = reviewService.approveReview(reviewId);
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getReviewStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/reviews/{reviewId}/reject - Moderator rejects a review
     */
    @PutMapping("/{reviewId}/reject")
    public ResponseEntity<?> rejectReview(@PathVariable Integer reviewId) {
        try {
            Review updated = reviewService.rejectReview(reviewId);
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getReviewStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/reviews/{reviewId}/respond - Moderator/Artisan responds to a review
     */
    @PutMapping("/{reviewId}/respond")
    public ResponseEntity<?> respondToReview(
            @PathVariable Integer reviewId,
            @RequestBody Map<String, String> body) {
        try {
            String response = body.get("response");
            Review updated = reviewService.respondToReview(reviewId, response);
            return ResponseEntity.ok(Map.of("success", true, "reviewId", updated.getReviewId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/reviews/pending - Moderator gets all pending reviews
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Review>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getReviewsByStatus("PENDING"));
    }
}
