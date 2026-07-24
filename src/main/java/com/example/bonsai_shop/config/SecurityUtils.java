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

public class SecurityUtils {

    /**
     * Trích xuất email từ đối tượng Principal bất kể đăng nhập thông thường (UserDetails) hay Đăng nhập Google (OAuth2User).
     */
    public static String extractEmail(Object principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof CustomOAuth2User customOAuth2User) {
            return customOAuth2User.getEmail();
        } else if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            if (email != null && !email.trim().isEmpty()) {
                return email.trim();
            }
            return oAuth2User.getName();
        }
        return null;
    }

    /**
     * Lấy đối tượng User từ database dựa trên Principal hiện tại.
     */
    public static User getCurrentUser(Object principal, UserRepository userRepository) {
        String email = extractEmail(principal);
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Refreshes active Spring Security Authentication Session instantly whenever user avatar or profile is updated.
     */
    public static void updateSecurityContext(User updatedUser) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            CustomUserDetails updatedPrincipal = new CustomUserDetails(updatedUser, currentAuth.getAuthorities());
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    updatedPrincipal,
                    currentAuth.getCredentials(),
                    currentAuth.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }
}
