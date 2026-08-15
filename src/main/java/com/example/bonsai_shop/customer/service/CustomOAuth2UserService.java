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
import org.springframework.security.oauth2.core.OAuth2Error;
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
// Service xu ly thong tin user tra ve tu Google OAuth2 va chuyen thanh principal cua he thong.
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleActionRepository roleActionRepository;

    @Override
    @Transactional
    // Duoc Spring Security goi sau khi Google xac thuc thanh cong de nap/tao user noi bo.
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // Lấy thông tin từ Google
        // Goi DefaultOAuth2UserService de lay attributes tu Google userinfo endpoint.
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Rut cac thong tin can dung tu profile Google.
        String email    = (String) attributes.get("email");
        String fullName = (String) attributes.get("name");
        String avatar   = (String) attributes.get("picture");

        // Tìm hoặc tạo user trong database
        // Neu email Google chua ton tai thi tao customer moi trong database.
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewGoogleUser(email, fullName, avatar));

        // Luôn cập nhật avatar từ Google mỗi lần đăng nhập
        // trừ khi user đã tự upload avatar riêng (lưu trên Cloudinary)
        boolean hasCustomAvatar = user.getAvatar() != null
                && !user.getAvatar().contains("googleusercontent.com")
                && !user.getAvatar().isBlank();
        // Neu user chua upload avatar rieng thi dong bo avatar moi tu Google.
        if (!hasCustomAvatar && avatar != null) {
            user.setAvatar(avatar);
            userRepository.save(user);
        }

        // Kiểm tra trạng thái tài khoản
        // Chan dang nhap Google neu tai khoan da bi khoa trong he thong.
        if ("LOCKED".equals(user.getStatus())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_locked"), "Tài khoản đã bị khóa!");
        }

        // Chặn tài khoản quản trị/nhân viên đăng nhập bằng Google để đảm bảo bảo mật
        // Chi cho CUSTOMER dang nhap bang Google, cac role nhan vien phai dung form login.
        if (user.getRole() != null) {
            String roleName = user.getRole().getRoleName();
            if (roleName != null) {
                // Chuan hoa role de chap nhan ca CUSTOMER va ROLE_CUSTOMER.
                String normRole = roleName.trim().toUpperCase(Locale.ROOT);
                if (!"CUSTOMER".equals(normRole) && !"ROLE_CUSTOMER".equals(normRole)) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("role_not_allowed"), "Tài khoản của bạn không thể đăng nhập bằng phương thức này");
                }
            }
        }

        // Tạo authorities từ role của user
        // Build role/action authority giong form login de phan quyen thong nhat.
        List<SimpleGrantedAuthority> authorities = buildAuthorities(user);

        // Bo sung attributes noi bo de cac man hinh co the doc email/fullName/avatar/roleName tu principal.
        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("email", user.getEmail());
        enrichedAttributes.put("fullName", user.getFullName());
        enrichedAttributes.put("avatar", user.getAvatar());
        enrichedAttributes.put("roleName", user.getRole() != null ? user.getRole().getRoleName() : "");

        // Trả về OAuth2User với email làm nameAttributeKey
        // Tra principal rieng cua he thong, email la nameAttributeKey cho OAuth2User.
        return new CustomOAuth2User(user, authorities, enrichedAttributes, "email");
    }

    // Tao user moi cho lan dang nhap Google dau tien.
    private User createNewGoogleUser(String email, String fullName, String avatar) {
        // Gan role mac dinh ROLE_CUSTOMER cho tai khoan tao tu Google.
        Role roleUser = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role không tồn tại!"));

        // User Google khong can password noi bo va duoc ACTIVE vi Google da xac minh email.
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

    // Build danh sach authority cho user OAuth2 tu role va action trong database.
    private List<SimpleGrantedAuthority> buildAuthorities(User user) {
        // Validate user phai co role truoc khi tao principal.
        if (user.getRole() == null || user.getRole().getRoleName() == null) {
            throw new OAuth2AuthenticationException("Tai khoan chua duoc gan role!");
        }

        // Chuan hoa role ve dang ROLE_xxx.
        String normRole = normalizeRoleName(user.getRole().getRoleName());
        List<SimpleGrantedAuthority> mappedAuthorities = new ArrayList<>();
        mappedAuthorities.add(new SimpleGrantedAuthority(normRole));

        // Gan them ROLE_ADMIN cho Owner de tuong thich logic cu neu co.
        if ("ROLE_OWNER".equals(normRole)) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        // Lay cac action dang bat cua role va map thanh ACTION_xxx.
        List<SimpleGrantedAuthority> actionAuthorities = roleActionRepository
                .findByRoleRoleIdAndIsEnabledTrue(user.getRole().getRoleId())
                .stream()
                // Bo qua action null de tranh loi khi tao SimpleGrantedAuthority.
                .filter(roleAction -> roleAction.getAction() != null && roleAction.getAction().getActionCode() != null)
                .map(roleAction -> new SimpleGrantedAuthority(
                        "ACTION_" + roleAction.getAction().getActionCode()))
                .toList();

        // Hop nhat role authority va action authority.
        return Stream.concat(mappedAuthorities.stream(), actionAuthorities.stream()).toList();
    }

    // Chuan hoa role tu database: CUSTOMER -> ROLE_CUSTOMER, ROLE_CUSTOMER giu nguyen.
    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
