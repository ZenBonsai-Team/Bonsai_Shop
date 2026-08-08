package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.repository.CommunityPostLikeRepository;
import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.CommunityPostLike;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * L2 INTEGRATION TEST (Chiêu 1 & Chiêu 2) cho Community.
 * Tích hợp từ Controller -> Service -> Repository -> Database sử dụng Spring Boot context thực tế.
 */
@SpringBootTest
@Transactional // Đảm bảo rollback dữ liệu sau khi chạy để tránh ô nhiễm DB
public class CommunityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CommunityPostLikeRepository likeRepository;

    private User testUser;
    private CommunityPost testPost;

    @BeforeEach
    void setUp() {
        // Thiết lập MockMvc từ Spring context
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Lấy hoặc tạo vai trò bắt buộc
        Role role = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CUSTOMER").build()));

        // Gieo dữ liệu giả lập (Seed Data) trực tiếp vào Database thực
        testUser = User.builder()
                .email("anhpq@bsms.com")
                .fullName("Anh PQ")
                .username("anhpq")
                .password("$2a$10$dummyBCryptHashForPassword") // password không được null
                .role(role) // role không được null
                .status("ACTIVE")
                .build();
        testUser = userRepository.save(testUser);

        testPost = CommunityPost.builder()
                .title("Bonsai Care Guide")
                .content("Detailed guidelines for pruning your Bonsai tree.")
                .authorId(testUser.getUserId())
                .authorName(testUser.getFullName())
                .status("APPROVED")
                .likesCount(0)
                .commentsCount(0)
                .category("Kỹ thuật")
                .build();
        testPost = postRepository.save(testPost);
    }

    @Test
    @DisplayName("TC-INT-Community-001: Lấy danh sách bài viết cộng đồng (Layer & DB integration)")
    void testGetCommunityPage_ReturnsPostsFromDatabase() throws Exception {
        mockMvc.perform(get("/community"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/community"))
                .andExpect(model().attributeExists("posts"));
    }

    @Test
    @DisplayName("TC-INT-Community-003: Thực hiện thích bài viết và lưu vào Database (Toggle On)")
    @WithMockUser(username = "anhpq@bsms.com", roles = "CUSTOMER")
    void testLikePost_PersistsLikeInDatabase() throws Exception {
        // Thực hiện hành động gửi HTTP POST với CSRF token
        mockMvc.perform(post("/community/post/" + testPost.getPostId() + "/like").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likesCount").value(1));

        // Kiểm chứng DB State: Dữ liệu thực sự được lưu vào DB
        Optional<CommunityPostLike> optLike = likeRepository.findByPostIdAndUserId(testPost.getPostId(), testUser.getUserId());
        assertThat(optLike).isPresent();

        CommunityPost updatedPost = postRepository.findById(testPost.getPostId()).orElseThrow();
        assertThat(updatedPost.getLikesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-INT-Community-004: Tạo bài đăng lọc từ ngữ nhạy cảm và lưu trạng thái FLAGGED vào DB")
    @WithMockUser(username = "anhpq@bsms.com", roles = "CUSTOMER")
    void testCreatePost_WithProfanity_SavesAsFlaggedInDatabase() throws Exception {
        // Thực hiện hành động gửi form tạo bài đăng có từ nhạy cảm với CSRF token
        mockMvc.perform(post("/community/create").with(csrf())
                        .param("title", "Cây đm")
                        .param("content", "Nội dung bình thường")
                        .param("category", "Kỹ thuật"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/community"));

        // Kiểm chứng DB State: Kiểm tra xem bài viết được lưu ở trạng thái FLAGGED và tiêu đề bị ẩn không
        CommunityPost savedPost = postRepository.findAll().stream()
                .filter(p -> "Nội dung bình thường".equals(p.getContent()))
                .findFirst()
                .orElseThrow();

        assertThat(savedPost.getStatus()).isEqualTo("FLAGGED");
        assertThat(savedPost.getTitle()).isEqualTo("Cây ***");
    }
}
