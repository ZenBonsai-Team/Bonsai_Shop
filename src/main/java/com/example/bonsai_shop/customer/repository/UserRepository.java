package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByStatus(String status);
    List<User> findByRoleRoleId(Integer roleId);
    List<User> findByRoleRoleNameIn(List<String> roleNames);

    @org.springframework.data.jpa.repository.Query(value = 
        "SELECT u.* FROM USER u " +
        "LEFT JOIN community_post p ON u.UserID = p.AuthorID AND p.Status = 'APPROVED' " +
        "WHERE u.RoleID = 3 AND u.Status = 'ACTIVE' " +
        "GROUP BY u.UserID " +
        "ORDER BY COUNT(p.PostID) DESC, COALESCE(SUM(p.LikesCount), 0) DESC", 
        nativeQuery = true)
    List<User> findFeaturedArtisans();
}
