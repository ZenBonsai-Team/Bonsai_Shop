package com.example.bonsai_shop.config;

import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

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
