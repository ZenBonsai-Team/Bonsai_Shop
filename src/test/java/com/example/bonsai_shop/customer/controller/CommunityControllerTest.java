package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.repository.*;
import com.example.bonsai_shop.customer.service.FileStorageService;
import com.example.bonsai_shop.customer.service.ProfanityFilterService;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommunityControllerTest {

    @Mock private CommunityPostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommunityCommentRepository commentRepository;
    @Mock private CommunityPostLikeRepository likeRepository;
    @Mock private ModerationNotificationRepository notificationRepository;
    @Mock private CloudinaryStorageService cloudinaryStorageService;
    @Mock private FileStorageService fileStorageService;
    @Mock private CommunityPostBookmarkRepository bookmarkRepository;
    @Mock private ProfanityFilterService profanityFilterService;

    @InjectMocks
    private CommunityController communityController;

    private User testUser;
    private UserDetails mockUserDetails;
    private CommunityPost samplePost;

    @BeforeEach
    public void setUp() {
        testUser = User.builder()
                .userId(1)
                .email("test@gmail.com")
                .username("testuser")
                .fullName("Test User")
                .avatar("avatar_url")
                .status("ACTIVE")
                .build();

        mockUserDetails = mock(UserDetails.class);
        lenient().when(mockUserDetails.getUsername()).thenReturn("test@gmail.com");

        samplePost = CommunityPost.builder()
                .postId(1)
                .title("Bài viết test")
                .content("Nội dung bài viết")
                .category("Tất cả")
                .status("APPROVED")
                .authorId(1)
                .authorName("Test User")
                .likesCount(0)
                .commentsCount(0)
                .build();
    }

    // ==========================================
    // 1. community() Endpoints Tests
    // ==========================================

    @Test
    public void testCommunity_Default() {
        // TC-UNIT-Community-001
        Model model = new ConcurrentModel();
        when(postRepository.findAllByStatusOrderByCreatedAtDesc("APPROVED")).thenReturn(List.of(samplePost));
        when(userRepository.findFeaturedArtisans()).thenReturn(Collections.emptyList());

        String view = communityController.community(model, null, null, mockUserDetails);

        assertEquals("customer/community", view);
        assertTrue(model.containsAttribute("posts"));
        assertEquals("Tất cả", model.getAttribute("selectedCategory"));
    }

    @Test
    public void testCommunity_CategoryBookmarked_LoggedIn() {
        // TC-UNIT-Community-002
        Model model = new ConcurrentModel();
        CommunityPostBookmark bookmark = CommunityPostBookmark.builder().postId(1).userId(1).build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(1)).thenReturn(List.of(bookmark));
        when(postRepository.findAllById(List.of(1))).thenReturn(List.of(samplePost));

        String view = communityController.community(model, "Đã lưu", null, mockUserDetails);

        assertEquals("customer/community", view);
        List<CommunityPost> posts = (List<CommunityPost>) model.getAttribute("posts");
        assertNotNull(posts);
        assertEquals(1, posts.size());
    }

    @Test
    public void testCommunity_CategoryFiltered_SearchByHashtag() {
        // TC-UNIT-Community-003
        Model model = new ConcurrentModel();
        when(postRepository.searchPostsByCategorySmart(eq("Cây cảnh"), eq("Tung"), eq("Tung"), eq("#Tung"), eq("Tung")))
                .thenReturn(List.of(samplePost));

        String view = communityController.community(model, "Cây cảnh", "#Tung", mockUserDetails);

        assertEquals("customer/community", view);
        assertEquals("#Tung", model.getAttribute("searchQuery"));
    }

    @Test
    public void testCommunity_AnonymousUser() {
        // TC-UNIT-Community-004
        Model model = new ConcurrentModel();
        when(postRepository.findAllByStatusOrderByCreatedAtDesc("APPROVED")).thenReturn(List.of(samplePost));

        String view = communityController.community(model, null, null, "anonymousUser");

        assertEquals("customer/community", view);
        assertNull(model.getAttribute("currentUser"));
    }

    // ==========================================
    // 2. viewAuthorProfile() Endpoints Tests
    // ==========================================

    @Test
    public void testViewAuthorProfile_FindById() {
        // TC-UNIT-Community-005
        Model model = new ConcurrentModel();
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(1, "APPROVED")).thenReturn(List.of(samplePost));

        String view = communityController.viewAuthorProfile("1", model, mockUserDetails);

        assertEquals("customer/author-profile", view);
        assertEquals(testUser, model.getAttribute("author"));
    }

    @Test
    public void testViewAuthorProfile_FindByName() {
        // TC-UNIT-Community-006
        Model model = new ConcurrentModel();
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(postRepository.findByAuthorNameAndStatusOrderByCreatedAtDesc("Test User", "APPROVED")).thenReturn(List.of(samplePost));

        String view = communityController.viewAuthorProfile("Test User", model, mockUserDetails);

        assertEquals("customer/author-profile", view);
        assertEquals(testUser, model.getAttribute("author"));
    }

    @Test
    public void testViewAuthorProfile_NotFound() {
        // TC-UNIT-Community-007
        Model model = new ConcurrentModel();
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        String view = communityController.viewAuthorProfile("UnknownUser", model, mockUserDetails);

        assertEquals("customer/author-profile", view);
        User fallbackAuthor = (User) model.getAttribute("author");
        assertNotNull(fallbackAuthor);
        assertEquals("UnknownUser", fallbackAuthor.getFullName());
    }

    // ==========================================
    // 3. viewPost() Endpoints Tests
    // ==========================================

    @Test
    public void testViewPost_Approved() {
        // TC-UNIT-Community-008
        Model model = new ConcurrentModel();
        when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(1)).thenReturn(Collections.emptyList());

        String view = communityController.viewPost(1, model, mockUserDetails);

        assertEquals("customer/community-detail", view);
        assertEquals(samplePost, model.getAttribute("post"));
    }

    @Test
    public void testViewPost_Flagged_GuestDenied() {
        // TC-UNIT-Community-009
        samplePost.setStatus("FLAGGED");
        when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));

        assertThrows(RuntimeException.class, () -> {
            communityController.viewPost(1, new ConcurrentModel(), "anonymousUser");
        });
    }

    @Test
    public void testViewPost_Flagged_AuthorAllowed() {
        // TC-UNIT-Community-010
        samplePost.setStatus("FLAGGED");
        when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));

        Model model = new ConcurrentModel();
        String view = communityController.viewPost(1, model, mockUserDetails);

        assertEquals("customer/community-detail", view);
    }

    @Test
    public void testViewPost_Flagged_ModAllowed() {
        // TC-UNIT-Community-011
        samplePost.setStatus("FLAGGED");
        when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));

        // Mock moderator principal
        UserDetails modDetails = mock(UserDetails.class);
        lenient().when(modDetails.getUsername()).thenReturn("mod@gmail.com");
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CONTENT_MODERATOR"));
        doReturn(authorities).when(modDetails).getAuthorities();

        Model model = new ConcurrentModel();
        String view = communityController.viewPost(1, model, modDetails);

        assertEquals("customer/community-detail", view);
    }

    @Test
    public void testViewPost_NotFound() {
        // TC-UNIT-Community-012
        when(postRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            communityController.viewPost(999, new ConcurrentModel(), mockUserDetails);
        });
    }

    // ==========================================
    // 4. likePost() Endpoints Tests
    // ==========================================

    @Test
    public void testLikePost_NotLoggedIn() {
        // TC-UNIT-Community-013
        ResponseEntity<Map<String, Object>> response = communityController.likePost(1, "anonymousUser");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
    }

    @Test
    public void testLikePost_ToggleOn() {
        // TC-UNIT-Community-014
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));
        when(likeRepository.findByPostIdAndUserId(1, 1)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = communityController.likePost(1, mockUserDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertTrue((Boolean) response.getBody().get("liked"));
        assertEquals(1, response.getBody().get("likesCount"));
        verify(likeRepository, times(1)).save(any(CommunityPostLike.class));
    }

    @Test
    public void testLikePost_ToggleOff() {
        // TC-UNIT-Community-015
        samplePost.setLikesCount(1);
        CommunityPostLike like = CommunityPostLike.builder().id(5).postId(1).userId(1).build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));
        when(likeRepository.findByPostIdAndUserId(1, 1)).thenReturn(Optional.of(like));

        ResponseEntity<Map<String, Object>> response = communityController.likePost(1, mockUserDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertFalse((Boolean) response.getBody().get("liked"));
        assertEquals(0, response.getBody().get("likesCount"));
        verify(likeRepository, times(1)).delete(like);
    }

    // ==========================================
    // 5. showCreateForm() Endpoints Tests
    // ==========================================

    @Test
    public void testShowCreateForm_NotLoggedIn() {
        // TC-UNIT-Community-016
        String view = communityController.showCreateForm(new ConcurrentModel(), null, "anonymousUser");
        assertEquals("redirect:/login", view);
    }

    @Test
    public void testShowCreateForm_Success() {
        // TC-UNIT-Community-017
        String view = communityController.showCreateForm(new ConcurrentModel(), "Cây cảnh", mockUserDetails);
        assertEquals("customer/community-create", view);
    }

    // ==========================================
    // 6. createPost() Endpoints Tests
    // ==========================================

    @Test
    public void testCreatePost_Approved() {
        // TC-UNIT-Community-018
        CommunityPost post = new CommunityPost();
        post.setTitle("Cây cảnh");
        post.setContent("Cây sanh cổ thụ");

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(profanityFilterService.containsProfanity(anyString())).thenReturn(false);

        String view = communityController.createPost(post, mockFile, mockUserDetails);

        assertEquals("redirect:/community", view);
        assertEquals("APPROVED", post.getStatus());
        verify(postRepository, times(1)).save(post);
    }

    @Test
    public void testCreatePost_Flagged() {
        // TC-UNIT-Community-019
        CommunityPost post = new CommunityPost();
        post.setTitle("Cây đm");
        post.setContent("Bài viết thô tục");

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(profanityFilterService.containsProfanity("Cây đm")).thenReturn(true);
        when(profanityFilterService.maskProfanity("Cây đm")).thenReturn("Cây ***");
        when(profanityFilterService.maskProfanity("Bài viết thô tục")).thenReturn("Bài viết thô tục");

        String view = communityController.createPost(post, mockFile, mockUserDetails);

        assertEquals("redirect:/community", view);
        assertEquals("FLAGGED", post.getStatus());
        assertEquals("Cây ***", post.getTitle());
    }

    @Test
    public void testCreatePost_CloudinaryUploadException() throws Exception {
        // TC-UNIT-Community-020
        CommunityPost post = new CommunityPost();
        post.setTitle("Cây cảnh");
        post.setContent("Cây sanh cổ thụ");

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));

        // Mock Cloudinary fails
        when(cloudinaryStorageService.uploadImage(any(), any())).thenThrow(new RuntimeException("Cloudinary Error"));
        // Spring file storage fallback succeeds
        when(fileStorageService.storeAvatar(any())).thenReturn("/local/avatar.jpg");

        String view = communityController.createPost(post, mockFile, mockUserDetails);

        assertEquals("redirect:/community", view);
        assertEquals("/local/avatar.jpg", post.getImageUrl());
    }

    // ==========================================
    // 7. addComment() Endpoints Tests
    // ==========================================

    @Test
    public void testAddComment_NotLoggedIn() {
        // TC-UNIT-Community-021
        ResponseEntity<Map<String, Object>> response = communityController.addComment(1, new HashMap<>(), "anonymousUser");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testAddComment_EmptyContent() {
        // TC-UNIT-Community-022
        Map<String, String> body = new HashMap<>();
        body.put("content", "  ");

        ResponseEntity<Map<String, Object>> response = communityController.addComment(1, body, mockUserDetails);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testAddComment_TooLongContent() {
        // TC-UNIT-Community-023
        Map<String, String> body = new HashMap<>();
        body.put("content", "a".repeat(501));

        ResponseEntity<Map<String, Object>> response = communityController.addComment(1, body, mockUserDetails);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testAddComment_Flagged() {
        // TC-UNIT-Community-024
        Map<String, String> body = new HashMap<>();
        body.put("content", "bình luận đm");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));
        when(profanityFilterService.containsProfanity("bình luận đm")).thenReturn(true);
        when(profanityFilterService.maskProfanity("bình luận đm")).thenReturn("bình luận ***");

        ResponseEntity<Map<String, Object>> response = communityController.addComment(1, body, mockUserDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(commentRepository, times(1)).save(argThat(c -> "FLAGGED".equals(c.getStatus())));
    }

    // ==========================================
    // 8. bookmarkPost() Endpoints Tests
    // ==========================================

    @Test
    public void testBookmarkPost_NotLoggedIn() {
        // TC-UNIT-Community-025
        ResponseEntity<Map<String, Object>> response = communityController.bookmarkPost(1, "anonymousUser");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testBookmarkPost_ToggleOnOff() {
        // TC-UNIT-Community-026
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));
        when(bookmarkRepository.findByPostIdAndUserId(1, 1)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = communityController.bookmarkPost(1, mockUserDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertTrue((Boolean) response.getBody().get("bookmarked"));

        // Toggle Off
        CommunityPostBookmark bookmark = CommunityPostBookmark.builder().id(2).postId(1).userId(1).build();
        when(bookmarkRepository.findByPostIdAndUserId(1, 1)).thenReturn(Optional.of(bookmark));

        ResponseEntity<Map<String, Object>> responseOff = communityController.bookmarkPost(1, mockUserDetails);
        assertFalse((Boolean) responseOff.getBody().get("bookmarked"));
        verify(bookmarkRepository, times(1)).delete(bookmark);
    }

    // ==========================================
    // 9. editPostForm() / editPost() Security Tests
    // ==========================================

    @Test
    public void testEditPostForm_NotAuthor() {
        // TC-UNIT-Community-027
        samplePost.setAuthorId(2); // Author is User 2
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser)); // Current User is User 1
        when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));

        String view = communityController.editPostForm(1, new ConcurrentModel(), mockUserDetails);

        assertEquals("redirect:/community", view);
    }
}
