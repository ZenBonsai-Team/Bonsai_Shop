package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

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

        // Tạo authorities từ role của user
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().getRoleName())
        );

        // Trả về OAuth2User với email làm nameAttributeKey
        return new DefaultOAuth2User(authorities, attributes, "email");
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
}