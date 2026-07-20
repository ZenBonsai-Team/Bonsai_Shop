package com.example.bonsai_shop.customer.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.bonsai_shop.customer.repository.CommunityCommentRepository;
import com.example.bonsai_shop.customer.repository.CommunityPostLikeRepository;
import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.CommunityComment;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.CommunityPostLike;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ModerationNotification;

import com.example.bonsai_shop.customer.service.FileStorageService;
import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import org.springframework.web.multipart.MultipartFile;

import com.example.bonsai_shop.customer.repository.CommunityPostBookmarkRepository;
import com.example.bonsai_shop.entity.CommunityPostBookmark;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class CommunityController {

    private final CommunityPostRepository postRepository;
    private final UserRepository userRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityPostLikeRepository likeRepository;
    private final ModerationNotificationRepository notificationRepository;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final FileStorageService fileStorageService;
    private final CommunityPostBookmarkRepository bookmarkRepository;

    @GetMapping
    public String community(Model model,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<CommunityPost> posts = new java.util.ArrayList<>();

        if (category != null && category.trim().equals("Đã lưu")) {
            if (userDetails != null) {
                User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
                if (user != null) {
                    List<CommunityPostBookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
                    List<Integer> postIds = bookmarks.stream().map(CommunityPostBookmark::getPostId).collect(java.util.stream.Collectors.toList());
                    if (!postIds.isEmpty()) {
                        posts = postRepository.findAllById(postIds).stream()
                                .filter(p -> "APPROVED".equals(p.getStatus()))
                                .collect(java.util.stream.Collectors.toList());
                    }
                }
            }
        } else if (category != null && !category.trim().isEmpty() && !category.equals("Tất cả")) {
            if (search != null && !search.trim().isEmpty()) {
                posts = postRepository.searchPostsByCategory(category, search);
            } else {
                posts = postRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "APPROVED");
            }
        } else {
            if (search != null && !search.trim().isEmpty()) {
                posts = postRepository.searchPosts(search);
            } else {
                posts = postRepository.findAllByStatusOrderByCreatedAtDesc("APPROVED");
            }
        }

        java.util.Set<Integer> likedPostIds = new java.util.HashSet<>();
        java.util.Set<Integer> bookmarkedPostIds = new java.util.HashSet<>();

        // Add info to check if user is logged in (for UI controls)
        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
                // Fetch notifications for current user
                List<ModerationNotification> notifications = notificationRepository.findByTargetUsernameOrderByCreatedAtDesc(user.getFullName());
                model.addAttribute("moderationNotifications", notifications);

                // Fetch user's liked and bookmarked post IDs for instant feed UI state
                likeRepository.findByUserId(user.getUserId()).forEach(l -> likedPostIds.add(l.getPostId()));
                bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId()).forEach(b -> bookmarkedPostIds.add(b.getPostId()));
            });
        }

        // Fetch dynamic Artisans from DB for Featured Artisans Widget (RoleID = 3 is ROLE_ARTISAN)
        // Ordered by total approved posts and sum of likes dynamically
        List<User> featuredArtisans = userRepository.findFeaturedArtisans();
        if (featuredArtisans.isEmpty()) {
            featuredArtisans = userRepository.findByRoleRoleId(3);
        }
        model.addAttribute("featuredArtisans", featuredArtisans);

        model.addAttribute("likedPostIds", likedPostIds);
        model.addAttribute("bookmarkedPostIds", bookmarkedPostIds);

        model.addAttribute("posts", posts);
        model.addAttribute("selectedCategory", category != null ? category : "Tất cả");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("activePage", "community");

        return "customer/community";
    }

    // ===== TRANG HỒ SƠ TÁC GIẢ BÀI VIẾT (PUBLIC AUTHOR PROFILE) =====
    @GetMapping("/author/{identifier}")
    public String viewAuthorProfile(@PathVariable("identifier") String identifier, Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        User author = null;
        Integer authorId = null;
        String authorName = identifier;

        // 1. Try parsing as integer user ID first
        try {
            authorId = Integer.parseInt(identifier);
            author = userRepository.findById(authorId).orElse(null);
            if (author != null) {
                authorName = author.getFullName();
            }
        } catch (NumberFormatException e) {
            // Identifier is a string name
        }

        // 2. If not found by ID, try searching by full name in user list
        if (author == null) {
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                if (u.getFullName() != null && (u.getFullName().equalsIgnoreCase(identifier) || u.getEmail().equalsIgnoreCase(identifier))) {
                    author = u;
                    authorId = u.getUserId();
                    authorName = u.getFullName();
                    break;
                }
            }
        }

        // 3. If still no User entity found, fallback to virtual author object
        if (author == null) {
            author = new User();
            author.setUserId(authorId != null ? authorId : 0);
            author.setFullName(authorName);
            author.setAddress("Việt Nam");
            author.setAvatar("https://ui-avatars.com/api/?name=" + java.net.URLEncoder.encode(authorName, java.nio.charset.StandardCharsets.UTF_8));
        }

        // 4. Fetch posts by authorId or by authorName
        List<CommunityPost> authorPosts = new java.util.ArrayList<>();
        if (authorId != null && authorId > 0) {
            authorPosts = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
        }
        if (authorPosts.isEmpty() && authorName != null) {
            authorPosts = postRepository.findByAuthorNameOrderByCreatedAtDesc(authorName);
        }

        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
            });
        }

        model.addAttribute("author", author);
        model.addAttribute("authorPosts", authorPosts);
        model.addAttribute("activePage", "community");
        return "customer/author-profile";
    }

    @GetMapping("/post/{id}")
    public String viewPost(@PathVariable("id") Integer id, Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));

        if (!"APPROVED".equals(post.getStatus())) {
            // Check if user is Admin or Moderator. Admins/Moderators can see hidden posts
            boolean isAdminOrMod = userDetails != null && userDetails.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority())
                            || "ROLE_CONTENT_MODERATOR".equals(a.getAuthority())
                            || "ROLE_MODERATOR".equals(a.getAuthority()));
            if (!isAdminOrMod) {
                throw new RuntimeException("Bài viết này đã bị ẩn bởi quản trị viên.");
            }
        }

        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
            });
        }

        // Lấy bình luận thực tế
        List<CommunityComment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(id);

        // Đồng bộ số lượng bình luận thực tế trong DB với post.commentsCount
        if (post.getCommentsCount() == null || post.getCommentsCount() != comments.size()) {
            post.setCommentsCount(comments.size());
            postRepository.save(post);
        }

        // Lấy 3 bài viết tương tự cùng danh mục
        List<CommunityPost> relatedPosts = postRepository.findTop3ByCategoryAndStatusAndPostIdNotOrderByCreatedAtDesc(
                post.getCategory(), "APPROVED", id);
        // Kiểm tra xem user hiện tại đã like / bookmark chưa
        boolean isLiked = false;
        boolean isBookmarked = false;
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null) {
                isLiked = likeRepository.findByPostIdAndUserId(id, user.getUserId()).isPresent();
                isBookmarked = bookmarkRepository.existsByPostIdAndUserId(id, user.getUserId());
            }
        }

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("relatedPosts", relatedPosts);
        model.addAttribute("isLiked", isLiked);
        model.addAttribute("isBookmarked", isBookmarked);
        model.addAttribute("activePage", "community");
        return "customer/community-detail";
    }

    // ===== THÍCH / BỎ THÍCH BÀI VIẾT (TOGGLE - AJAX API) =====
    @PostMapping("/post/{id}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likePost(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        // Phải đăng nhập mới like được
        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để thích bài viết.");
            return ResponseEntity.status(401).body(response);
        }
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        // Toggle like
        var existingLike = likeRepository.findByPostIdAndUserId(id, user.getUserId());
        boolean isNowLiked;
        if (existingLike.isPresent()) {
            // Đã like → bỏ like
            likeRepository.delete(existingLike.get());
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            isNowLiked = false;
        } else {
            // Chưa like → thêm like
            likeRepository.save(CommunityPostLike.builder()
                    .postId(id)
                    .userId(user.getUserId())
                    .build());
            post.setLikesCount(post.getLikesCount() + 1);
            isNowLiked = true;
        }
        postRepository.save(post);

        response.put("success", true);
        response.put("liked", isNowLiked);
        response.put("likesCount", post.getLikesCount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/create")
    public String showCreateForm(Model model, 
                                 @RequestParam(required = false) String category,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
            model.addAttribute("currentUser", user);
        });

        CommunityPost newPost = new CommunityPost();
        if (category != null && !category.trim().isEmpty()) {
            newPost.setCategory(category.trim());
        }
        model.addAttribute("post", newPost);
        model.addAttribute("activePage", "community");
        return "customer/community-create";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute("post") CommunityPost post,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng chưa được xác thực"));

        // Process uploaded image file if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                CloudinaryUploadResponse response = cloudinaryStorageService.uploadImage(imageFile, CloudinaryFolder.COMMUNITY);
                if (response != null && response.getUrl() != null) {
                    post.setImageUrl(response.getUrl());
                }
            } catch (Exception e) {
                try {
                    String localUrl = fileStorageService.storeAvatar(imageFile);
                    post.setImageUrl(localUrl);
                } catch (Exception ex) {
                    // Ignore, fallback to entered URL or default
                }
            }
        }

        // Set author info from logged in user
        post.setAuthorId(user.getUserId());
        post.setAuthorName(
                user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : user.getUsername());

        String avatar = user.getAvatar();
        if (avatar == null || avatar.isEmpty()) {
            avatar = "https://ui-avatars.com/api/?name="
                    + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + "&background=random";
        }
        post.setAuthorAvatar(avatar);

        // Pre-fill metrics
        post.setCreatedAt(LocalDateTime.now());
        post.setLikesCount(0);
        post.setCommentsCount(0);
        post.setStatus("APPROVED");

        if (post.getReadTime() == null || post.getReadTime() <= 0) {
            post.setReadTime(5); // default
        }

        if (post.getImageUrl() == null || post.getImageUrl().trim().isEmpty()) {
            // fallback generic beautiful bonsai image
            post.setImageUrl(
                    "https://images.unsplash.com/photo-1599599810769-bcde5a160d32?auto=format&fit=crop&q=80&w=800");
        }

        postRepository.save(post);
        return "redirect:/community";
    }

    // ===== THÊM BÌNH LUẬN (AJAX API) =====
    @PostMapping("/post/{id}/comment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable("id") Integer id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để bình luận.");
            return ResponseEntity.status(401).body(response);
        }

        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Nội dung bình luận không được trống.");
            return ResponseEntity.badRequest().body(response);
        }

        if (content.length() > 500) {
            response.put("success", false);
            response.put("message", "Bình luận tối đa 500 ký tự.");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng chưa được xác thực"));

        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));

        String avatar = user.getAvatar();
        if (avatar == null || avatar.isEmpty()) {
            avatar = "https://ui-avatars.com/api/?name="
                    + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + "&background=random";
        }

        String authorName = user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : user.getUsername();

        CommunityComment comment = CommunityComment.builder()
                .postId(id)
                .userId(user.getUserId())
                .authorName(authorName)
                .authorAvatar(avatar)
                .content(content.trim())
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);

        // Cập nhật số lượng bình luận trên Post
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        boolean isAuthor = (post.getAuthorId() != null && post.getAuthorId().equals(user.getUserId()))
                || (post.getAuthorName() != null && post.getAuthorName().equalsIgnoreCase(authorName));

        response.put("success", true);
        response.put("commentsCount", post.getCommentsCount());
        response.put("authorName", authorName);
        response.put("authorAvatar", avatar);
        response.put("content", comment.getContent());
        response.put("isAuthor", isAuthor);
        
        // Định dạng ngày hiển thị thân thiện
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        response.put("createdAt", comment.getCreatedAt().format(formatter));

        return ResponseEntity.ok(response);
    }

    // ===== LƯU BÀI VIẾT / BỎ LƯU BÀI VIẾT (TOGGLE - AJAX API) =====
    @PostMapping("/post/{id}/bookmark")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bookmarkPost(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để lưu bài viết.");
            return ResponseEntity.status(401).body(response);
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        var existingBookmark = bookmarkRepository.findByPostIdAndUserId(id, user.getUserId());
        boolean isBookmarked;
        if (existingBookmark.isPresent()) {
            bookmarkRepository.delete(existingBookmark.get());
            isBookmarked = false;
        } else {
            bookmarkRepository.save(CommunityPostBookmark.builder()
                    .postId(id)
                    .userId(user.getUserId())
                    .build());
            isBookmarked = true;
        }

        response.put("success", true);
        response.put("bookmarked", isBookmarked);
        response.put("message", isBookmarked ? "Đã lưu bài viết thành công!" : "Đã bỏ lưu bài viết!");
        return ResponseEntity.ok(response);
    }
}
