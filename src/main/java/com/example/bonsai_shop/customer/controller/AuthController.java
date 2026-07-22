package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor

public class AuthController {

    private final UserService userService;

    // ===== TRANG LOGIN =====
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error, String logout,
            Model model) {

        if (error != null) {
            model.addAttribute(
                    "error1",
                    "Sai tài khoản, mật khẩu hoặc tài khoản chưa kích hoạt");
        }

        if(logout != null){
            model.addAttribute("success",  "Đăng xuất thành công!");
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
            userService.register(fullName, username, email, password, phone);
            model.addAttribute("email", email); // ← truyền email sang verify-otp
            model.addAttribute("success", "Mã OTP đã được gửi đến " + email);
            return "customer/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "customer/register";
        }
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otpCode,
                            Model model) {
        try {
            userService.verifyOtp(email, otpCode);
            userService.activateUser(email); // ← kích hoạt tài khoản
            model.addAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "customer/login";
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