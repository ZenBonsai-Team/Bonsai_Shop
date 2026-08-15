package com.example.bonsai_shop.config;

import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomOAuth2User;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

// Helper dung chung de doc principal hien tai cho ca form login va Google OAuth2 login.
public class SecurityUtils {

    /**
     * Trích xuất email từ đối tượng Principal bất kể đăng nhập thông thường (UserDetails) hay Đăng nhập Google (OAuth2User).
     */
    public static String extractEmail(Object principal) {
        // Principal null nghia la chua dang nhap hoac request khong co authentication.
        if (principal == null) {
            return null;
        }
        // Dang nhap form tra ve UserDetails, username trong he thong la email.
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof CustomOAuth2User customOAuth2User) {
            // Dang nhap Google bang principal rieng thi doc email da dong bo tu User entity.
            return customOAuth2User.getEmail();
        } else if (principal instanceof OAuth2User oAuth2User) {
            // Fallback cho OAuth2User mac dinh: uu tien attribute email cua provider.
            String email = oAuth2User.getAttribute("email");
            if (email != null && !email.trim().isEmpty()) {
                return email.trim();
            }
            // Neu provider khong co email thi dung name cua OAuth2User lam fallback.
            return oAuth2User.getName();
        }
        return null;
    }

    /**
     * Lấy đối tượng User từ database dựa trên Principal hiện tại.
     */
    public static User getCurrentUser(Object principal, UserRepository userRepository) {
        // Rut email thong nhat tu principal form/OAuth2.
        String email = extractEmail(principal);
        // Khong co email thi khong the truy van user.
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        // Tim user trong database theo email cua principal hien tai.
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Refreshes active Spring Security Authentication Session instantly whenever user avatar or profile is updated.
     */
    public static void updateSecurityContext(User updatedUser) {
        // Lay Authentication hien tai trong thread/request dang xu ly.
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            // Tao principal moi tu User vua cap nhat nhung giu nguyen bo authorities hien tai.
            CustomUserDetails updatedPrincipal = new CustomUserDetails(updatedUser, currentAuth.getAuthorities());
            // Tao Authentication moi de session thay ngay thong tin avatar/profile moi.
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    updatedPrincipal,
                    currentAuth.getCredentials(),
                    currentAuth.getAuthorities()
            );
            // Ghi Authentication moi vao SecurityContext.
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }
}
