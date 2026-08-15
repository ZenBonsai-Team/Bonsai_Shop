package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
// Repository thao tac User, duoc dung cho dang nhap, dang ky, OAuth2 va cac man hinh user.
public interface UserRepository extends JpaRepository<User, Integer> {
    // Tim user theo email, dung lam username cho form login va Google OAuth2.
    Optional<User> findByEmail(String email);
    // Kiem tra email da ton tai khi dang ky tai khoan moi.
    boolean existsByEmail(String email);
    // Kiem tra username da ton tai khi dang ky/cap nhat profile.
    boolean existsByUsername(String username);
    // Lay danh sach user theo status, vi du ACTIVE/PENDING/LOCKED.
    List<User> findByStatus(String status);
    // Lay user theo roleId de phuc vu loc/quan ly nhom tai khoan.
    List<User> findByRoleRoleId(Integer roleId);
    // Lay user theo danh sach roleName, dung cho cac luong can tim moderator/artisan.
    List<User> findByRoleRoleNameIn(List<String> roleNames);

    // Lay artisan noi bat dua tren so bai viet da duyet va tong like bai viet.
    @org.springframework.data.jpa.repository.Query(value = 
        "SELECT u.* FROM USER u " +
        "LEFT JOIN community_post p ON u.UserID = p.AuthorID AND p.Status = 'APPROVED' " +
        "WHERE u.RoleID = 3 AND u.Status = 'ACTIVE' " +
        "GROUP BY u.UserID " +
        "ORDER BY COUNT(p.PostID) DESC, COALESCE(SUM(p.LikesCount), 0) DESC", 
        nativeQuery = true)
    List<User> findFeaturedArtisans();
}
