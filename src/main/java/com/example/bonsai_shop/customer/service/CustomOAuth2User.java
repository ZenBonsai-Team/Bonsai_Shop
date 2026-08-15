package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
// Principal cho dang nhap Google OAuth2, giu attributes cua Google va thong tin User noi bo.
public class CustomOAuth2User extends DefaultOAuth2User {

    private final Integer userId;
    private final String email;
    private final String fullName;
    private final String avatar;
    private final String roleName;

    // Tao OAuth2 principal rieng de cac man hinh doc duoc userId/email/fullName/avatar/roleName.
    public CustomOAuth2User(User user,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
        // Copy thong tin tu User entity da duoc dong bo/tao trong CustomOAuth2UserService.
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.avatar = user.getAvatar();
        this.roleName = user.getRole() != null ? user.getRole().getRoleName() : "";
    }

    // Dong bo cach lay username voi CustomUserDetails: OAuth2 user cung dung email.
    public String getUsername() {
        return email;
    }
}
