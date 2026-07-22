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

    Page<CommunityPost> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<CommunityPost> findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(String title, String authorName, Pageable pageable);

    Page<CommunityPost> findByStatusAndTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(String status, String title, String authorName, Pageable pageable);

    List<CommunityPost> findAllByStatusOrderByCreatedAtDesc(String status);
    
    List<CommunityPost> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);
    
    @Query("SELECT p FROM CommunityPost p WHERE p.status = 'APPROVED' AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query1, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query1, '%')) OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query2, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query2, '%')) OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query3, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query3, '%')) OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query4, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query4, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<CommunityPost> searchPostsSmart(@Param("query1") String query1, 
                                         @Param("query2") String query2, 
                                         @Param("query3") String query3,
                                         @Param("query4") String query4);

    @Query("SELECT p FROM CommunityPost p WHERE p.status = 'APPROVED' AND p.category = :category AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query1, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query1, '%')) OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query2, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query2, '%')) OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query3, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query3, '%')) OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query4, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query4, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<CommunityPost> searchPostsByCategorySmart(@Param("category") String category, 
                                                   @Param("query1") String query1, 
                                                   @Param("query2") String query2, 
                                                   @Param("query3") String query3,
                                                   @Param("query4") String query4);

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
