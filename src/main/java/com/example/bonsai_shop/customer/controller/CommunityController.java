package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class CommunityController {

    private final CommunityPostRepository postRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String community(Model model,
                            @RequestParam(value = "category", required = false) String category,
                            @RequestParam(value = "search", required = false) String search,
                            @AuthenticationPrincipal UserDetails userDetails) {

        List<CommunityPost> posts;

        if (category != null && !category.trim().isEmpty() && !category.equals("Tất cả")) {
            if (search != null && !search.trim().isEmpty()) {
                posts = postRepository.searchPostsByCategory(category, search);
            } else {
                posts = postRepository.findByCategoryOrderByCreatedAtDesc(category);
            }
        } else {
            if (search != null && !search.trim().isEmpty()) {
                posts = postRepository.searchPosts(search);
            } else {
                posts = postRepository.findAllByOrderByCreatedAtDesc();
            }
        }

        // Add info to check if user is logged in (for UI controls)
        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
            });
        }

        model.addAttribute("posts", posts);
        model.addAttribute("selectedCategory", category != null ? category : "Tất cả");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("activePage", "community");

        return "customer/community";
    }

    @GetMapping("/post/{id}")
    public String viewPost(@PathVariable("id") Integer id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));

        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
            });
        }

        model.addAttribute("post", post);
        model.addAttribute("activePage", "community");
        return "customer/community-detail";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
            model.addAttribute("currentUser", user);
        });

        model.addAttribute("post", new CommunityPost());
        model.addAttribute("activePage", "community");
        return "customer/community-create";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute("post") CommunityPost post,
                             @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng chưa được xác thực"));

        // Set author info from logged in user
        post.setAuthorName(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : user.getUsername());
        
        String avatar = user.getAvatar();
        if (avatar == null || avatar.isEmpty()) {
            avatar = "https://ui-avatars.com/api/?name=" + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + "&background=random";
        }
        post.setAuthorAvatar(avatar);

        // Pre-fill metrics
        post.setCreatedAt(LocalDateTime.now());
        post.setLikesCount(0);
        post.setCommentsCount(0);
        
        if (post.getReadTime() == null || post.getReadTime() <= 0) {
            post.setReadTime(5); // default
        }

        if (post.getImageUrl() == null || post.getImageUrl().trim().isEmpty()) {
            // fallback generic beautiful bonsai image
            post.setImageUrl("https://images.unsplash.com/photo-1599599810769-bcde5a160d32?auto=format&fit=crop&q=80&w=800");
        }

        postRepository.save(post);
        return "redirect:/community";
    }
}
