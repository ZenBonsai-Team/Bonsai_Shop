package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Integer> {
    
    List<CommunityPost> findAllByOrderByCreatedAtDesc();
    
    List<CommunityPost> findByCategoryOrderByCreatedAtDesc(String category);
    
    @Query("SELECT p FROM CommunityPost p WHERE " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<CommunityPost> searchPosts(@Param("query") String query);

    @Query("SELECT p FROM CommunityPost p WHERE p.category = :category AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<CommunityPost> searchPostsByCategory(@Param("category") String category, @Param("query") String query);
}
