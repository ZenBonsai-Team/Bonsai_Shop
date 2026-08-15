package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Locale;

@Getter
// Principal cho dang nhap form: boc User entity thanh UserDetails de Spring Security su dung.
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

    // Copy cac thong tin can hien thi/phan quyen tu User entity vao principal trong session.
    public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.fullName = user.getFullName();
        this.avatar = user.getAvatar();
        this.roleName = user.getRole() != null ? user.getRole().getRoleName() : "";

        // Chuan hoa status de Spring Security biet tai khoan co duoc dang nhap va co bi khoa khong.
        String status = user.getStatus() == null ? "" : user.getStatus().trim().toUpperCase(Locale.ROOT);
        this.enabled = "ACTIVE".equals(status);
        this.accountNonLocked = !"LOCKED".equals(status);
        // Authorities gom ROLE_xxx va ACTION_xxx da duoc build o CustomUserDetailsService.
        this.authorities = authorities;
    }

    @Override
    // Spring Security dung username lam dinh danh dang nhap; he thong nay dung email.
    public String getUsername() {
        return email;
    }

    @Override
    // He thong chua ap dung ngay het han tai khoan nen luon tra true.
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    // He thong chua ap dung ngay het han credential nen luon tra true.
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
