package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ChangePasswordController {

    private final UserService userService;

    @GetMapping("/profile/change-password")
    public String changePasswordPage(@AuthenticationPrincipal Object principal, Model model) {
        String email = extractEmail(principal);
        if (email == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", userService.getCurrentUserProfile(email));
        return "customer/change-password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(
            @AuthenticationPrincipal Object principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        String email = extractEmail(principal);
        if (email == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", userService.getCurrentUserProfile(email));

        try {
            userService.changePassword(
                    email,
                    currentPassword,
                    newPassword,
                    confirmPassword
            );

            model.addAttribute("success", "Đổi mật khẩu thành công!");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "customer/change-password";
    }

    private String extractEmail(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }
        return null;
    }
}
