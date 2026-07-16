package com.example.bonsai_shop.customer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bonsai_shop.entity.CommunityPostLike;

@Repository
public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Integer> {
    /** Check if a specific user has already liked a post */
    Optional<CommunityPostLike> findByPostIdAndUserId(Integer postId, Integer userId);

    /** Count total likes for a post */
    long countByPostId(Integer postId);
}
