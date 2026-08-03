package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.repository.RoleActionRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleActionRepository roleActionRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // Lấy thông tin từ Google
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email    = (String) attributes.get("email");
        String fullName = (String) attributes.get("name");
        String avatar   = (String) attributes.get("picture");

        // Tìm hoặc tạo user trong database
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewGoogleUser(email, fullName, avatar));

        // Nếu user cũ chưa có avatar thì cập nhật từ Google
        if (user.getAvatar() == null && avatar != null) {
            user.setAvatar(avatar);
            userRepository.save(user);
        }

        // Kiểm tra trạng thái tài khoản
        if ("LOCKED".equals(user.getStatus())) {
            throw new OAuth2AuthenticationException("Tài khoản đã bị khóa!");
        }

        // Chặn tài khoản quản trị/nhân viên đăng nhập bằng Google để đảm bảo bảo mật
        if (user.getRole() != null) {
            String roleName = user.getRole().getRoleName();
            if (roleName != null) {
                String normRole = roleName.trim().toUpperCase(Locale.ROOT);
                if (!"CUSTOMER".equals(normRole) && !"ROLE_CUSTOMER".equals(normRole)) {
                    throw new OAuth2AuthenticationException("Tài khoản quản trị/nhân viên bắt buộc phải đăng nhập bằng mật khẩu!");
                }
            }
        }

        // Tạo authorities từ role của user
        List<SimpleGrantedAuthority> authorities = buildAuthorities(user);

        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("email", user.getEmail());
        enrichedAttributes.put("fullName", user.getFullName());
        enrichedAttributes.put("avatar", user.getAvatar());
        enrichedAttributes.put("roleName", user.getRole() != null ? user.getRole().getRoleName() : "");

        // Trả về OAuth2User với email làm nameAttributeKey
        return new CustomOAuth2User(user, authorities, enrichedAttributes, "email");
    }

    private User createNewGoogleUser(String email, String fullName, String avatar) {
        Role roleUser = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role không tồn tại!"));

        User newUser = User.builder()
                .email(email)
                .fullName(fullName != null ? fullName : email)
                .avatar(avatar)
                .password("") // Google login không cần password
                .role(roleUser)
                .status("ACTIVE") // Google đã xác thực email rồi
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(newUser);
    }

    private List<SimpleGrantedAuthority> buildAuthorities(User user) {
        if (user.getRole() == null || user.getRole().getRoleName() == null) {
            throw new OAuth2AuthenticationException("Tai khoan chua duoc gan role!");
        }

        String normRole = normalizeRoleName(user.getRole().getRoleName());
        List<SimpleGrantedAuthority> mappedAuthorities = new ArrayList<>();
        mappedAuthorities.add(new SimpleGrantedAuthority(normRole));

        if ("ROLE_OWNER".equals(normRole)) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if ("ROLE_ARTISAN".equals(normRole)) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
        }

        List<SimpleGrantedAuthority> actionAuthorities = roleActionRepository
                .findByRoleRoleIdAndIsEnabledTrue(user.getRole().getRoleId())
                .stream()
                .filter(roleAction -> roleAction.getAction() != null && roleAction.getAction().getActionCode() != null)
                .map(roleAction -> new SimpleGrantedAuthority(
                        "ACTION_" + roleAction.getAction().getActionCode()))
                .toList();

        return Stream.concat(mappedAuthorities.stream(), actionAuthorities.stream()).toList();
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
