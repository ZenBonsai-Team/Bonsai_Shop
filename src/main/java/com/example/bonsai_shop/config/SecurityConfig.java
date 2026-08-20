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
// Cau hinh bao mat trung tam: dang nhap form, dang nhap Google OAuth2, logout va phan quyen URL/method.
public class SecurityConfig {

        private final CustomUserDetailsService customUserDetailsService;
        private final CustomOAuth2UserService customOAuth2UserService;

        @Bean
        // Bean ma hoa mat khau bang BCrypt, dung khi dang ky va khi Spring Security kiem tra form login.
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        // Xay dung filter chain chinh de Spring Security xu ly request, authentication va authorization.
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // Tat CSRF cho cac API dang goi bang JavaScript/REST hoac endpoint realtime khong gui CSRF token.
                                .csrf(csrf -> csrf.ignoringRequestMatchers(
                                                "/api/notifications/**", 
                                                "/api/wishlist/**", 
                                                "/community/**", 
                                                "/moderator/orders/api/**",
                                                "/api/live/**",
                                                "/api/reviews/**"
                                ))
                                // Gan CustomUserDetailsService de form login lay user/role/action tu database.
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
                                                                "/products/detail",
                                                                "/products/detail/**",
                                                                "/marketplace",
                                                                "/register",
                                                                "/login",
                                                                "/forgot-password",
                                                                "/verify-otp",
                                                                "/resend-otp",
                                                                "/bonsai-luxury",
                                                                "/bonsai-luxury-detail/**",
                                                                "/reset-password",
                                                                "/verify-otp-to-reset-password",
                                                                "/resend-otp-reset",
                                                                "/vnpay/**", // ← cho phép thanh toán VNPAY
                                                                "/avatars/**", // ← cho phép xem ảnh avatar
                                                                "/community", // ← cho phép xem trang cộng đồng công
                                                                              // khai
                                                                "/community/post/**", // ← cho phép xem bài viết chi
                                                                                      // tiết
                                                                "/css/**", // ← cho phép CSS
                                                                "/js/**", // ← cho phép JS
                                                                "/images/**", // ← cho phép images
                                                                "/lookup", // ← cho phép truy cập /lookup
                                                                "/order/lookup", // ← cho phép khách tra cứu đơn hàng theo mã
                                                                "/orders/lookup", // ← cho phép truy cập /orders/lookup
                                                                "/cart", // ← cho phép khách xem giỏ hàng
                                                                "/checkout", // ← cho phép khách vào trang checkout
                                                                "/order/success", // ← cho phép khách xem thông báo thành công
                                                                "/wishlist",
                                                                "/api/cart/**",
                                                                "/api/wishlist/**",
                                                                "/api/products/**",
                                                                "/api/orders", // ← tạm thời cho phép để test API chính
                                                                "/api/orders/**", // ← tạm thời cho phép để test API con
                                                                "/api/notifications/**",
                                                                "/ws-live-chat/**",
                                                                "/api/live/**",
                                                                "/api/reviews/**",
                                                                "/live" // ← trang xem live cho khách hàng
                                                ).permitAll()
                                                // Chỉ OWNER mới vào được /owner và /owner/**
                                                // Chi tai khoan co ROLE_OWNER moi duoc vao khu vuc Owner.
                                                .requestMatchers("/owner", "/owner/**").hasRole("OWNER")
                                                // Chỉ CONTENT_MODERATOR mới vào được /moderator/community và /moderator/community/**
                                                // Chi Content Moderator moi duoc vao cac man hinh kiem duyet noi dung.
                                                .requestMatchers("/moderator/community", "/moderator/community/**").hasRole("CONTENT_MODERATOR")
                                                .requestMatchers("/moderator/live-session", "/moderator/live-session/**").hasRole("CONTENT_MODERATOR")
                                                .requestMatchers("/moderator/reviews", "/moderator/reviews/**").hasRole("CONTENT_MODERATOR")
                                                // Chỉ MODERATOR mới vào được /moderator và /moderator/**
                                                // Moderator xu ly don duoc vao khu vuc /moderator con lai.
                                                .requestMatchers("/moderator", "/moderator/**").hasRole("MODERATOR")
                                                // Chỉ ARTISAN mới vào được /artisan và /artisan/**
                                                // Artisan duoc vao khu vuc quan ly san pham/lich cua nghe nhan.
                                                .requestMatchers("/artisan", "/artisan/**").hasRole("ARTISAN")
                                                // Chặn theo Action cụ thể (permission-based)
                                                // Cac matcher ben duoi dung permission/action rieng thay vi chi dua vao role.
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
                                // Cau hinh dang nhap bang email/password qua form /login.
                                .formLogin(form -> form
                                                .loginPage("/login") // trang login tự tạo
                                                .loginProcessingUrl("/login") // URL xử lý form login
                                                .successHandler(roleBasedSuccessHandler())
                                                .failureUrl("/login?error") // login sai về trang này
                                                .permitAll())
                                // Cau hinh dang nhap bang Google OAuth2, dung chung trang login va success handler.
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login") // dùng chung trang login
                                                .successHandler(roleBasedSuccessHandler())
                                                .failureUrl("/login?error")
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                // Custom service chuyen profile Google thanh User trong database.
                                                                .userService(customOAuth2UserService)))
                                // Cau hinh dang xuat va don dep session/cookie dang nhap.
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
        // Dieu huong sau dang nhap thanh cong dua tren role/action cua tai khoan.
        public AuthenticationSuccessHandler roleBasedSuccessHandler() {
                return (request, response, authentication) -> {
                        // Kiem tra cac role/action hien co trong Authentication de quyet dinh trang dich.
                        boolean isOwner = authentication.getAuthorities().stream()
                                        .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority()));
                        boolean isArtisan = authentication.getAuthorities().stream()
                                        .anyMatch(authority -> "ROLE_ARTISAN".equals(authority.getAuthority()));
                        boolean isContentModerator = authentication.getAuthorities().stream()
                                        .anyMatch(authority -> "ROLE_CONTENT_MODERATOR"
                                                        .equals(authority.getAuthority()));
                        boolean isModerator = authentication.getAuthorities().stream()
                                        .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority())
                                                        || "ACTION_ORDER_VIEW_ALL".equals(authority.getAuthority()));

                        // Uu tien Owner truoc vi day la nhom quyen cao nhat trong he thong.
                        if (isOwner) {
                                response.sendRedirect("/owner");
                        } else if (isContentModerator) {
                                // Content Moderator vao thang man hinh kiem duyet cong dong.
                                response.sendRedirect("/moderator/community");
                        } else if (isModerator) {
                                // Moderator xu ly don hang vao dashboard don.
                                response.sendRedirect("/moderator/orders");
                        } else if (isArtisan) {
                                // Artisan vao thang trang quan ly san pham.
                                response.sendRedirect("/artisan/products");
                        } else {
                                // Customer hoac role mac dinh ve trang chu.
                                response.sendRedirect("/home");
                        }
                };
        }

        @Bean
        // AuthenticationManager dung CustomUserDetailsService va PasswordEncoder de xu ly form login.
        public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
                // Gan service load user va BCrypt encoder cho provider dang nhap bang mat khau.
                builder.userDetailsService(customUserDetailsService)
                                .passwordEncoder(passwordEncoder());
                return builder.build();
        }
}
