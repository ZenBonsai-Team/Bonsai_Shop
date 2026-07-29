package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.config.SecurityUtils;
import com.example.bonsai_shop.customer.repository.CommunityPostBookmarkRepository;
import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.CommunityPostBookmark;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    private String extractEmail(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof OAuth2User oAuth2User) {
            return  oAuth2User.getAttribute("email");
        }
        return null;
    }

    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal Object principal, Model model) {
        String email = extractEmail(principal);
        if (email == null) {
            return "redirect:/login";
        }

        User user = userService.getCurrentUserProfile(email);

        List<CommunityPost> myBonsaiPosts = communityPostRepository.findByAuthorIdOrderByCreatedAtDesc(user.getUserId());
        for (CommunityPost post : myBonsaiPosts) {
            post.setAuthorName(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : user.getUsername());
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                post.setAuthorAvatar(user.getAvatar());
            }
        }

        // Lấy các bài viết đã lưu (saved / bookmarked)
        List<CommunityPostBookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
        List<Integer> savedPostIds = bookmarks.stream().map(CommunityPostBookmark::getPostId).collect(Collectors.toList());
        List<CommunityPost> savedPosts = savedPostIds.isEmpty() ? List.of() : communityPostRepository.findAllById(savedPostIds);
        for (CommunityPost post : savedPosts) {
            if (post.getAuthorId() != null) {
                userRepository.findById(post.getAuthorId()).ifPresent(author -> {
                    post.setAuthorName(author.getFullName() != null && !author.getFullName().isEmpty() ? author.getFullName() : author.getUsername());
                    if (author.getAvatar() != null && !author.getAvatar().isEmpty()) {
                        post.setAuthorAvatar(author.getAvatar());
                    }
                });
            }
        }

        // Lấy lịch sử đơn hàng của người dùng
        List<Order> orders = orderService.getOrdersByCustomerId(user.getUserId());

        model.addAttribute("user", user);
        model.addAttribute("myBonsaiPosts", myBonsaiPosts);
        model.addAttribute("savedPosts", savedPosts);
        model.addAttribute("orders", orders);
        return "customer/profile"; // templates/customer/profile.html

    }

    @GetMapping("/orders")
    public String viewMyOrders() {
        return "redirect:/profile#orderHistorySection";
    }

    @GetMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal  Object principal, Model model) {
            String email = extractEmail(principal);
            if (email == null) return "redirect:/login";

            User user = userService.getCurrentUserProfile(email);
            model.addAttribute("user", user);
           return "customer/profile_update";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal Object principal,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String username,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                Model model) {
        String email = extractEmail(principal);
        if (email == null) return "redirect:/login";

        try {
            userService.updateUserProfile(email, fullName, username, phone, address, avatarFile);
            User user = userService.getCurrentUserProfile(email);
            SecurityUtils.updateSecurityContext(user);
            model.addAttribute("user", user);
            model.addAttribute("success", "Cập nhật thông tin thành công!");
        } catch (RuntimeException e) {
            User user = userService.getCurrentUserProfile(email);
            model.addAttribute("user", user);
            model.addAttribute("error", e.getMessage());
        }
        return "customer/profile_update";
    }

}
