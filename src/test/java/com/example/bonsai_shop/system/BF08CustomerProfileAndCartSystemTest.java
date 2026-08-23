package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.*;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.product.repository.*;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BF08CustomerProfileAndCartSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RegisterOtpRepository otpRepository;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private CommunityPostBookmarkRepository bookmarkRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository segmentRepository;

    @MockitoBean
    private CloudinaryStorageService cloudinaryStorageService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_CUSTOMER").description("Khách hàng").build()));
    }

    private RequestPostProcessor customerUser(User user) {
        return user(new CustomUserDetails(user, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private User createActiveCustomer(String username, String email) {
        return userRepository.save(User.builder()
                .fullName("Test Customer")
                .username(username)
                .email(email)
                .password("encoded-password")
                .phone("0912345678")
                .status("ACTIVE")
                .role(customerRole)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ======================== TESTS ========================

    @Test
    void tcSysBF08001_registerCustomerSuccess() throws Exception {
        String email = "new.customer@test.com";
        String username = "newcustomer";

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Nguyen Van Customer")
                        .param("username", username)
                        .param("email", email)
                        .param("password", "password123")
                        .param("phone", "0987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/verify-otp"))
                .andExpect(model().attributeExists("email"))
                .andExpect(model().attributeExists("success"));

        Optional<User> pendingUserOpt = userRepository.findByEmail(email);
        assertTrue(pendingUserOpt.isPresent());
        User pendingUser = pendingUserOpt.get();
        assertEquals("Nguyen Van Customer", pendingUser.getFullName());
        assertEquals("PENDING", pendingUser.getStatus());

        Optional<PasswordResetOtp> otpOpt = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);
        assertTrue(otpOpt.isPresent());
        assertFalse(otpOpt.get().getIsUsed());
    }

    @Test
    void tcSysBF08002_registerCustomerFailureDuplicateEmail() throws Exception {
        createActiveCustomer("existinguser", "existing@test.com");

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Nguyen Van Customer")
                        .param("username", "newcustomer")
                        .param("email", "existing@test.com")
                        .param("password", "password123")
                        .param("phone", "0987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/register"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("formData"));
    }

    @Test
    void tcSysBF08003_registerCustomerFailureDuplicateUsername() throws Exception {
        createActiveCustomer("existinguser", "existing@test.com");

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Nguyen Van Customer")
                        .param("username", "existinguser")
                        .param("email", "newemail@test.com")
                        .param("password", "password123")
                        .param("phone", "0987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/register"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("formData"));
    }

    @Test
    void tcSysBF08004_registerCustomerFailureValidationErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "")
                        .param("username", "")
                        .param("email", "invalid-email")
                        .param("password", "123")
                        .param("phone", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/register"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("formData"));
    }

    @Test
    void tcSysBF08005_verifyOtpSuccessAndAutoLogin() throws Exception {
        String email = "verify.otp@test.com";
        String username = "verifyotp";
        
        // Tạo user PENDING
        User user = User.builder()
                .fullName("Verify Otp Customer")
                .username(username)
                .email(email)
                .password("encoded-password")
                .phone("0987654321")
                .status("PENDING")
                .role(customerRole)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // Tạo OTP
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(email)
                .otpCode("123456")
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();
        otpRepository.save(otp);

        mockMvc.perform(post("/verify-otp")
                        .with(csrf())
                        .param("email", email)
                        .param("otpCode", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attributeExists("registrationSuccess"));

        User activatedUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals("ACTIVE", activatedUser.getStatus());

        PasswordResetOtp usedOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email).orElseThrow();
        assertTrue(usedOtp.getIsUsed());
    }

    @Test
    void tcSysBF08006_verifyOtpFailureWrongCode() throws Exception {
        String email = "wrong.otp@test.com";
        
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(email)
                .otpCode("123456")
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();
        otpRepository.save(otp);

        mockMvc.perform(post("/verify-otp")
                        .with(csrf())
                        .param("email", email)
                        .param("otpCode", "999999"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/verify-otp"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("email", email));
    }

    @Test
    void tcSysBF08007_verifyOtpFailureExpired() throws Exception {
        String email = "expired.otp@test.com";
        
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(email)
                .otpCode("123456")
                .expiredAt(LocalDateTime.now().minusMinutes(1)) // Expired
                .isUsed(false)
                .createdAt(LocalDateTime.now().minusMinutes(6))
                .build();
        otpRepository.save(otp);

        mockMvc.perform(post("/verify-otp")
                        .with(csrf())
                        .param("email", email)
                        .param("otpCode", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/verify-otp"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("email", email));
    }

    @Test
    void tcSysBF08008_resendOtpSuccess() throws Exception {
        String email = "resend.otp@test.com";
        User user = User.builder()
                .fullName("Resend Otp Customer")
                .username("resendotp")
                .email(email)
                .password("encoded-password")
                .phone("0987654321")
                .status("PENDING")
                .role(customerRole)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        mockMvc.perform(post("/resend-otp")
                        .with(csrf())
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/verify-otp"))
                .andExpect(model().attributeExists("success"))
                .andExpect(model().attribute("email", email));

        Optional<PasswordResetOtp> newOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);
        assertTrue(newOtp.isPresent());
        assertFalse(newOtp.get().getIsUsed());
    }

    @Test
    void tcSysBF08009_viewProfilePageSuccess() throws Exception {
        User user = createActiveCustomer("profileuser", "profile@test.com");

        mockMvc.perform(get("/profile")
                        .with(customerUser(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("myBonsaiPosts"))
                .andExpect(model().attributeExists("savedPosts"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("reviewedProductIds"))
                .andExpect(model().attributeExists("expiredOrderIds"));

        mockMvc.perform(get("/profile/update")
                        .with(customerUser(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attributeExists("user"));
        
        mockMvc.perform(get("/orders")
                        .with(customerUser(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile#orderHistorySection"));
    }

    @Test
    void tcSysBF08010_updateProfileSuccessAndAvatarUpload() throws Exception {
        User user = createActiveCustomer("updateprofile", "updateprofile@test.com");
        
        String newAvatarUrl = "http://cloudinary.com/avatar.jpg";
        when(cloudinaryStorageService.uploadImage(any(), any()))
                .thenReturn(new CloudinaryUploadResponse(newAvatarUrl, "avatar-public-id", "image"));

        MockMultipartFile avatarFile = new MockMultipartFile("avatarFile", "avatar.jpg", "image/jpeg", "avatar-data".getBytes());

        mockMvc.perform(multipart("/profile/update")
                        .file(avatarFile)
                        .with(customerUser(user)).with(csrf())
                        .param("fullName", "Nguyen Van Updated")
                        .param("username", "updatedprofile")
                        .param("phone", "0988888888")
                        .param("address", "Hanoi, Vietnam"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attributeExists("success"))
                .andExpect(model().attributeExists("user"));

        User updatedUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertEquals("Nguyen Van Updated", updatedUser.getFullName());
        assertEquals("updatedprofile", updatedUser.getUsername());
        assertEquals("0988888888", updatedUser.getPhone());
        assertEquals("Hanoi, Vietnam", updatedUser.getAddress());
        assertEquals(newAvatarUrl, updatedUser.getAvatar());
        assertEquals("avatar-public-id", updatedUser.getAvatarPublicId());
    }

    @Test
    void tcSysBF08011_updateProfileFailureValidationErrors() throws Exception {
        User user = createActiveCustomer("failprofile", "failprofile@test.com");
        createActiveCustomer("existingusername", "existingusername@test.com");

        // Case: Ho ten qua ngan
        mockMvc.perform(post("/profile/update")
                        .with(customerUser(user)).with(csrf())
                        .param("fullName", "ab")
                        .param("username", "failprofile")
                        .param("phone", "0987654321")
                        .param("address", "Hanoi"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("user", user));

        // Case: So dien thoai sai dinh dang
        mockMvc.perform(post("/profile/update")
                        .with(customerUser(user)).with(csrf())
                        .param("fullName", "Valid Name")
                        .param("username", "failprofile")
                        .param("phone", "1234567")
                        .param("address", "Hanoi"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attributeExists("error"));

        // Case: Ten dang nhap da ton tai
        mockMvc.perform(post("/profile/update")
                        .with(customerUser(user)).with(csrf())
                        .param("fullName", "Valid Name")
                        .param("username", "existingusername")
                        .param("phone", "0987654321")
                        .param("address", "Hanoi"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void tcSysBF08012_viewCartAndCheckoutAndWishlistPages() throws Exception {
        User user = createActiveCustomer("cartuser", "cart@test.com");

        mockMvc.perform(get("/cart")
                        .with(customerUser(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/cart"))
                .andExpect(model().attribute("activePage", "cart"));

        mockMvc.perform(get("/checkout")
                        .with(customerUser(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/checkout"))
                .andExpect(model().attribute("activePage", "checkout"))
                .andExpect(model().attributeExists("user"));

        mockMvc.perform(get("/wishlist")
                        .with(customerUser(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/wishlist"))
                .andExpect(model().attribute("activePage", "wishlist"));
    }
}
