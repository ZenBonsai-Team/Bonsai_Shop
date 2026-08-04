package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.Review;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ReviewRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Get all APPROVED reviews for a product (shown on product page)
     */
    public List<Review> getApprovedReviews(Integer productId) {
        return reviewRepository.findByProductProductIdAndReviewStatus(productId, "APPROVED");
    }

    /**
     * Alias for getApprovedReviews - used by MarketplaceController
     */
    public List<Review> getApprovedReviewsByProduct(Integer productId) {
        return getApprovedReviews(productId);
    }

    /**
     * Get all reviews by status (for moderator management)
     */
    public List<Review> getReviewsByStatus(String status) {
        return reviewRepository.findByReviewStatus(status);
    }

    /**
     * Check if a customer is eligible to review a product
     * (must have a COMPLETED order containing this product)
     */
    public boolean canCustomerReview(Integer customerId, Integer productId) {
        // Check not already reviewed
        if (reviewRepository.existsByCustomerUserIdAndProductProductId(customerId, productId)) {
            return false;
        }
        // Check if there's a completed order with this product
        return orderRepository.existsByCustomerUserIdAndOrderStatusAndOrderDetails_Product_ProductId(
                customerId, "COMPLETED", productId);
    }

    /**
     * Submit a new review (goes into PENDING state for moderation)
     */
    public Review submitReview(Integer customerId, Integer productId, Integer rating, String comment) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + customerId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        Review review = Review.builder()
                .customer(customer)
                .product(product)
                .rating(rating)
                .comment(comment)
                .reviewStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        return reviewRepository.save(review);
    }

    /**
     * Moderator approves a review
     */
    public Review approveReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        review.setReviewStatus("APPROVED");
        return reviewRepository.save(review);
    }

    /**
     * Moderator rejects a review
     */
    public Review rejectReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        review.setReviewStatus("REJECTED");
        return reviewRepository.save(review);
    }

    /**
     * Moderator or artisan responds to a review
     */
    public Review respondToReview(Integer reviewId, String response) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        review.setResponse(response);
        return reviewRepository.save(review);
    }

    /**
     * Get average rating for a product
     */
    public double getAverageRating(Integer productId) {
        List<Review> approved = reviewRepository.findByProductProductIdAndReviewStatus(productId, "APPROVED");
        if (approved.isEmpty()) return 0.0;
        return approved.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Count approved reviews for a product
     */
    public long countApprovedReviews(Integer productId) {
        return reviewRepository.countByProductProductIdAndReviewStatus(productId, "APPROVED");
    }
}
