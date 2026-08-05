package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProductProductIdAndReviewStatus(Integer productId, String reviewStatus);
    List<Review> findByReviewStatus(String reviewStatus);
    long countByProductProductIdAndReviewStatus(Integer productId, String reviewStatus);
    boolean existsByCustomerUserIdAndProductProductId(Integer customerId, Integer productId);
    List<Review> findByCustomerUserId(Integer customerId);
}
