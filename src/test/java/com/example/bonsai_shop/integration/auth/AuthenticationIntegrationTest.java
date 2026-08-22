package com.example.bonsai_shop.integration.auth;

import com.example.bonsai_shop.customer.repository.CommunityPostBookmarkRepository;
import com.example.bonsai_shop.customer.repository.CommunityPostRepository;
import com.example.bonsai_shop.customer.repository.RegisterOtpRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomOAuth2UserService;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.customer.service.CustomUserDetailsService;
import com.example.bonsai_shop.customer.service.EmailService;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.CommunityPost;
import com.example.bonsai_shop.entity.CommunityPostBookmark;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.Review;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.owner.controller.AuthenticationController;
import com.example.bonsai_shop.owner.service.SystemConfigService;
import com.example.bonsai_shop.product.repository.ReviewRepository;
import com.example.bonsai_shop.product.service.OrderService;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        controllers = com.example.bonsai_shop.customer.controller.ProfileController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(UserService.class)
class AuthenticationIntegrationTest {

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private RegisterOtpRepository otpRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CloudinaryStorageService cloudinaryStorageService;

    @MockitoBean
    private CommunityPostRepository communityPostRepository;

    @MockitoBean
    private CommunityPostBookmarkRepository bookmarkRepository;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private AuthenticationController authenticationController;

    @MockitoBean
    private SystemConfigService systemConfigService;

    @BeforeEach
    void resetMocks() {
        SecurityContextHolder.clearContext();
        reset(userRepository, roleRepository, otpRepository, emailService, passwordEncoder,
                cloudinaryStorageService, communityPostRepository, bookmarkRepository, orderService,
                reviewRepository, customUserDetailsService, customOAuth2UserService, authenticationController,
                systemConfigService);

        mockMvc = MockMvcBuilders.standaloneSetup(new com.example.bonsai_shop.customer.controller.ProfileController(
                        userService,
                        communityPostRepository,
                        bookmarkRepository,
                        userRepository,
                        orderService,
                        reviewRepository
                ))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void redirectUnauthenticatedUserToLoginPage() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void displayProfileForAuthenticatedFormLoginUser() throws Exception {
        User currentUser = customer(1, "form-profile");
        stubProfileData(currentUser);

        mockMvc.perform(get("/profile").with(formLoginUser(currentUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile"))
                .andExpect(model().attribute("user", currentUser));
    }

    @Test
    void displayProfileForAuthenticatedOAuth2User() throws Exception {
        User currentUser = customer(2, "oauth-profile");
        stubProfileData(currentUser);

        mockMvc.perform(get("/profile").with(oauth2User(currentUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile"))
                .andExpect(model().attribute("user", currentUser));
    }

    @Test
    void redirectWhenOAuth2PrincipalHasNoEmailAttribute() throws Exception {
        mockMvc.perform(get("/profile").with(oauth2UserWithoutEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void handleAuthenticatedPrincipalWithUnsupportedType() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                new Object(),
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        mockMvc.perform(get("/profile").with(authenticatedPrincipal(authentication.getPrincipal(), authentication.getAuthorities())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void failProfileLoadingWhenUserCannotBeFound() {
        when(userRepository.findByEmail("missing-profile@example.com")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () ->
                mockMvc.perform(get("/profile")
                        .with(formLoginEmail("missing-profile@example.com")))
        );

        assertThat(exception).hasMessageContaining("Kh");
    }

    @Test
    void loadCurrentUsersCommunityPosts() throws Exception {
        User currentUser = customer(10, "my-posts");
        CommunityPost ownPost = communityPost(101, currentUser.getUserId(), "Own post");
        stubProfileData(currentUser, List.of(ownPost), List.of(), List.of(), List.of());

        MvcResult result = performProfile(currentUser);

        assertThat(modelList(result, "myBonsaiPosts", CommunityPost.class))
                .extracting(CommunityPost::getPostId)
                .containsExactly(ownPost.getPostId());
    }

    @Test
    void useFullNameAsPostAuthorNameWhenAvailable() throws Exception {
        User currentUser = customer(11, "full-name-author");
        currentUser.setFullName("Available Full Name");
        CommunityPost ownPost = communityPost(102, currentUser.getUserId(), "Own post");
        stubProfileData(currentUser, List.of(ownPost), List.of(), List.of(), List.of());

        CommunityPost post = firstModelPost(performProfile(currentUser), "myBonsaiPosts");

        assertThat(post.getAuthorName()).isEqualTo("Available Full Name");
    }

    @Test
    void useUsernameAsPostAuthorNameWhenFullNameIsUnavailable() throws Exception {
        User currentUser = customer(12, "username-author");
        currentUser.setFullName("");
        CommunityPost ownPost = communityPost(103, currentUser.getUserId(), "Own post");
        stubProfileData(currentUser, List.of(ownPost), List.of(), List.of(), List.of());

        CommunityPost post = firstModelPost(performProfile(currentUser), "myBonsaiPosts");

        assertThat(post.getAuthorName()).isEqualTo(currentUser.getUsername());
    }

    @Test
    void setAuthorAvatarForOwnPostsWhenAvatarExists() throws Exception {
        User currentUser = customer(13, "own-avatar");
        currentUser.setAvatar("https://cdn.example.com/avatar.png");
        CommunityPost ownPost = communityPost(104, currentUser.getUserId(), "Own post");
        stubProfileData(currentUser, List.of(ownPost), List.of(), List.of(), List.of());

        CommunityPost post = firstModelPost(performProfile(currentUser), "myBonsaiPosts");

        assertThat(post.getAuthorAvatar()).isEqualTo("https://cdn.example.com/avatar.png");
    }

    @Test
    void doNotSetAuthorAvatarWhenUserAvatarIsAbsent() throws Exception {
        User currentUser = customer(14, "no-own-avatar");
        currentUser.setAvatar(null);
        CommunityPost ownPost = communityPost(105, currentUser.getUserId(), "Own post");
        stubProfileData(currentUser, List.of(ownPost), List.of(), List.of(), List.of());

        CommunityPost post = firstModelPost(performProfile(currentUser), "myBonsaiPosts");

        assertThat(post.getAuthorAvatar()).isNull();
    }

    @Test
    void loadBookmarkedCommunityPosts() throws Exception {
        User currentUser = customer(20, "saved-posts");
        User author = customer(21, "saved-author");
        CommunityPost savedPost = communityPost(201, author.getUserId(), "Saved post");
        stubProfileData(currentUser, List.of(), List.of(bookmark(currentUser, savedPost)), List.of(), List.of());
        when(communityPostRepository.findAllById(List.of(savedPost.getPostId()))).thenReturn(List.of(savedPost));
        when(userRepository.findById(author.getUserId())).thenReturn(Optional.of(author));

        MvcResult result = performProfile(currentUser);

        assertThat(modelList(result, "savedPosts", CommunityPost.class))
                .extracting(CommunityPost::getPostId)
                .containsExactly(savedPost.getPostId());
    }

    @Test
    void returnEmptySavedPostListWhenUserHasNoBookmarks() throws Exception {
        User currentUser = customer(22, "no-bookmarks");
        stubProfileData(currentUser);

        MvcResult result = performProfile(currentUser);

        assertThat(modelList(result, "savedPosts", CommunityPost.class)).isEmpty();
    }

    @Test
    void populateBookmarkedPostAuthorInformation() throws Exception {
        User currentUser = customer(23, "bookmark-author-info");
        User author = customer(24, "bookmark-author");
        author.setFullName("Bookmark Author");
        author.setAvatar("https://cdn.example.com/bookmark-author.png");
        CommunityPost savedPost = communityPost(202, author.getUserId(), "Saved post");
        stubProfileData(currentUser, List.of(), List.of(bookmark(currentUser, savedPost)), List.of(), List.of());
        when(communityPostRepository.findAllById(List.of(savedPost.getPostId()))).thenReturn(List.of(savedPost));
        when(userRepository.findById(author.getUserId())).thenReturn(Optional.of(author));

        CommunityPost post = firstModelPost(performProfile(currentUser), "savedPosts");

        assertThat(post.getAuthorName()).isEqualTo("Bookmark Author");
        assertThat(post.getAuthorAvatar()).isEqualTo("https://cdn.example.com/bookmark-author.png");
    }

    @Test
    void skipSavedPostAuthorLookupWhenAuthorIdIsNull() throws Exception {
        User currentUser = customer(25, "null-author-bookmark");
        CommunityPost savedPost = communityPost(203, null, "Saved post without author");
        stubProfileData(currentUser, List.of(), List.of(bookmark(currentUser, savedPost)), List.of(), List.of());
        when(communityPostRepository.findAllById(List.of(savedPost.getPostId()))).thenReturn(List.of(savedPost));

        CommunityPost post = firstModelPost(performProfile(currentUser), "savedPosts");

        assertThat(post.getAuthorId()).isNull();
        assertThat(post.getAuthorName()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void loadCustomerOrderHistory() throws Exception {
        User currentUser = customer(30, "order-history");
        Order order = order(301, currentUser, "BSMS-AUTH-ORDER", LocalDateTime.now(), null);
        stubProfileData(currentUser, List.of(), List.of(), List.of(order), List.of());

        MvcResult result = performProfile(currentUser);

        assertThat(modelList(result, "orders", Order.class))
                .extracting(Order::getOrderId)
                .containsExactly(order.getOrderId());
    }

    @Test
    void buildReviewedProductIdSet() throws Exception {
        User currentUser = customer(31, "reviewed-products");
        Product product = product(401);
        Review review = Review.builder().reviewId(501).customer(currentUser).product(product).rating(5).build();
        stubProfileData(currentUser, List.of(), List.of(), List.of(), List.of(review));

        MvcResult result = performProfile(currentUser);

        assertThat(modelSet(result, "reviewedProductIds", Integer.class)).containsExactly(product.getProductId());
    }

    @Test
    void markOrderAsExpiredWhenCompletionTimeIsOlderThan30Days() throws Exception {
        User currentUser = customer(32, "expired-completed");
        Order expiredOrder = order(302, currentUser, "BSMS-OLD-COMPLETE",
                LocalDateTime.now().minusDays(40), LocalDateTime.now().minusDays(31));
        stubProfileData(currentUser, List.of(), List.of(), List.of(expiredOrder), List.of());

        MvcResult result = performProfile(currentUser);

        assertThat(modelSet(result, "expiredOrderIds", Integer.class)).contains(expiredOrder.getOrderId());
    }

    @Test
    void doNotMarkRecentlyCompletedOrderAsExpired() throws Exception {
        User currentUser = customer(33, "recent-completed");
        Order recentOrder = order(303, currentUser, "BSMS-RECENT-COMPLETE",
                LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(5));
        stubProfileData(currentUser, List.of(), List.of(), List.of(recentOrder), List.of());

        MvcResult result = performProfile(currentUser);

        assertThat(modelSet(result, "expiredOrderIds", Integer.class)).doesNotContain(recentOrder.getOrderId());
    }

    @Test
    void useOrderDateWhenCompletedTimeIsUnavailable() throws Exception {
        User currentUser = customer(34, "order-date-fallback");
        Order expiredOrder = order(304, currentUser, "BSMS-OLD-ORDER-DATE",
                LocalDateTime.now().minusDays(45), null);
        stubProfileData(currentUser, List.of(), List.of(), List.of(expiredOrder), List.of());

        MvcResult result = performProfile(currentUser);

        assertThat(modelSet(result, "expiredOrderIds", Integer.class)).contains(expiredOrder.getOrderId());
    }

    @Test
    void markOrderExpiredWhenBothCompletionAndOrderDatesAreNull() throws Exception {
        User currentUser = customer(35, "null-order-date");
        Order expiredOrder = order(305, currentUser, "BSMS-NULL-DATE", null, null);
        stubProfileData(currentUser, List.of(), List.of(), List.of(expiredOrder), List.of());

        MvcResult result = performProfile(currentUser);

        assertThat(modelSet(result, "expiredOrderIds", Integer.class)).contains(expiredOrder.getOrderId());
    }

    @Test
    void redirectToOrderHistorySectionOnProfilePage() throws Exception {
        User currentUser = customer(36, "orders-redirect");

        mockMvc.perform(get("/orders").with(formLoginUser(currentUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile#orderHistorySection"));
    }

    @Test
    void displayProfileUpdatePageForAuthenticatedUser() throws Exception {
        User currentUser = customer(40, "profile-update-get");
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));

        mockMvc.perform(get("/profile/update").with(formLoginUser(currentUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attribute("user", currentUser));
    }

    @Test
    void redirectUnauthenticatedUserFromProfileUpdatePage() throws Exception {
        mockMvc.perform(get("/profile/update"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void updateProfileInformationSuccessfullyWithoutAvatar() throws Exception {
        User currentUser = customer(41, "update-no-avatar");
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));

        mockMvc.perform(post("/profile/update")
                        .with(formLoginUser(currentUser))
                        .with(csrf())
                        .param("fullName", "Updated Profile Name")
                        .param("username", "updated_user_41")
                        .param("phone", "0912345678")
                        .param("address", "123 Test Street"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attributeExists("success"));

        assertThat(currentUser.getFullName()).isEqualTo("Updated Profile Name");
        assertThat(currentUser.getUsername()).isEqualTo("updated_user_41");
        assertThat(currentUser.getPhone()).isEqualTo("0912345678");
        assertThat(currentUser.getAddress()).isEqualTo("123 Test Street");
        verifyNoInteractions(cloudinaryStorageService);
    }

    @Test
    void uploadNewAvatarWhileUpdatingProfile() throws Exception {
        User currentUser = customer(42, "upload-avatar");
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(cloudinaryStorageService.uploadImage(any(), eq(CloudinaryFolder.AVATAR)))
                .thenReturn(new CloudinaryUploadResponse("https://cdn.example.com/new-avatar.png", "avatars/new", "image"));

        mockMvc.perform(multipart("/profile/update")
                        .file(avatarFile())
                        .with(formLoginUser(currentUser))
                        .with(csrf())
                        .param("fullName", currentUser.getFullName())
                        .param("username", currentUser.getUsername())
                        .param("phone", currentUser.getPhone())
                        .param("address", currentUser.getAddress()))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attributeExists("success"));

        assertThat(currentUser.getAvatar()).isEqualTo("https://cdn.example.com/new-avatar.png");
        assertThat(currentUser.getAvatarPublicId()).isEqualTo("avatars/new");
        verify(cloudinaryStorageService).uploadImage(any(), eq(CloudinaryFolder.AVATAR));
    }

    @Test
    void replaceExistingAvatarAndDeletePreviousCloudinaryImage() throws Exception {
        User currentUser = customer(43, "replace-avatar");
        currentUser.setAvatar("https://cdn.example.com/old-avatar.png");
        currentUser.setAvatarPublicId("avatars/old");
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(cloudinaryStorageService.uploadImage(any(), eq(CloudinaryFolder.AVATAR)))
                .thenReturn(new CloudinaryUploadResponse("https://cdn.example.com/replaced-avatar.png", "avatars/replaced", "image"));

        mockMvc.perform(multipart("/profile/update")
                        .file(avatarFile())
                        .with(formLoginUser(currentUser))
                        .with(csrf())
                        .param("fullName", currentUser.getFullName())
                        .param("username", currentUser.getUsername())
                        .param("phone", currentUser.getPhone())
                        .param("address", currentUser.getAddress()))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile_update"))
                .andExpect(model().attributeExists("success"));

        assertThat(currentUser.getAvatar()).isEqualTo("https://cdn.example.com/replaced-avatar.png");
        assertThat(currentUser.getAvatarPublicId()).isEqualTo("avatars/replaced");
        verify(cloudinaryStorageService).deleteFile("avatars/old", "image");
    }

    @Test
    void redirectUnauthenticatedProfileUpdateRequestToLogin() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .with(csrf())
                        .param("fullName", "Unauthenticated User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private MvcResult performProfile(User currentUser) throws Exception {
        return mockMvc.perform(get("/profile").with(formLoginUser(currentUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/profile"))
                .andReturn();
    }

    private void stubProfileData(User currentUser) {
        stubProfileData(currentUser, List.of(), List.of(), List.of(), List.of());
    }

    private void stubProfileData(User currentUser,
                                 List<CommunityPost> ownPosts,
                                 List<CommunityPostBookmark> bookmarks,
                                 List<Order> orders,
                                 List<Review> reviews) {
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(communityPostRepository.findByAuthorIdOrderByCreatedAtDesc(currentUser.getUserId())).thenReturn(ownPosts);
        when(bookmarkRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getUserId())).thenReturn(bookmarks);
        when(orderService.getOrdersByCustomerId(currentUser.getUserId())).thenReturn(orders);
        when(reviewRepository.findByCustomerUserId(currentUser.getUserId())).thenReturn(reviews);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor formLoginUser(User currentUser) {
        CustomUserDetails userDetails = new CustomUserDetails(
                currentUser,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        return authenticatedPrincipal(userDetails, userDetails.getAuthorities());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor formLoginEmail(String email) {
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        email,
                        "encoded-password",
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                );
        return authenticatedPrincipal(userDetails, userDetails.getAuthorities());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor oauth2User(String email) {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                Map.of("email", email),
                "email"
        );
        return authenticatedPrincipal(principal, principal.getAuthorities());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor oauth2UserWithoutEmail() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                Map.of("sub", "oauth-user-without-email"),
                "sub"
        );
        return authenticatedPrincipal(principal, principal.getAuthorities());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedPrincipal(
            Object principal,
            Collection<? extends org.springframework.security.core.GrantedAuthority> authorities) {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "n/a", authorities));
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    private User customer(int id, String label) {
        return User.builder()
                .userId(id)
                .fullName("Customer " + label)
                .username("user_" + label.replaceAll("[^A-Za-z0-9]", "_"))
                .email(label.replaceAll("[^A-Za-z0-9]", "_") + "@example.com")
                .password("encoded-password")
                .phone("0901234567")
                .address("Default address")
                .role(Role.builder().roleId(1).roleName("ROLE_CUSTOMER").build())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CommunityPost communityPost(Integer postId, Integer authorId, String title) {
        return CommunityPost.builder()
                .postId(postId)
                .authorId(authorId)
                .title(title)
                .content("Content")
                .summary("Summary")
                .category("Community")
                .status("APPROVED")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CommunityPostBookmark bookmark(User user, CommunityPost post) {
        return CommunityPostBookmark.builder()
                .id(post.getPostId() + 1000)
                .userId(user.getUserId())
                .postId(post.getPostId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Order order(Integer id, User customer, String code, LocalDateTime orderDate, LocalDateTime completedAt) {
        return Order.builder()
                .orderId(id)
                .customer(customer)
                .orderCode(code)
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhone())
                .shippingAddress(customer.getAddress())
                .orderDate(orderDate)
                .completedAt(completedAt)
                .orderStatus("COMPLETED")
                .orderType("ONLINE")
                .totalAmount(new BigDecimal("1000000"))
                .build();
    }

    private Product product(Integer id) {
        return Product.builder()
                .productId(id)
                .productCode("AUTH-PROD-" + id)
                .productName("Auth Product " + id)
                .price(new BigDecimal("1500000"))
                .productStatus("AVAILABLE")
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private MockMultipartFile avatarFile() {
        return new MockMultipartFile(
                "avatarFile",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image".getBytes()
        );
    }

    private CommunityPost firstModelPost(MvcResult result, String attributeName) {
        return modelList(result, attributeName, CommunityPost.class).getFirst();
    }

    private <T> List<T> modelList(MvcResult result, String attributeName, Class<T> elementType) {
        Object value = result.getModelAndView().getModel().get(attributeName);

        assertThat(value).isInstanceOf(List.class);
        return ((List<?>) value).stream()
                .map(elementType::cast)
                .toList();
    }

    private <T> Set<T> modelSet(MvcResult result, String attributeName, Class<T> elementType) {
        Object value = result.getModelAndView().getModel().get(attributeName);

        assertThat(value).isInstanceOf(Set.class);
        return ((Collection<?>) value).stream()
                .map(elementType::cast)
                .collect(java.util.stream.Collectors.toSet());
    }
}
