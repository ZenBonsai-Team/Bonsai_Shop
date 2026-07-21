package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Locale;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Integer userId;
    private final String email;
    private final String password;
    private final String fullName;
    private final String avatar;
    private final String roleName;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.fullName = user.getFullName();
        this.avatar = user.getAvatar();
        this.roleName = user.getRole() != null ? user.getRole().getRoleName() : "";

        String status = user.getStatus() == null ? "" : user.getStatus().trim().toUpperCase(Locale.ROOT);
        this.enabled = "ACTIVE".equals(status);
        this.accountNonLocked = !"LOCKED".equals(status);
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
