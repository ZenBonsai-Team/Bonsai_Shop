package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Integer> {
    
    List<CommunityPost> findAllByOrderByCreatedAtDesc();
    
    Page<CommunityPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CommunityPost> findAllByStatusOrderByCreatedAtDesc(String status);
    
    List<CommunityPost> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);
    
    @Query("SELECT p FROM CommunityPost p WHERE p.status = 'APPROVED' AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<CommunityPost> searchPosts(@Param("query") String query);

    @Query("SELECT p FROM CommunityPost p WHERE p.status = 'APPROVED' AND p.category = :category AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<CommunityPost> searchPostsByCategory(@Param("category") String category, @Param("query") String query);

    List<CommunityPost> findTop3ByCategoryAndStatusAndPostIdNotOrderByCreatedAtDesc(String category, String status, Integer postId);

    List<CommunityPost> findByAuthorIdOrderByCreatedAtDesc(Integer authorId);

    List<CommunityPost> findByAuthorNameOrderByCreatedAtDesc(String authorName);

    List<CommunityPost> findByAuthorIdAndCategoryOrderByCreatedAtDesc(Integer authorId, String category);
}
