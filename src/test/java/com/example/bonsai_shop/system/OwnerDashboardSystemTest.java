package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import com.example.bonsai_shop.owner.service.AccountService;
import com.example.bonsai_shop.owner.service.OwnerDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnerDashboardSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OwnerDashboardService ownerDashboardService;

    @MockitoBean
    private CloudinaryStorageService cloudinaryStorageService;

    private User ownerUser;

    @BeforeEach
    void setUp() {
        ownerUser = findOrCreateOwner();
    }

    private RequestPostProcessor ownerDetailsUser() {
        return user(new CustomUserDetails(ownerUser,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
    }

    private User findOrCreateOwner() {
        Role role = roleRepository.findByRoleName("ROLE_OWNER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_OWNER").description("Chủ cửa hàng").build()));
        String email = "owner.test@test.com";
        return accountRepository.findAll().stream()
                .filter(item -> email.equalsIgnoreCase(item.getEmail()))
                .findFirst()
                .orElseGet(() -> accountRepository.save(User.builder()
                        .fullName("Test Owner")
                        .email(email)
                        .username("owner.test")
                        .password("123456")
                        .phone("0910000088")
                        .status("ACTIVE")
                        .role(role)
                        .build()));
    }

    private User createStaffUser(String username, String email, Role role) {
        return accountRepository.save(User.builder()
                .fullName("Staff User")
                .username(username)
                .email(email)
                .password("encoded-pass")
                .phone("0912345678")
                .status("ACTIVE")
                .role(role)
                .build());
    }

    // ======================== DASHBOARD TESTS ========================

    @Test
    void testDashboard_Success() throws Exception {
        mockMvc.perform(get("/owner")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/dashboard"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(model().attributeExists("monthlyRevenue"));

        // Check select month query param
        mockMvc.perform(get("/owner/dashboard")
                        .param("month", "2026-08")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/dashboard"))
                .andExpect(model().attribute("selectedMonth", "2026-08"));
    }

    @Test
    void testDashboard_SoldAndGardenTrees() throws Exception {
        mockMvc.perform(get("/owner/dashboard/sold-trees")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/sold_trees"))
                .andExpect(model().attributeExists("soldTrees"));

        mockMvc.perform(get("/owner/dashboard/garden-trees")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/garden_trees"))
                .andExpect(model().attributeExists("gardenTrees"));
    }

    @Test
    void testDashboard_FinancialReports() throws Exception {
        mockMvc.perform(get("/owner/dashboard/artisan-revenue")
                        .param("month", "2026-08")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/artisan_revenue"))
                .andExpect(model().attributeExists("artisanRevenueDetails"));

        mockMvc.perform(get("/owner/dashboard/forfeited-deposits")
                        .param("month", "2026-08")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/finance_sources"));

        mockMvc.perform(get("/owner/dashboard/customer-refunds")
                        .param("month", "2026-08")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/finance_sources"));
    }

    // ======================== SYSTEM CONFIG TESTS ========================

    @Test
    void testSystemConfig_GetAndSaveSuccess() throws Exception {
        mockMvc.perform(get("/owner/system-config")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/system_config"))
                .andExpect(model().attribute("activeMenu", "system-config"));

        // Mock upload banner files
        when(cloudinaryStorageService.uploadImage(any(), any()))
                .thenReturn(new CloudinaryUploadResponse("http://cloudinary.com/banner.jpg", "banner-id", "image"));

        MockMultipartFile homeFile = new MockMultipartFile("home_banner_image_file", "home.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile marketFile = new MockMultipartFile("marketplace_banner_image_file", "market.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile communityFile = new MockMultipartFile("community_banner_image_file", "community.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile luxuryFile = new MockMultipartFile("luxury_banner_image_file", "luxury.jpg", "image/jpeg", "data".getBytes());

        mockMvc.perform(multipart("/owner/system-config")
                        .file(homeFile)
                        .file(marketFile)
                        .file(communityFile)
                        .file(luxuryFile)
                        .with(ownerDetailsUser()).with(csrf())
                        .param("shipping_fee_per_km", "15000")
                        .param("crane_fee_per_km", "25000"))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/system_config"))
                .andExpect(model().attributeExists("success"));
    }

    // ======================== USER MANAGEMENT TESTS ========================

    @Test
    void testListAccounts_Success() throws Exception {
        mockMvc.perform(get("/owner/users")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/user_list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    void testCreateUserAccount_SuccessAndFailure() throws Exception {
        Role staffRole = roleRepository.findByRoleName("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_STAFF").description("Nhan vien").build()));

        mockMvc.perform(get("/owner/users/create")
                        .with(ownerDetailsUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/users_create"))
                .andExpect(model().attributeExists("roles"));

        // Create success
        String email = "staff.user@test.com";
        mockMvc.perform(post("/owner/users/create")
                        .with(ownerDetailsUser()).with(csrf())
                        .param("fullName", "Staff Nguyen")
                        .param("email", email)
                        .param("password", "password123")
                        .param("phone", "0987654321")
                        .param("roleId", String.valueOf(staffRole.getRoleId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash().attributeExists("success"));

        // Create failure - Duplicate email
        mockMvc.perform(post("/owner/users/create")
                        .with(ownerDetailsUser()).with(csrf())
                        .param("fullName", "Staff Nguyen Duplicate")
                        .param("email", email)
                        .param("password", "password123")
                        .param("phone", "0987654321")
                        .param("roleId", String.valueOf(staffRole.getRoleId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users/create"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void testUserAccountStatus_ToggleLockUnlock() throws Exception {
        Role staffRole = roleRepository.findByRoleName("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_STAFF").description("Nhan vien").build()));
        User staff = createStaffUser("stafftoggle", "staff.toggle@test.com", staffRole);

        // Toggle status
        mockMvc.perform(post("/owner/users/toggle-status")
                        .with(ownerDetailsUser()).with(csrf())
                        .param("userId", String.valueOf(staff.getUserId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash().attributeExists("success"));

        // Reset ve ACTIVE truoc khi test lock de tranh loi "Tai khoan da bi khoa roi!"
        staff.setStatus("ACTIVE");
        accountRepository.save(staff);

        // Lock
        mockMvc.perform(post("/owner/users/lock")
                        .with(ownerDetailsUser()).with(csrf())
                        .param("userId", String.valueOf(staff.getUserId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash().attributeExists("success"));

        // Unlock
        mockMvc.perform(post("/owner/users/unlock")
                        .with(ownerDetailsUser()).with(csrf())
                        .param("userId", String.valueOf(staff.getUserId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash().attributeExists("success"));
    }
}
