package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.customer.repository.RoleActionRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
// Service duoc Spring Security goi khi dang nhap bang email/password de load
// user va quyen.
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleActionRepository roleActionRepository;

    @Override
    // Load user theo email dang nhap, dong thoi build danh sach role/action
    // authority cho SecurityContext.
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Tim user theo email; neu khong co thi Spring Security coi la dang nhap that
        // bai.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy email: " + email));

        // Validate tai khoan phai co role de biet duoc quyen truy cap.
        if (user.getRole() == null || user.getRole().getRoleName() == null) {
            throw new UsernameNotFoundException("Tài khoản chưa được gán role!");
        }

        // Chuan hoa role ve dang ROLE_xxx de dung voi hasRole/hasAuthority cua Spring
        // Security.
        String normRole = normalizeRoleName(user.getRole().getRoleName());
        SimpleGrantedAuthority roleAuthority = new SimpleGrantedAuthority(normRole);

        // Lay cac action dang bat cua role va chuyen thanh authority ACTION_xxx.
        List<SimpleGrantedAuthority> actionAuthorities = roleActionRepository
                .findByRoleRoleIdAndIsEnabledTrue(user.getRole().getRoleId())
                .stream()
                // Bo qua action bi null de tranh loi khi map authority.
                .filter(roleAction -> roleAction.getAction() != null && roleAction.getAction().getActionCode() != null)
                .map(roleAction -> new SimpleGrantedAuthority(
                        "ACTION_" + roleAction.getAction().getActionCode()))
                .toList();

        // Gom role chinh va cac role mapping bo sung neu can.
        List<SimpleGrantedAuthority> mappedAuthorities = new java.util.ArrayList<>();
        mappedAuthorities.add(roleAuthority);
        // Gan them ROLE_ADMIN cho Owner de tuong thich cac man hinh/logic cu neu co
        // check ADMIN.
        if ("ROLE_OWNER".equals(normRole)) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        // Hop nhat role authority va action authority thanh bo quyen cuoi cung.
        List<SimpleGrantedAuthority> allAuthorities = Stream
                .concat(mappedAuthorities.stream(), actionAuthorities.stream())
                .toList();

        // Tra principal rieng cua he thong de Spring Security dung khi tao
        // Authentication.
        return new CustomUserDetails(user, allAuthorities);
    }

    // Chuan hoa role tu database: OWNER -> ROLE_OWNER, ROLE_OWNER giu nguyen.
    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
