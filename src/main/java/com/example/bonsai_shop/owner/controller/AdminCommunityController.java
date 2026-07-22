package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.customer.repository.CommunityCommentRepository;
import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.CommunityComment;
import com.example.bonsai_shop.entity.ModerationNotification;
import com.example.bonsai_shop.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/moderator/community")
@PreAuthorize("hasRole('CONTENT_MODERATOR')")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final ModerationNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String index(Model model,
                        @RequestParam(value = "tab", defaultValue = "posts") String activeTab,
                        @RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "status", required = false) String status,
                        @RequestParam(value = "postPage", defaultValue = "0") int postPage,
                        @RequestParam(value = "commentPage", defaultValue = "0") int commentPage) {
        
        Pageable postPageable = PageRequest.of(postPage, 10);
        Page<CommunityPost> postsPage;
        
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status);

        if (hasSearch && hasStatus) {
            postsPage = postRepository.findByStatusAndTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(status, search.trim(), search.trim(), postPageable);
        } else if (hasSearch) {
            postsPage = postRepository.findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(search.trim(), search.trim(), postPageable);
        } else if (hasStatus) {
            postsPage = postRepository.findByStatusOrderByCreatedAtDesc(status, postPageable);
        } else {
            postsPage = postRepository.findAllByOrderByCreatedAtDesc(postPageable);
        }
        
        Pageable commentPageable = PageRequest.of(commentPage, 10);
        Page<CommunityComment> commentsPage;

        if (hasSearch && hasStatus) {
            commentsPage = commentRepository.findByStatusAndContentContainingIgnoreCaseOrAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(status, search.trim(), search.trim(), commentPageable);
        } else if (hasSearch) {
            commentsPage = commentRepository.findByContentContainingIgnoreCaseOrAuthorNameContainingIgnoreCaseOrderByCreatedAtDesc(search.trim(), search.trim(), commentPageable);
        } else if (hasStatus) {
            commentsPage = commentRepository.findByStatusOrderByCreatedAtDesc(status, commentPageable);
        } else {
            commentsPage = commentRepository.findAllByOrderByCreatedAtDesc(commentPageable);
        }

        // Load all posts for lookup (for comments image + title)
        List<CommunityPost> allPosts = postRepository.findAll();
        Map<Integer, CommunityPost> postsMap = allPosts.stream()
                .collect(Collectors.toMap(CommunityPost::getPostId, post -> post, (a, b) -> a));

        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("comments", commentsPage.getContent());
        model.addAttribute("postsMap", postsMap);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("search", search != null ? search.trim() : "");
        model.addAttribute("statusFilter", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("role", "CONTENT MODERATOR");
        model.addAttribute("activeMenu", activeTab);
        model.addAttribute("paramActivePage", activeTab);
        
        // Pagination metadata
        model.addAttribute("postCurrentPage", postPage);
        model.addAttribute("postTotalPages", postsPage.getTotalPages());
        model.addAttribute("commentCurrentPage", commentPage);
        model.addAttribute("commentTotalPages", commentsPage.getTotalPages());

        return "owner/community_management";

    }

    @PostMapping("/comments/{id}/approve")
    public String approveComment(@PathVariable("id") Integer id,
                                RedirectAttributes redirectAttributes) {
        try {
            CommunityComment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận"));
            comment.setStatus("APPROVED");
            commentRepository.save(comment);

            // Gửi thông báo bằng email thực tế
            User user = userRepository.findById(comment.getUserId()).orElse(null);
            String email = user != null ? user.getEmail() : comment.getAuthorName();
            notificationRepository.save(ModerationNotification.builder()
                    .targetUsername(email)
                    .message("✅ Bình luận của bạn trên bài viết #" + comment.getPostId() + " đã được phê duyệt và hiển thị công khai.")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build());

            redirectAttributes.addFlashAttribute("success", "Đã duyệt bình luận thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/community?tab=comments";
    }

    @PostMapping("/posts/{id}/approve")
    public String approvePost(@PathVariable("id") Integer id,
                              RedirectAttributes redirectAttributes) {
        try {
            CommunityPost post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
            post.setStatus("APPROVED");
            postRepository.save(post);

            // Gửi thông báo bằng email thực tế
            User user = userRepository.findById(post.getAuthorId()).orElse(null);
            String email = user != null ? user.getEmail() : post.getAuthorName();
            notificationRepository.save(ModerationNotification.builder()
                    .targetUsername(email)
                    .message("✅ Bài viết '" + post.getTitle() + "' của bạn đã được phê duyệt và hiển thị công khai.")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build());

            redirectAttributes.addFlashAttribute("success", "Đã duyệt bài viết thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/community?tab=posts";
    }

    @PostMapping("/posts/{id}/toggle-status")
    public String togglePostStatus(@PathVariable("id") Integer id,
                                   @RequestParam(value = "reason", required = false) String reason,
                                   RedirectAttributes redirectAttributes) {
        try {
            CommunityPost post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
            
            User user = userRepository.findById(post.getAuthorId()).orElse(null);
            String email = user != null ? user.getEmail() : post.getAuthorName();

            if ("APPROVED".equals(post.getStatus())) {
                post.setStatus("HIDDEN");
                String reasonStr = (reason != null && !reason.trim().isEmpty()) ? reason : "Vi phạm quy chuẩn nội dung";
                notificationRepository.save(ModerationNotification.builder()
                        .targetUsername(email)
                        .message("Bài viết '" + post.getTitle() + "' của bạn đã bị tạm ẩn bởi Quản trị viên. Lý do: " + reasonStr)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build());
                redirectAttributes.addFlashAttribute("success", "Đã ẩn bài viết thành công!");
            } else {
                post.setStatus("APPROVED");
                String reasonStr = (reason != null && !reason.trim().isEmpty()) ? reason : "Đã được phê duyệt hiển thị lại";
                notificationRepository.save(ModerationNotification.builder()
                        .targetUsername(email)
                        .message("Bài viết '" + post.getTitle() + "' của bạn đã được hiển thị lại. Ghi chú: " + reasonStr)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build());
                redirectAttributes.addFlashAttribute("success", "Đã hiện bài viết thành công!");
            }
            postRepository.save(post);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/community?tab=posts";
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable("id") Integer id,
                             @RequestParam(value = "reason", required = false) String reason,
                             RedirectAttributes redirectAttributes) {
        try {
            CommunityPost post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
            
            User user = userRepository.findById(post.getAuthorId()).orElse(null);
            String email = user != null ? user.getEmail() : post.getAuthorName();

            String reasonStr = (reason != null && !reason.trim().isEmpty()) ? reason : "Vi phạm quy chuẩn nội dung";
            notificationRepository.save(ModerationNotification.builder()
                    .targetUsername(email)
                    .message("Bài viết '" + post.getTitle() + "' của bạn đã bị xóa vĩnh viễn bởi Quản trị viên. Lý do: " + reasonStr)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build());

            postRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa bài viết thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/community?tab=posts";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable("id") Integer id,
                                @RequestParam(value = "reason", required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            CommunityComment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận"));
            
            User user = userRepository.findById(comment.getUserId()).orElse(null);
            String email = user != null ? user.getEmail() : comment.getAuthorName();

            String reasonStr = (reason != null && !reason.trim().isEmpty()) ? reason : "Vi phạm quy chuẩn nội dung";
            
            String postTitle = "Bài viết #" + comment.getPostId();
            java.util.Optional<CommunityPost> postOpt = postRepository.findById(comment.getPostId());
            if (postOpt.isPresent()) {
                postTitle = postOpt.get().getTitle();
            }

            notificationRepository.save(ModerationNotification.builder()
                    .targetUsername(email)
                    .message("Bình luận của bạn tại bài viết '" + postTitle + "' đã bị xóa bởi Quản trị viên. Lý do: " + reasonStr)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build());

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
