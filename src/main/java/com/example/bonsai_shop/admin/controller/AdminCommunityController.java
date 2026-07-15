package com.example.bonsai_shop.admin.controller;

import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.customer.repository.CommunityCommentRepository;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.CommunityComment;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/moderator/community")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;

    @GetMapping
    public String index(Model model, @RequestParam(value = "tab", defaultValue = "posts") String activeTab) {
        List<CommunityPost> posts = postRepository.findAllByOrderByCreatedAtDesc();
        List<CommunityComment> comments = commentRepository.findAllByOrderByCreatedAtDesc();

        model.addAttribute("posts", posts);
        model.addAttribute("comments", comments);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("activeMenu", "admin-community");

        return "admin/community_management";
    }

    @PostMapping("/posts/{id}/toggle-status")
    public String togglePostStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            CommunityPost post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
            
            if ("APPROVED".equals(post.getStatus())) {
                post.setStatus("HIDDEN");
                redirectAttributes.addFlashAttribute("success", "Đã ẩn bài viết thành công!");
            } else {
                post.setStatus("APPROVED");
                redirectAttributes.addFlashAttribute("success", "Đã hiện bài viết thành công!");
            }
            postRepository.save(post);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/community?tab=posts";
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            postRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa bài viết thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/community?tab=posts";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            CommunityComment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận"));
            
            // Cập nhật CommentsCount cho Post
            postRepository.findById(comment.getPostId()).ifPresent(post -> {
                int newCount = Math.max(0, post.getCommentsCount() - 1);
                post.setCommentsCount(newCount);
                postRepository.save(post);
            });

            commentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa bình luận thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/community?tab=comments";
    }
}
