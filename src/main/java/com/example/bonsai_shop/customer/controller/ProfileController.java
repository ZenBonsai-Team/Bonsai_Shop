package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.entity.CommunityPost;
import java.util.List;

import com.example.bonsai_shop.customer.repository.CommunityPostBookmarkRepository;
import com.example.bonsai_shop.entity.CommunityPostBookmark;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostBookmarkRepository bookmarkRepository;

    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // Lấy email của người đang đăng nhập
        String email = userDetails.getUsername();

        // Lấy thông tin đầy đủ từ database
        User user = userService.getCurrentUserProfile(email);

        List<CommunityPost> myBonsaiPosts = communityPostRepository.findByAuthorIdOrderByCreatedAtDesc(user.getUserId());

        // Lấy các bài viết đã lưu (saved / bookmarked)
        List<CommunityPostBookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
        List<Integer> savedPostIds = bookmarks.stream().map(CommunityPostBookmark::getPostId).collect(Collectors.toList());
        List<CommunityPost> savedPosts = savedPostIds.isEmpty() ? List.of() : communityPostRepository.findAllById(savedPostIds);

        model.addAttribute("user", user);
        model.addAttribute("myBonsaiPosts", myBonsaiPosts);
        model.addAttribute("savedPosts", savedPosts);
        return "customer/profile"; // templates/customer/profile.html
    }

    @GetMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
            String email = userDetails.getUsername();
            User user = userService.getCurrentUserProfile(email);
            model.addAttribute("user", user);
            return "customer/profile_update";
    }

    @PostMapping("/profile/update")
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
            User user = userService.getCurrentUserProfile(email);
            model.addAttribute("user", user);
            model.addAttribute("success", "Cập nhật thông tin thành công!");
        } catch (RuntimeException e) {
            User user = userService.getCurrentUserProfile(email);
            model.addAttribute("user", user);
            model.addAttribute("error", e.getMessage());
        }
        return "customer/profile";
    }


}