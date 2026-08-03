package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.service.CustomUserDetailsService;
import com.example.bonsai_shop.customer.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
@RequiredArgsConstructor

public class AuthController {

    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    // ===== TRANG LOGIN =====
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            HttpServletRequest request,
            Model model) {

        if (error != null) {
            Object exception = request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            String errorMsg = "Sai tài khoản, mật khẩu hoặc tài khoản chưa kích hoạt";
            if (exception instanceof Exception ex) {
                errorMsg = ex.getMessage();
            }
            model.addAttribute("errorMsg", errorMsg);
        }

        if (logout != null) {
            model.addAttribute("success", "Đăng xuất thành công!");
        }

        return "customer/login";
    }

    // ===== TRANG REGISTER =====
    @GetMapping("/register")
    public String registerPage() {
        return "customer/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String phone,
                           Model model) {
        try {
            // Validate dữ liệu trước khi gọi service
            StringBuilder errors = new StringBuilder();
            
            if (fullName == null || fullName.trim().isEmpty()) {
                errors.append("Họ tên không được để trống. ");
            }
            if (username == null || username.trim().isEmpty()) {
                errors.append("Tên người dùng không được để trống. ");
            }
            if (email == null || email.trim().isEmpty()) {
                errors.append("Email không được để trống. ");
            } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                errors.append("Email không hợp lệ. ");
            }
            if (password == null || password.isEmpty()) {
                errors.append("Mật khẩu không được để trống. ");
            } else if (password.length() < 3) {
                errors.append("Mật khẩu phải có ít nhất 6 ký tự. ");
            }
            
            if (errors.length() > 0) {
                model.addAttribute("error", errors.toString());
                model.addAttribute("formData", new java.util.HashMap<String, String>() {{
                    put("fullName", fullName);
                    put("username", username);
                    put("email", email);
                    put("phone", phone);
                }});
                return "customer/register";
            }
            
            userService.register(fullName, username, email, password, phone);
            model.addAttribute("email", email);
            model.addAttribute("success", "Mã OTP đã được gửi đến " + email);
            return "customer/verify-otp";
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            
            // Giữ lại dữ liệu đã nhập đúng
            model.addAttribute("error", errorMsg);
            model.addAttribute("formData", new java.util.HashMap<String, String>() {{
                put("fullName", fullName);
                put("username", username);
                put("email", email);
                put("phone", phone);
            }});
            return "customer/register";
        }
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otpCode,
                            HttpServletRequest request,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            userService.verifyOtp(email, otpCode);
            userService.activateUser(email); // ← kích hoạt tài khoản
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
            // Tạo Authentication token
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // Gán vào SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Lưu SecurityContext vào session để Spring Security nhớ
            HttpSessionSecurityContextRepository securityContextRepository =
                    new HttpSessionSecurityContextRepository();
            securityContextRepository.saveContext(
                    SecurityContextHolder.getContext(), request,
                    Objects.requireNonNull(((ServletRequestAttributes) RequestContextHolder
                            .currentRequestAttributes()).getResponse())
            );
            // ===== KẾT THÚC TỰ ĐỘNG ĐĂNG NHẬP =====

//            // Sử dụng Flash Attribute để truyền message qua redirect
            redirectAttributes.addFlashAttribute("registrationSuccess", "Đăng ký tài khoản thành công!");

            return "redirect:/home"; // nhảy thẳng về trang chủ
        } catch (RuntimeException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "customer/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, Model model) {
        try {
            userService.sendOtp(email);
            model.addAttribute("email", email);
            model.addAttribute("success", "Mã OTP mới đã được gửi đến " + email);
            return "customer/verify-otp";
        } catch (RuntimeException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "customer/verify-otp";
        }
    }



}