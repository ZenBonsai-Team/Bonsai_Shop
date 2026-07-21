package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.CommunityPostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityPostBookmarkRepository extends JpaRepository<CommunityPostBookmark, Integer> {

    Optional<CommunityPostBookmark> findByPostIdAndUserId(Integer postId, Integer userId);

    List<CommunityPostBookmark> findByUserIdOrderByCreatedAtDesc(Integer userId);

    boolean existsByPostIdAndUserId(Integer postId, Integer userId);

    void deleteByPostIdAndUserId(Integer postId, Integer userId);
}
