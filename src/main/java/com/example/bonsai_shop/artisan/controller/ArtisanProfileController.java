package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/artisan/profile")
@RequiredArgsConstructor
public class ArtisanProfileController {

    private final UserService userService;

    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User artisan = userService.getCurrentUserProfile(userDetails.getUsername());
        model.addAttribute("artisan", artisan);
        model.addAttribute("role", "ARTISAN");
        model.addAttribute("activeMenu", "artisan-profile");
        return "artisan/profile";
    }

    @GetMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User artisan = userService.getCurrentUserProfile(userDetails.getUsername());
        model.addAttribute("artisan", artisan);
        model.addAttribute("role", "ARTISAN");
        model.addAttribute("activeMenu", "artisan-profile");
        return "artisan/profile-update";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String username,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                Model model) {
        String email = userDetails.getUsername();
        try {
            userService.updateUserProfile(email, fullName, username, phone, address, avatarFile);
            return "redirect:/artisan/profile";
        } catch (RuntimeException e) {
            User artisan = userService.getCurrentUserProfile(email);
            model.addAttribute("artisan", artisan);
            model.addAttribute("role", "ARTISAN");
            model.addAttribute("activeMenu", "artisan-profile");
            model.addAttribute("error", e.getMessage());
            return "artisan/profile-update";
        }
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("role", "ARTISAN");
        model.addAttribute("activeMenu", "artisan-profile");
        return "artisan/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        model.addAttribute("role", "ARTISAN");
        model.addAttribute("activeMenu", "artisan-profile");
        try {
            userService.changePassword(
                    userDetails.getUsername(),
                    currentPassword,
                    newPassword,
                    confirmPassword
            );
            model.addAttribute("success", "Đổi mật khẩu thành công!");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "artisan/change-password";
    }
}
