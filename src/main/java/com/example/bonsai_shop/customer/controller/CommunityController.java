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
import com.example.bonsai_shop.customer.service.CustomOAuth2User;

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
    private final com.example.bonsai_shop.customer.service.ProfanityFilterService profanityFilterService;

    private String getEmailFromPrincipal(Object principal) {
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUsername();
        }
        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            return ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
        }
        return null;
    }

    private boolean hasRole(Object principal, String... roles) {
        if (principal == null || "anonymousUser".equals(principal)) {
            return false;
        }
        java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = null;
        if (principal instanceof UserDetails) {
            authorities = ((UserDetails) principal).getAuthorities();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            authorities = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAuthorities();
        }
        if (authorities != null) {
            for (String role : roles) {
                if (authorities.stream().anyMatch(a -> role.equals(a.getAuthority()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @GetMapping
    public String community(Model model,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            @AuthenticationPrincipal Object principal) {

        List<CommunityPost> posts = new java.util.ArrayList<>();
        String currentEmail = getEmailFromPrincipal(principal);

        if (category != null && category.trim().equals("Đã lưu")) {
            if (currentEmail != null) {
                User user = userRepository.findByEmail(currentEmail).orElse(null);
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
        if (currentEmail != null) {
            userRepository.findByEmail(currentEmail).ifPresent(user -> {
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
        model.addAttribute("trendingHashtags", getTrendingHashtags());

        return "customer/community";
    }

    private List<String> getTrendingHashtags() {
        List<String> defaultTags = java.util.Arrays.asList(
                "TùngLaHán", "NghệThuậtUốnCây", "SanhNamĐiền", "BonsaiHảiHậu", "KỹThuậtChămSóc", "ChămSócMùaĐông"
        );

        try {
            org.springframework.data.domain.Page<CommunityPost> recentPostsPage = 
                    postRepository.findByStatusOrderByCreatedAtDesc("APPROVED", org.springframework.data.domain.PageRequest.of(0, 100));
            List<CommunityPost> recentPosts = recentPostsPage.getContent();

            if (recentPosts.isEmpty()) {
                return defaultTags;
            }

            java.util.Map<String, Integer> tagCounts = new java.util.HashMap<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#([\\p{L}\\p{N}]+)");

            for (CommunityPost post : recentPosts) {
                String text = (post.getTitle() != null ? post.getTitle() : "") + " " + (post.getContent() != null ? post.getContent() : "");
                java.util.regex.Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    String tag = matcher.group(1);
                    if (tag.length() >= 2) {
                        tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
                    }
                }
            }

            if (tagCounts.isEmpty()) {
                return defaultTags;
            }

            List<String> sortedTags = tagCounts.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .map(java.util.Map.Entry::getKey)
                    .limit(6)
                    .collect(java.util.stream.Collectors.toList());

            if (sortedTags.size() < 6) {
                for (String defaultTag : defaultTags) {
                    boolean exists = sortedTags.stream().anyMatch(t -> t.equalsIgnoreCase(defaultTag));
                    if (!exists) {
                        sortedTags.add(defaultTag);
                    }
                    if (sortedTags.size() == 6) {
                        break;
                    }
                }
            }

            return sortedTags;
        } catch (Exception e) {
            return defaultTags;
        }
    }

    // ===== TRANG HỒ SƠ TÁC GIẢ BÀI VIẾT (PUBLIC AUTHOR PROFILE) =====
    @GetMapping("/author/{identifier}")
    public String viewAuthorProfile(@PathVariable("identifier") String identifier, Model model,
            @AuthenticationPrincipal Object principal) {
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

        String currentEmail = getEmailFromPrincipal(principal);
        if (currentEmail != null) {
            userRepository.findByEmail(currentEmail).ifPresent(user -> {
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
            @AuthenticationPrincipal Object principal) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));

        if (!"APPROVED".equals(post.getStatus())) {
            // Check if user is Admin or Moderator. Admins/Moderators can see hidden posts
            boolean isAdminOrMod = hasRole(principal, "ROLE_OWNER", "ROLE_CONTENT_MODERATOR", "ROLE_MODERATOR");
            if (!isAdminOrMod) {
                throw new RuntimeException("Bài viết này đã bị ẩn bởi quản trị viên.");
            }
        }

        String currentEmail = getEmailFromPrincipal(principal);
        if (currentEmail != null) {
            userRepository.findByEmail(currentEmail).ifPresent(user -> {
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
        if (currentEmail != null) {
            User user = userRepository.findByEmail(currentEmail).orElse(null);
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
            @AuthenticationPrincipal Object principal) {
        Map<String, Object> response = new HashMap<>();
        String currentEmail = getEmailFromPrincipal(principal);
        // Phải đăng nhập mới like được
        if (currentEmail == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để thích bài viết.");
            return ResponseEntity.status(401).body(response);
        }
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findByEmail(currentEmail)
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
                                 @AuthenticationPrincipal Object principal) {
        String currentEmail = getEmailFromPrincipal(principal);
        if (currentEmail == null) {
            return "redirect:/login";
        }

        userRepository.findByEmail(currentEmail).ifPresent(user -> {
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
            @AuthenticationPrincipal Object principal) {
        String currentEmail = getEmailFromPrincipal(principal);
        if (currentEmail == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(currentEmail)
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

        boolean isFlagged = profanityFilterService.containsProfanity(post.getTitle()) || profanityFilterService.containsProfanity(post.getContent());
        if (isFlagged) {
            post.setTitle(profanityFilterService.maskProfanity(post.getTitle()));
            post.setContent(profanityFilterService.maskProfanity(post.getContent()));
            post.setStatus("FLAGGED");
            
            // 1. Thông báo cảnh báo cho Tác giả
            notificationRepository.save(ModerationNotification.builder()
                    .targetUsername(user.getEmail())
                    .message("⚠️ Bài viết '" + post.getTitle() + "' của bạn nghi vấn chứa từ ngữ vi phạm và đã chuyển vào Hàng chờ kiểm duyệt.")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build());

            // 2. Gửi thông báo cảnh báo chỉ cho các tài khoản CONTENT_MODERATOR
            List<User> moderators = userRepository.findByRoleRoleNameIn(List.of("ROLE_CONTENT_MODERATOR", "CONTENT_MODERATOR"));
            for (User mod : moderators) {
                notificationRepository.save(ModerationNotification.builder()
                        .targetUsername(mod.getEmail())
                        .message("🚨 Bài viết mới từ " + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + " nghi vấn chứa từ ngữ vi phạm cần kiểm duyệt.")
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        } else {
            post.setStatus("APPROVED");
        }

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
            @AuthenticationPrincipal Object principal) {
        
        Map<String, Object> response = new HashMap<>();
        String currentEmail = getEmailFromPrincipal(principal);
        
        if (currentEmail == null) {
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

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Người dùng chưa được xác thực"));

        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));

        String avatar = user.getAvatar();
        if (avatar == null || avatar.isEmpty()) {
            avatar = "https://ui-avatars.com/api/?name="
                    + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + "&background=random";
        }

        String authorName = user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : user.getUsername();

        boolean isFlagged = profanityFilterService.containsProfanity(content);
        String maskedContent = profanityFilterService.maskProfanity(content.trim());

        CommunityComment comment = CommunityComment.builder()
                .postId(id)
                .userId(user.getUserId())
                .authorName(authorName)
                .authorAvatar(avatar)
                .content(maskedContent)
                .status(isFlagged ? "FLAGGED" : "APPROVED")
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);

        if (isFlagged) {
            // 1. Thông báo cảnh báo cho người viết bình luận
            notificationRepository.save(ModerationNotification.builder()
                    .targetUsername(user.getEmail())
                    .message("⚠️ Bình luận của bạn trên bài viết #" + id + " nghi vấn chứa từ ngữ vi phạm và đã chuyển vào Hàng chờ kiểm duyệt.")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build());

            // 2. Gửi thông báo cảnh báo chỉ cho các tài khoản CONTENT_MODERATOR
            List<User> moderators = userRepository.findByRoleRoleNameIn(List.of("ROLE_CONTENT_MODERATOR", "CONTENT_MODERATOR"));
            for (User mod : moderators) {
                notificationRepository.save(ModerationNotification.builder()
                        .targetUsername(mod.getEmail())
                        .message("🚨 Bình luận mới từ " + authorName + " trên bài viết #" + id + " nghi vấn chứa từ ngữ vi phạm cần bạn kiểm duyệt.")
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        } else {
            // Nếu bình luận hợp lệ, gửi thông báo cho Tác giả bài viết (nếu người cmt không phải là tác giả)
            if (post.getAuthorId() != null && !post.getAuthorId().equals(user.getUserId())) {
                userRepository.findById(post.getAuthorId()).ifPresent(author -> {
                    notificationRepository.save(ModerationNotification.builder()
                            .targetUsername(author.getEmail())
                            .message("💬 " + authorName + " đã bình luận về bài viết #" + id + " của bạn.")
                            .isRead(false)
                            .createdAt(LocalDateTime.now())
                            .build());
                });
            }
        }

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
            @AuthenticationPrincipal Object principal) {
        Map<String, Object> response = new HashMap<>();
        String currentEmail = getEmailFromPrincipal(principal);
        if (currentEmail == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để lưu bài viết.");
            return ResponseEntity.status(401).body(response);
        }
        User user = userRepository.findByEmail(currentEmail)
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
