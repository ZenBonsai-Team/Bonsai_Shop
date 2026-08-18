package com.example.bonsai_shop.security;

import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.artisan.service.ProductJournalService;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomOAuth2UserService;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.customer.service.CustomUserDetailsService;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.moderator.service.MyOrderService;
import com.example.bonsai_shop.moderator.service.OrderActionService;
import com.example.bonsai_shop.moderator.service.OrderDetailService;
import com.example.bonsai_shop.owner.service.AccountService;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.service.CartService;
import com.example.bonsai_shop.product.service.MailService;
import com.example.bonsai_shop.product.service.OrderService;
import com.example.bonsai_shop.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite kiểm thử Phân quyền & Bảo mật (Security & Authorization Testing) cho BSMS.
 * Quét file SecurityConfig.java và kiểm thử việc chặn truy cập trái phép bằng @WithMockUser.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class SecurityAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private ArtisanProductService artisanProductService;

    @MockitoBean
    private ProductJournalService productJournalService;

    @MockitoBean
    private MyOrderService myOrderService;

    @MockitoBean
    private OrderDetailService orderDetailService;

    @MockitoBean
    private OrderActionService orderActionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderLogRepository orderLogRepository;

    @MockitoBean
    private OrderHandlingRepository orderHandlingRepository;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private FinancialLedgerService financialLedgerService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ProductService productService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private RequestPostProcessor customUser(String email, String roleName) {
        User userEntity = User.builder()
                .userId(1)
                .email(email)
                .fullName("Test User")
                .avatar("https://ui-avatars.com/api/?name=Test")
                .role(Role.builder().roleId(1).roleName(roleName).build())
                .status("ACTIVE")
                .build();
        CustomUserDetails details = new CustomUserDetails(userEntity, List.of(new SimpleGrantedAuthority("ROLE_" + roleName)));
        return user(details);
    }

    @Test
    @DisplayName("Negative Test: Khách vãng lai (Unauthenticated) cố tình truy cập /owner/users bị redirect về /login")
    void testUnauthenticatedUser_AccessOwnerEndpoint_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/owner/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Negative Test: Tài khoản ROLE_CUSTOMER cố tình truy cập /owner/users nhận HTTP 403 Forbidden")
    @WithMockUser(roles = "CUSTOMER")
    void testCustomerRole_AccessOwnerEndpoint_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/owner/users").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Negative Test: Tài khoản ROLE_CUSTOMER cố tình truy cập /artisan/products nhận HTTP 403 Forbidden")
    @WithMockUser(roles = "CUSTOMER")
    void testCustomerRole_AccessArtisanEndpoint_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/artisan/products").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Negative Test: Tài khoản ROLE_CUSTOMER cố tình truy cập /moderator/orders nhận HTTP 403 Forbidden")
    @WithMockUser(roles = "CUSTOMER")
    void testCustomerRole_AccessModeratorEndpoint_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/moderator/orders").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Positive Test: Tài khoản ROLE_OWNER truy cập /owner/users được phép truy cập (HTTP 200 OK)")
    void testOwnerRole_AccessOwnerEndpoint_IsAuthorized() throws Exception {
        mockMvc.perform(get("/owner/users").with(customUser("owner@bsms.com", "OWNER")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Positive Test: Tài khoản ROLE_ARTISAN truy cập /artisan/products được phép truy cập (HTTP 200 OK)")
    void testArtisanRole_AccessArtisanEndpoint_IsAuthorized() throws Exception {
        mockMvc.perform(get("/artisan/products").with(customUser("artisan@bsms.com", "ARTISAN")).with(csrf()))
                .andExpect(status().isOk());
    }
}
