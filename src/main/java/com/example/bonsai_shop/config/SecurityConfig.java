package com.example.bonsai_shop.config;

import com.example.bonsai_shop.customer.service.CustomOAuth2UserService;
import com.example.bonsai_shop.customer.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final CustomUserDetailsService customUserDetailsService;
        private final CustomOAuth2UserService customOAuth2UserService;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/notifications/**", "/community/**"))
                                .userDetailsService(customUserDetailsService)
                                .authorizeHttpRequests(auth -> auth
                                                // Trang công khai
                                                .requestMatchers(
                                                                "/",
                                                                "/home",
                                                                "/contact",
                                                                "/about",
                                                                "/terms",
                                                                "/privacy",
                                                                "/error",
                                                                "/product/**",
                                                                "/marketplace",
                                                                "/register",
                                                                "/login",
                                                                "/forgot-password",
                                                                "/verify-otp",
                                                                "/resend-otp",
                                                                "/bonsai-luxury",
                                                                "/bonsai_luxury_detail/**",
                                                                "/reset-password",
                                                                "/verify-otp-to-reset-password",
                                                                "/resend-otp-reset",
                                                                "/vnpay/**", // ← cho phép thanh toán VNPAY
                                                                "/avatars/**", // ← cho phép xem ảnh avatar
                                                                "/community", // ← cho phép xem trang cộng đồng công khai
                                                                "/community/post/**", // ← cho phép xem bài viết chi tiết
                                                                "/css/**", // ← cho phép CSS
                                                                "/js/**", // ← cho phép JS
                                                                "/images/**", // ← cho phép images
                                                                "/api/orders", // ← tạm thời cho phép để test API chính
                                                                "/api/orders/**", // ← tạm thời cho phép để test API con
                                                                "/api/products/**", // ← cho phép lấy thông tin chi tiết sản phẩm
                                                                "/api/notifications/**" // ← cho phép lấy thông tin thông báo nút chuông
                                                ).permitAll()
                                                 // Chỉ OWNER mới vào được /owner/**
                                                 .requestMatchers("/owner/**").hasRole("OWNER")
                                                 // Chỉ CONTENT_MODERATOR mới vào được /moderator/community/**
                                                 .requestMatchers("/moderator/community/**").hasRole("CONTENT_MODERATOR")
                                                 // Chỉ MODERATOR mới vào được /moderator/**
                                                 .requestMatchers("/moderator/**").hasRole("MODERATOR")
                                                 // Chỉ ARTISAN mới vào được /artisan/**
                                                 .requestMatchers("/artisan/**").hasRole("ARTISAN")
                                                // Chặn theo Action cụ thể (permission-based)
                                                .requestMatchers("/products/create", "/products/edit/**",
                                                                "/prodcuts/delete/**")
                                                .hasAuthority("ACTION_PRODUCT_MANAGE")
                                                .requestMatchers("/orders/all")
                                                .hasAuthority("ACTION_ORDER_VIEW_ALL")
                                                .requestMatchers("/orders/handle-claim/**")
                                                .hasAuthority("ACTION_ORDER_HANDLE_CLAIM")
                                                .requestMatchers("/users/**")
                                                .hasAuthority("ACTION_USER_MANAGE")
                                                // Các trang khác cần đăng nhập
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login") // trang login tự tạo
                                                .loginProcessingUrl("/login") // URL xử lý form login
                                                .successHandler(roleBasedSuccessHandler())
                                                .failureUrl("/login?error") // login sai về trang này
                                                .permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")               // dùng chung trang login
                                                .successHandler(roleBasedSuccessHandler())
                                                .failureUrl("/login?error")
                                                .userInfoEndpoint(userInfo -> userInfo
                                                .userService(customOAuth2UserService)
                                )
                        )
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true) // xóa session
                                                .clearAuthentication(true) // xóa authentication
                                                .deleteCookies("JSESSIONID") // xóa cookie
                                                .permitAll());

                return http.build();
        }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isOwner = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority()));
            boolean isArtisan = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ARTISAN".equals(authority.getAuthority()));
            boolean isContentModerator = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_CONTENT_MODERATOR".equals(authority.getAuthority()));
            boolean isModerator = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority())
                            || "ACTION_ORDER_VIEW_ALL".equals(authority.getAuthority()));

            if (isOwner) {
                response.sendRedirect("/owner");
            } else if (isContentModerator) {
                response.sendRedirect("/moderator/community");
            } else if (isModerator) {
                response.sendRedirect("/moderator/orders");
            } else if (isArtisan) {
                response.sendRedirect("/artisan");
            } else {
                response.sendRedirect("/home");
            }
        };
    }
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder());
        return builder.build();
    }
}

