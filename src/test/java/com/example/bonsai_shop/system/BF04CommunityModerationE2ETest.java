package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.*;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.SelectOption;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * L3 SYSTEM TEST (E2E Business Flow Testing - §3b) cho BF-04: Community Moderation & Auto-Approval.
 * Sử dụng Microsoft Playwright để giả lập các tương tác trình duyệt thực tế (click, fill, navigate).
 * Chạy chế độ HEADED (mở trình duyệt thật trên Windows) và SLOW_MO (chậm 1 giây mỗi thao tác) để quan sát trực quan.
 * Bao gồm kiểm thử RBAC trên giao diện và Quản lý phiên (Session Management) khi Đăng xuất.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BF04CommunityModerationE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Các repository phục vụ việc dọn dẹp data để tránh xung đột giữa các lần chạy
    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private CommunityPostLikeRepository postLikeRepository;

    @Autowired
    private CommunityPostBookmarkRepository postBookmarkRepository;

    @Autowired
    private CommunityCommentRepository commentRepository;

    @Autowired
    private ModerationNotificationRepository notificationRepository;

    private Playwright playwright;
    private Browser browser;
    private User customerEntity;
    private User moderatorEntity;

    @BeforeAll
    void setUpAll() {
        // Dọn dẹp sạch sẽ database của phần Community trước khi chạy test E2E để tránh xung đột
        postLikeRepository.deleteAll();
        postBookmarkRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        notificationRepository.deleteAll();

        playwright = Playwright.create();
        // Mở trình duyệt thật (headless = false) và làm chậm đi 1 giây (slowMo = 1000) để người dùng xem trực quan
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false)
                        .setSlowMo(1000)
        );

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CUSTOMER").build()));

        Role modRole = roleRepository.findByRoleName("CONTENT_MODERATOR")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CONTENT_MODERATOR").build()));

        // Thiết lập / Đồng bộ tài khoản khách hàng
        userRepository.findByEmail("customer.e2e@test.com").ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(customerRole);
            userRepository.save(u);
        });
        customerEntity = userRepository.findByEmail("customer.e2e@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("E2E Customer")
                        .email("customer.e2e@test.com")
                        .username("customer_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(customerRole)
                        .status("ACTIVE")
                        .build()));

        // Thiết lập / Đồng bộ tài khoản kiểm duyệt viên
        userRepository.findByEmail("moderator.e2e@test.com").ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(modRole);
            userRepository.save(u);
        });
        moderatorEntity = userRepository.findByEmail("moderator.e2e@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("E2E Moderator")
                        .email("moderator.e2e@test.com")
                        .username("moderator_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(modRole)
                        .status("ACTIVE")
                        .build()));
    }

    @AfterAll
    void tearDownAll() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private void login(Page page, String email, String password) {
        page.navigate(getBaseUrl() + "/login");
        page.fill("#email", email);
        page.fill("#password", password);
        page.click("button.btn-signin");
        page.waitForURL(url -> !url.contains("/login"));
    }

    @Test
    @Order(1)
    @DisplayName("TC-E2E-BF04-001: Happy Path - Clean post auto-approval and visibility")
    void tcE2EBF04001_cleanPostAutoApproval() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            
            // Bước 1: Khách hàng đăng nhập và tạo bài viết sạch
            login(page, customerEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/community/create");
            System.out.println("[TC1 DEBUG] Loaded Create Page. Current URL: " + page.url());
            
            page.fill("#title", "Pines Care Guide");
            page.selectOption("#category", new SelectOption().setIndex(1));
            page.fill("#readTime", "5");
            page.fill("#summary", "This is a short summary of the pines care post.");
            page.fill("#content", "Detailed tips and step-by-step instructions for pruning bonsai pine trees.");
            
            System.out.println("[TC1 DEBUG] Fields filled. Submitting form...");
            page.click("form.create-post-form button[type='submit']");

            // Chờ load xong trang
            page.waitForTimeout(2000);
            System.out.println("[TC1 DEBUG] URL after submit: " + page.url());

            // Đảm bảo URL hiện tại chứa '/community' và đã thoát khỏi trang tạo
            assertTrue(page.url().contains("/community"));

            // Bước 2: Cuộn xem bảng tin để thấy bài viết hiển thị trực tiếp trong feed
            page.waitForSelector(".social-card-title a:has-text('Pines Care Guide')");
            assertTrue(page.locator(".social-card-title a:has-text('Pines Care Guide')").first().isVisible());
        }
    }

    @Test
    @Order(2)
    @DisplayName("TC-E2E-BF04-002: Exception Path - Flagged post and Moderator manual approval")
    void tcE2EBF04002_flaggedPostApprovedByModerator() {
        // 1. Tạo bài viết chứa từ tục tĩu (sử dụng "dm" làm từ nhạy cảm ASCII để tránh lỗi encoding)
        try (BrowserContext customerContext = browser.newContext()) {
            Page page = customerContext.newPage();
            login(page, customerEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/community/create");
            System.out.println("[TC2 DEBUG] Loaded Create Page. Current URL: " + page.url());

            page.fill("#title", "dm Cay dep");
            page.selectOption("#category", new SelectOption().setIndex(1));
            page.fill("#readTime", "5");
            page.fill("#summary", "Short summary describing the post.");
            page.fill("#content", "Detailed content that contains clean words besides the flagged title.");
            
            System.out.println("[TC2 DEBUG] Fields filled. Submitting form...");
            page.click("form.create-post-form button[type='submit']");
            
            page.waitForTimeout(2000);
            System.out.println("[TC2 DEBUG] URL after submit: " + page.url());

            // In thông tin bài viết trong DB để debug
            List<CommunityPost> postsInDb = postRepository.findAll();
            System.out.println("[TC2 DEBUG] Posts in DB right after submit:");
            for (CommunityPost cp : postsInDb) {
                System.out.println("[TC2 DEBUG] -> ID: " + cp.getPostId() + " | Title: " + cp.getTitle() + " | Status: " + cp.getStatus());
            }

            // Đảm bảo không hiển thị công khai trên bảng tin cộng đồng ( feed card )
            page.navigate(getBaseUrl() + "/community");
            System.out.println("[TC2 DEBUG] Loaded /community feed URL: " + page.url());
            
            // Lấy text của toàn bộ container feed và kiểm tra xem có chứa bài viết bị FLAGGED không
            page.waitForSelector(".social-feed-container");
            String feedText = page.locator(".social-feed-container").textContent();
            assertFalse(feedText.contains("*** Cay dep"), "Bài viết FLAGGED không được hiển thị công khai trên bảng tin!");
        }

        // 2. Moderator vào duyệt bài viết
        try (BrowserContext moderatorContext = browser.newContext()) {
            Page page = moderatorContext.newPage();
            login(page, moderatorEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/moderator/community");
            System.out.println("[TC2 DEBUG] Moderator page URL: " + page.url());

            // Phải xuất hiện từ ngữ đã được mask (*** Cay dep) trong bảng chờ duyệt
            page.waitForSelector("text=*** Cay dep");
            assertTrue(page.locator("text=*** Cay dep").first().isVisible());

            // Tìm hàng bảng (tr) cụ thể chứa bài viết và click nút Duyệt (submit form)
            Locator row = page.locator("tr:has-text('*** Cay dep')").first();
            row.locator("form[action*='/approve'] button[type='submit']").click();
            
            page.waitForTimeout(2000);
            System.out.println("[TC2 DEBUG] Moderator URL after approve: " + page.url());

            // Đã biến mất khỏi hàng chờ duyệt
            assertFalse(page.locator("text=*** Cay dep").isVisible());
        }

        // 3. Khách hàng kiểm tra bảng tin thấy bài viết xuất hiện dạng che giấu
        try (BrowserContext customerContext = browser.newContext()) {
            Page page = customerContext.newPage();
            login(page, customerEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/community");
            
            // Chờ cho container feed sẵn sàng và kiểm tra xem tiêu đề bài viết đã xuất hiện sau khi duyệt
            page.waitForSelector(".social-feed-container");
            String feedText = page.locator(".social-feed-container").textContent();
            assertTrue(feedText.contains("*** Cay dep"), "Bài viết sau khi được Duyệt phải xuất hiện trên bảng tin!");
        }
    }

    @Test
    @Order(3)
    @DisplayName("TC-E2E-BF04-003: Exception Path - Flagged post and Moderator rejection (deletion)")
    void tcE2EBF04003_flaggedPostRejectedByModerator() {
        // 1. Tạo bài viết tục tĩu tiếp theo
        try (BrowserContext customerContext = browser.newContext()) {
            Page page = customerContext.newPage();
            login(page, customerEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/community/create");
            System.out.println("[TC3 DEBUG] Loaded Create Page. Current URL: " + page.url());

            page.fill("#title", "dm Bonsai Canh");
            page.selectOption("#category", new SelectOption().setIndex(1));
            page.fill("#readTime", "5");
            page.fill("#summary", "Short summary for rejection test.");
            page.fill("#content", "Detailed content body for E2E rejection testing.");
            
            System.out.println("[TC3 DEBUG] Fields filled. Submitting form...");
            page.click("form.create-post-form button[type='submit']");
            
            page.waitForTimeout(2000);
            System.out.println("[TC3 DEBUG] URL after submit: " + page.url());

            // In thông tin bài viết trong DB để debug
            List<CommunityPost> postsInDb = postRepository.findAll();
            System.out.println("[TC3 DEBUG] Posts in DB right after submit:");
            for (CommunityPost cp : postsInDb) {
                System.out.println("[TC3 DEBUG] -> ID: " + cp.getPostId() + " | Title: " + cp.getTitle() + " | Status: " + cp.getStatus());
            }

            page.navigate(getBaseUrl() + "/community");
            page.waitForSelector(".social-feed-container");
            String feedText = page.locator(".social-feed-container").textContent();
            assertFalse(feedText.contains("*** Bonsai Canh"), "Bài viết FLAGGED tiếp theo không được hiển thị công khai!");
        }

        // 2. Moderator từ chối (xóa bài viết)
        try (BrowserContext moderatorContext = browser.newContext()) {
            Page page = moderatorContext.newPage();
            login(page, moderatorEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/moderator/community");
            System.out.println("[TC3 DEBUG] Moderator page URL: " + page.url());

            page.waitForSelector("text=*** Bonsai Canh");
            assertTrue(page.locator("text=*** Bonsai Canh").first().isVisible());

            // Tìm hàng bảng (tr) cụ thể chứa bài viết và click nút Xóa bên trong hàng đó
            Locator row = page.locator("tr:has-text('*** Bonsai Canh')").first();
            row.locator("button:has-text('Xóa')").click();
            
            page.waitForSelector("#moderationModal");
            
            // Xác nhận xóa trong modal
            page.click("#moderationForm button[type='submit']");
            page.waitForTimeout(1000); // chờ lưu và đóng modal

            // Đã bị xóa hoàn toàn khỏi DB/UI hàng chờ
            assertFalse(page.locator("text=*** Bonsai Canh").isVisible());
        }
    }

    @Test
    @Order(4)
    @DisplayName("TC-E2E-BF04-004: RBAC-on-UI Spot-check - Access boundaries validation")
    void tcE2EBF04004_rbacOnUiBoundaries() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            login(page, customerEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/community");

            // Khách hàng không được thấy các nút kiểm duyệt duyệt/ẩn bài viết
            assertFalse(page.locator("button:has-text('Duyệt')").isVisible());
            assertFalse(page.locator("button:has-text('Ẩn')").isVisible());

            // Cố tình truy cập trực tiếp URL của kiểm duyệt viên
            page.navigate(getBaseUrl() + "/moderator/community");
            
            // Bị trả về trang lỗi phân quyền
            assertTrue(page.content().contains("403") || page.content().contains("Forbidden") || page.content().contains("Access Denied") || page.url().contains("error"));
        }
    }

    @Test
    @Order(5)
    @DisplayName("TC-E2E-BF04-005: Session Management - Sign-out and session invalidation")
    void tcE2EBF04005_sessionManagementSignOut() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            login(page, customerEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/community");

            // Đăng xuất từ trang hồ sơ
            page.navigate(getBaseUrl() + "/profile");
            page.click("button:has-text('Đăng xuất')");
            page.waitForURL(url -> url.contains("/login"));

            // Thử quay lại trang tạo bài đăng
            page.navigate(getBaseUrl() + "/community/create");
            
            // Hệ thống tự động đá về trang login do mất phiên
            assertTrue(page.url().contains("/login"));
        }
    }
}
