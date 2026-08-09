package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * L3 SYSTEM TEST (HTTP Flow Testing - §3a) cho BF-04: Community Moderation & Auto-Approval.
 * Giả lập chuỗi yêu cầu mạng (multi-step HTTP requests) từ Customer và Content Moderator sử dụng MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BF04CommunityModerationSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModerationNotificationRepository notificationRepository;

    private User customerEntity;
    private User moderatorEntity;

    @BeforeEach
    void setUp() {
        // Thiết lập vai trò và người dùng giả lập
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CUSTOMER").build()));

        Role modRole = roleRepository.findByRoleName("CONTENT_MODERATOR")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CONTENT_MODERATOR").build()));

        customerEntity = userRepository.findByEmail("customer.bf04@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Test Customer")
                        .email("customer.bf04@test.com")
                        .username("customer_bf04")
                        .password("$2a$10$dummyHash")
                        .role(customerRole)
                        .status("ACTIVE")
                        .build()));

        moderatorEntity = userRepository.findByEmail("moderator.bf04@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Test Moderator")
                        .email("moderator.bf04@test.com")
                        .username("moderator_bf04")
                        .password("$2a$10$dummyHash")
                        .role(modRole)
                        .status("ACTIVE")
                        .build()));
    }

    private RequestPostProcessor customerUser() {
        return user(new CustomUserDetails(customerEntity, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private RequestPostProcessor moderatorUser() {
        return user(new CustomUserDetails(moderatorEntity, List.of(new SimpleGrantedAuthority("ROLE_CONTENT_MODERATOR"))));
    }

    @Test
    @DisplayName("TC-SYS-BF04-001: Đăng bài viết sạch - Tự động duyệt đăng lên bảng tin")
    void tcSysBF04001_cleanPostAutoApproval() throws Exception {
        // Bước 1: Khách hàng đăng bài viết sạch
        mockMvc.perform(post("/community/create")
                        .with(customerUser())
                        .with(csrf())
                        .param("title", "Bonsai Pine Pruning")
                        .param("content", "Guidelines for beginners")
                        .param("category", "Kỹ thuật"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/community"));

        // Bước 2: Hệ thống kiểm tra DB (status=APPROVED, không bị sửa đổi)
        CommunityPost post = postRepository.findAll().stream()
                .filter(p -> "Bonsai Pine Pruning".equals(p.getTitle()))
                .findFirst()
                .orElseThrow();
        assertEquals("APPROVED", post.getStatus());
        assertEquals("Guidelines for beginners", post.getContent());

        // Bước 3: Đọc bảng tin công cộng hiển thị bài viết mới
        mockMvc.perform(get("/community")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("posts"));
    }

    @Test
    @DisplayName("TC-SYS-BF04-002: Đăng bài viết tục tĩu - Mask từ nhạy cảm, Flaged và Moderator duyệt thủ công")
    void tcSysBF04002_flaggedPostApprovedByModerator() throws Exception {
        // Bước 1: Khách hàng đăng bài viết chứa từ tục tĩu "đm"
        mockMvc.perform(post("/community/create")
                        .with(customerUser())
                        .with(csrf())
                        .param("title", "đm Cây bonsai")
                        .param("content", "Normal content")
                        .param("category", "Kỹ thuật"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/community"));

        // Bước 2: Hệ thống chuyển trạng thái FLAGGED và mask từ nhạy cảm
        CommunityPost post = postRepository.findAll().stream()
                .filter(p -> "Normal content".equals(p.getContent()))
                .findFirst()
                .orElseThrow();
        assertEquals("FLAGGED", post.getStatus());
        assertTrue(post.getTitle().contains("***")); // Mask "đm Cây bonsai" -> "Cây ***" hoặc "*** Cây..."

        // Bước 3: Moderator xem danh sách kiểm duyệt
        mockMvc.perform(get("/moderator/community")
                        .with(moderatorUser()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("posts"));

        // Bước 4: Moderator bấm phê duyệt bài viết
        mockMvc.perform(post("/moderator/community/posts/" + post.getPostId() + "/approve")
                        .with(moderatorUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/moderator/community?tab=posts"));

        // Bước 5: Hệ thống đổi trạng thái thành APPROVED
        CommunityPost approvedPost = postRepository.findById(post.getPostId()).orElseThrow();
        assertEquals("APPROVED", approvedPost.getStatus());

        // Bước 6: Khách hàng đọc bảng tin công cộng thấy bài viết đã được đăng
        mockMvc.perform(get("/community")
                        .with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-SYS-BF04-003: Đăng bài viết tục tĩu - Bị khóa và Moderator từ chối (Xóa bài)")
    void tcSysBF04003_flaggedPostRejectedByModerator() throws Exception {
        // Bước 1: Khách hàng đăng bài viết tục tĩu
        mockMvc.perform(post("/community/create")
                        .with(customerUser())
                        .with(csrf())
                        .param("title", "đm Cây cảnh")
                        .param("content", "Rejection test content")
                        .param("category", "Kỹ thuật"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/community"));

        // Bước 2: Hệ thống lưu trạng thái FLAGGED
        CommunityPost post = postRepository.findAll().stream()
                .filter(p -> "Rejection test content".equals(p.getContent()))
                .findFirst()
                .orElseThrow();
        assertEquals("FLAGGED", post.getStatus());

        // Bước 3: Moderator xem hàng chờ
        mockMvc.perform(get("/moderator/community")
                        .with(moderatorUser()))
                .andExpect(status().isOk());

        // Bước 4: Moderator từ chối duyệt (Xóa bài đăng)
        mockMvc.perform(post("/community/post/delete/" + post.getPostId())
                        .with(moderatorUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/community"));

        // Bước 5: Hệ thống xóa khỏi database
        Optional<CommunityPost> deletedPost = postRepository.findById(post.getPostId());
        assertTrue(deletedPost.isEmpty());
    }
}
