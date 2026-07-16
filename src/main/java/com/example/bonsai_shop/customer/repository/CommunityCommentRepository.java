package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Integer> {
    List<CommunityComment> findByPostIdOrderByCreatedAtDesc(Integer postId);
    List<CommunityComment> findAllByOrderByCreatedAtDesc();
    Page<CommunityComment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
