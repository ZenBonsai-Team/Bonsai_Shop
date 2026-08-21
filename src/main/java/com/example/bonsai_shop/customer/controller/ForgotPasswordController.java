package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;

    // ===== BƯỚC 1: Nhập email =====
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "customer/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendOtp(@RequestParam String email, Model model) {
        try {
            userService.sendOtpResetPassword(email);
            model.addAttribute("email", email);
            model.addAttribute("success", "Mã OTP đã được gửi đến " + email);
            return "customer/verify-otp-reset-password";
        } catch (RuntimeException e) {
            model.addAttribute("emailInput", email);
            model.addAttribute("error", e.getMessage());
            return "customer/forgot-password";
        }
    }

//     ===== BƯỚC 2: Nhập OTP =====

    @PostMapping("/verify-otp-to-reset-password")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otpCode,
                            Model model) {
        try {
            userService.verifyOtp(email, otpCode);
            model.addAttribute("email", email);
            return "customer/reset-password";
        } catch (RuntimeException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "customer/verify-otp-reset-password";
        }
    }
    @PostMapping("/resend-otp-reset")
    public String resendOtpReset(@RequestParam String email, Model model) {
        try {
            userService.sendOtp(email);
            model.addAttribute("email", email);
            model.addAttribute("success", "Mã OTP mới đã được gửi đến " + email);
            return "customer/verify-otp-reset-password";
        } catch (RuntimeException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "customer/verify-otp-reset-password";
        }
    }

    // ===== BƯỚC 3: Đặt lại mật khẩu =====
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "customer/reset-password";
        }
        if (newPassword.isBlank()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Mật khẩu không được để trống!");
            return "customer/reset-password";
        }
        if (newPassword.length() < 6 || newPassword.length() > 50) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Mật khẩu phải có từ 6 đến 50 ký tự!");
            return "customer/reset-password";
        }
        try {
            userService.resetPassword(email, newPassword);
            return "redirect:/login?resetSuccess=true";
        } catch (RuntimeException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "customer/reset-password";
        }
    }
}
