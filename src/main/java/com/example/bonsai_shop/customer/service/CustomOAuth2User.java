package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final Integer userId;
    private final String email;
    private final String fullName;
    private final String avatar;
    private final String roleName;

    public CustomOAuth2User(User user,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.avatar = user.getAvatar();
        this.roleName = user.getRole() != null ? user.getRole().getRoleName() : "";
    }

    public String getUsername() {
        return email;
    }
}
