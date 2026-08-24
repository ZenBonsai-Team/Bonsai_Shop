package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.product.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.bonsai_shop.customer.service.EmailService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BF10ProductControllerCoverageSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository segmentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductMediaRepository productMediaRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User customerUserEntity;
    private User artisanUserEntity;
    private User moderatorUserEntity;
    private Product testProduct;
    private Role customerRole;
    private Role artisanRole;
    private Role moderatorRole;
    private ProductSegment standardSegment;

    @BeforeEach
    void setUp() {
        customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_CUSTOMER").description("Customer").build()));

        artisanRole = roleRepository.findByRoleName("ROLE_ARTISAN")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_ARTISAN").description("Artisan").build()));

        moderatorRole = roleRepository.findByRoleName("ROLE_MODERATOR")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_MODERATOR").description("Moderator").build()));

        customerUserEntity = findOrCreateUser("customer.bf10@test.com", "customer.bf10", customerRole);
        artisanUserEntity = findOrCreateUser("artisan.bf10@test.com", "artisan.bf10", artisanRole);
        moderatorUserEntity = findOrCreateUser("moderator.bf10@test.com", "moderator.bf10", moderatorRole);

        Category category = categoryRepository.save(Category.builder().categoryName("BF10 Cat").build());
        Variety variety = varietyRepository.save(Variety.builder().category(category).varietyName("BF10 Variety").build());
        standardSegment = segmentRepository.save(ProductSegment.builder().segmentName("BF10 Standard").build());

        testProduct = productRepository.save(Product.builder()
                .productName("BF10 Bonsai")
                .productCode("BF10-01")
                .productStatus("AVAILABLE")
                .isVisible(true)
                .segment(standardSegment)
                .variety(variety)
                .price(new BigDecimal("1200000"))
                .age(8)
                .height(45.0f)
                .trunkDiameter(6.0f)
                .style("Informal")
                .createdBy(artisanUserEntity)
                .createdAt(LocalDateTime.now())
                .isPublicPrice(true)
                .viewCount(0)
                .build());
    }

    private RequestPostProcessor customerUser() {
        return user(new CustomUserDetails(customerUserEntity, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private RequestPostProcessor moderatorUser() {
        return user(new CustomUserDetails(moderatorUserEntity, List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"))));
    }

    private User findOrCreateUser(String email, String username, Role role) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName(username + " Test")
                        .email(email)
                        .username(username)
                        .password("123456")
                        .phone("0912345600")
                        .status("ACTIVE")
                        .role(role)
                        .build()));
    }

    // ======================== REVIEW API TESTS ========================

    @Test
    void testReviewApi_GetReviewsAndCanReview() throws Exception {
        // GET product reviews (public)
        mockMvc.perform(get("/api/reviews/" + testProduct.getProductId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // GET can-review for anonymous
        mockMvc.perform(get("/api/reviews/" + testProduct.getProductId() + "/can-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canReview", is(false)));

        // GET can-review for logged in customer (not purchased yet)
        mockMvc.perform(get("/api/reviews/" + testProduct.getProductId() + "/can-review")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canReview", is(false)));
    }

    @Test
    void testReviewApi_SubmitReview_FailuresAndSuccess() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("rating", 5);
        body.put("comment", "Tuyệt vời!");

        // Submit review without login -> 401
        mockMvc.perform(post("/api/reviews/" + testProduct.getProductId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());

        // Submit review with invalid rating -> 400
        body.put("rating", 6);
        mockMvc.perform(post("/api/reviews/" + testProduct.getProductId())
                        .with(customerUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Đánh giá phải từ 1 đến 5 sao")));

        // Submit review when not purchased -> 400
        body.put("rating", 4);
        mockMvc.perform(post("/api/reviews/" + testProduct.getProductId())
                        .with(customerUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Bạn không thể đánh giá sản phẩm này")));
    }

    @Test
    void testReviewApi_ApproveAndReject_Success() throws Exception {
        // Tạo review chờ duyệt
        Review review = reviewRepository.save(Review.builder()
                .customer(customerUserEntity)
                .product(testProduct)
                .rating(5)
                .comment("Chờ duyệt")
                .reviewStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build());

        // Mod approve review
        mockMvc.perform(put("/api/reviews/" + review.getReviewId() + "/approve")
                        .with(moderatorUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Mod reject review
        mockMvc.perform(put("/api/reviews/" + review.getReviewId() + "/reject")
                        .with(moderatorUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    // ======================== CART API TESTS ========================

    @Test
    void testCartApi_GetAndAddAndRemoveItems() throws Exception {
        // GET cart without login -> 401
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());

        // GET cart logged in -> 200
        mockMvc.perform(get("/api/cart")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // POST add to cart without login -> 401
        Map<String, Integer> payload = Map.of("productId", testProduct.getProductId());
        mockMvc.perform(post("/api/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        // POST add to cart logged in -> 200
        mockMvc.perform(post("/api/cart/items")
                        .with(customerUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // DELETE from cart without login -> 401
        mockMvc.perform(delete("/api/cart/items/" + testProduct.getProductId())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        // DELETE from cart logged in -> 200
        mockMvc.perform(delete("/api/cart/items/" + testProduct.getProductId())
                        .with(customerUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testCartApi_SyncGuestCart() throws Exception {
        List<Integer> productIds = List.of(testProduct.getProductId());

        mockMvc.perform(post("/api/cart/sync")
                        .with(customerUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // ======================== WISHLIST API TESTS ========================

    @Test
    void testWishlistApi_AllEndpoints() throws Exception {
        // GET wishlist without login -> 401
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isUnauthorized());

        // GET wishlist logged in -> 200
        mockMvc.perform(get("/api/wishlist")
                        .with(customerUser()))
                .andExpect(status().isOk());

        // GET count wishlist logged in -> 200
        mockMvc.perform(get("/api/wishlist/count")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(notNullValue())));

        // POST add items wishlist -> 200
        Map<String, Integer> payload = Map.of("productId", testProduct.getProductId());
        mockMvc.perform(post("/api/wishlist/items")
                        .with(customerUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // POST toggle wishlist -> 200
        mockMvc.perform(post("/api/wishlist/toggle")
                        .with(customerUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // DELETE remove items wishlist -> 200
        mockMvc.perform(delete("/api/wishlist/items/" + testProduct.getProductId())
                        .with(customerUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // ======================== PREMIUM BONSAI TESTS ========================

    @Test
    void testPremiumBonsai_ListAndDetail() throws Exception {
        // Lấy danh sách premium bonsai
        mockMvc.perform(get("/bonsai-luxury")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/bonsai-luxury"))
                .andExpect(model().attributeExists("product"));

        // Lấy chi tiết premium (ID không tồn tại -> redirect)
        mockMvc.perform(get("/bonsai-luxury-detail/999999")
                        .with(customerUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bonsai-luxury"));
    }

    // ======================== PAYMENT TESTS ========================

    @Test
    void testPayment_CreatePaymentRedirectToVNPay() throws Exception {
        mockMvc.perform(get("/vnpay/create-payment")
                        .param("productId", String.valueOf(testProduct.getProductId()))
                        .with(customerUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html")));
    }

    @Test
    void testPayment_CallbackVNPay() throws Exception {
        // Tạo đơn hàng test
        Order testOrder = orderRepository.save(Order.builder()
                .orderCode("BF10VNP")
                .customer(customerUserEntity)
                .totalAmount(new BigDecimal("1200000"))
                .orderStatus("PENDING")
                .shippingAddress("Ha Noi")
                .customerPhone("0912345678")
                .customerEmail("customer.bf10@test.com")
                .orderDate(LocalDateTime.now())
                .build());

        // Test VNPay callback (response success "00")
        mockMvc.perform(get("/vnpay/payment-callback")
                        .param("vnp_TxnRef", testOrder.getOrderCode())
                        .param("vnp_Amount", "120000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_SecureHash", "fakeHash")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-result"));

        // Test VNPay callback (response error "01")
        mockMvc.perform(get("/vnpay/payment-callback")
                        .param("vnp_TxnRef", testOrder.getOrderCode())
                        .param("vnp_Amount", "120000000")
                        .param("vnp_ResponseCode", "01")
                        .param("vnp_TransactionStatus", "01")
                        .param("vnp_SecureHash", "fakeHash")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-result"));
    }

    // ======================== ADDITIONAL OPTIMIZATIONS ========================

    @Test
    void testCartApi_AddSoldProduct_ShouldFail() throws Exception {
        // Tạo sản phẩm trạng thái SOLD
        Product soldProduct = productRepository.save(Product.builder()
                .productName("BF10 Sold Bonsai")
                .productCode("BF10-SOLD")
                .productStatus("SOLD")
                .isVisible(true)
                .segment(standardSegment)
                .variety(testProduct.getVariety())
                .price(new BigDecimal("1200000"))
                .age(8)
                .height(45.0f)
                .trunkDiameter(6.0f)
                .createdBy(artisanUserEntity)
                .createdAt(LocalDateTime.now())
                .isPublicPrice(true)
                .build());

        Map<String, Integer> payload = Map.of("productId", soldProduct.getProductId());
        mockMvc.perform(post("/api/cart/items")
                        .with(customerUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testReviewApi_ApproveRejectNonExistent_ShouldFail() throws Exception {
        mockMvc.perform(put("/api/reviews/999999/approve")
                        .with(moderatorUser()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(notNullValue())));

        mockMvc.perform(put("/api/reviews/999999/reject")
                        .with(moderatorUser()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(notNullValue())));
    }

    @Test
    void testPremiumBonsai_DetailWithMediaSlots() throws Exception {
        // Tạo sản phẩm Premium
        Product premiumProduct = productRepository.save(Product.builder()
                .productName("BF10 Premium Bonsai")
                .productCode("BF10-PREM")
                .productStatus("AVAILABLE")
                .isVisible(true)
                .segment(segmentRepository.findById(3).orElse(standardSegment))
                .variety(testProduct.getVariety())
                .price(new BigDecimal("15000000"))
                .age(15)
                .height(80.0f)
                .trunkDiameter(12.0f)
                .createdBy(artisanUserEntity)
                .createdAt(LocalDateTime.now())
                .isPublicPrice(true)
                .build());

        // Gắn media slots
        productMediaRepository.save(ProductMedia.builder().product(premiumProduct).mediaType("IMAGE").slotType("FRONT").mediaUrl("http://front").displayOrder(1).build());
        productMediaRepository.save(ProductMedia.builder().product(premiumProduct).mediaType("IMAGE").slotType("LEFT").mediaUrl("http://left").displayOrder(2).build());
        productMediaRepository.save(ProductMedia.builder().product(premiumProduct).mediaType("IMAGE").slotType("BACK").mediaUrl("http://back").displayOrder(3).build());
        productMediaRepository.save(ProductMedia.builder().product(premiumProduct).mediaType("IMAGE").slotType("OTHER").mediaUrl("http://other").displayOrder(4).build());
        productMediaRepository.save(ProductMedia.builder().product(premiumProduct).mediaType("VIDEO").mediaUrl("http://video").displayOrder(1).build());

        mockMvc.perform(get("/bonsai-luxury-detail/" + premiumProduct.getProductId())
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/bonsai-luxury-detail"))
                .andExpect(model().attributeExists("imageMediaList"))
                .andExpect(model().attributeExists("videoMediaList"));
    }

    // ======================== ORDER API TESTS ========================

    @Test
    void testOrderApi_AllEndpoints() throws Exception {
        // 1. send-guest-otp
        Map<String, Object> otpPayload = new HashMap<>();
        otpPayload.put("email", "guest.bf10@test.com");
        otpPayload.put("productIds", List.of(testProduct.getProductId()));

        mockMvc.perform(post("/api/orders/send-guest-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 2. get orders list (Moderator)
        mockMvc.perform(get("/api/orders")
                        .with(moderatorUser()))
                .andExpect(status().isOk());

        // 3. get details
        Order order = orderRepository.save(Order.builder()
                .orderCode("BF10ORD")
                .customer(customerUserEntity)
                .totalAmount(new BigDecimal("1200000"))
                .orderStatus("PENDING")
                .shippingAddress("Ha Noi")
                .customerPhone("0912345678")
                .customerEmail("customer.bf10@test.com")
                .orderDate(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/orders/" + order.getOrderCode())
                        .with(moderatorUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCode", is(order.getOrderCode())));

        // get non-existent order details -> 404
        mockMvc.perform(get("/api/orders/NON_EXISTENT")
                        .with(moderatorUser()))
                .andExpect(status().isNotFound());

        // 4. claim order
        mockMvc.perform(post("/api/orders/" + order.getOrderCode() + "/claim")
                        .with(moderatorUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 5. unclaim order
        mockMvc.perform(post("/api/orders/" + order.getOrderCode() + "/unclaim")
                        .with(moderatorUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Gán lại đơn hàng để test reject (phải được gán cho moderator)
        mockMvc.perform(post("/api/orders/" + order.getOrderCode() + "/claim")
                        .with(moderatorUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 6. reject order
        Map<String, String> rejectPayload = Map.of("reason", "Từ chối đơn hàng test");
        mockMvc.perform(post("/api/orders/" + order.getOrderCode() + "/reject")
                        .with(moderatorUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 7. verify order (duyệt đơn và áp phí)
        order.setOrderStatus("PENDING");
        order.setAssignedTo(moderatorUserEntity);
        orderRepository.save(order);

        Map<String, Object> verifyPayload = new HashMap<>();
        verifyPayload.put("craneFee", 100000);
        verifyPayload.put("shippingFee", 50000);
        verifyPayload.put("depositAmount", 500000);

        mockMvc.perform(post("/api/orders/" + order.getOrderCode() + "/verify")
                        .with(moderatorUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 8. confirm-remaining-payment (xác nhận thu đủ tiền đợt 2)
        order.setOrderStatus("DEPOSITED");
        orderRepository.save(order);

        Map<String, String> remainingPayload = Map.of("notes", "Đã thu đủ tiền mặt.");
        mockMvc.perform(post("/api/orders/" + order.getOrderCode() + "/confirm-remaining-payment")
                        .with(moderatorUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remainingPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 9. complete order (hoàn tất đơn hàng đã thanh toán 100%)
        order.setOrderStatus("PAID");
        orderRepository.save(order);

        mockMvc.perform(post("/api/orders/" + order.getOrderCode() + "/complete")
                        .with(moderatorUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 10. get pool orders
        mockMvc.perform(get("/api/orders/pool")
                        .with(moderatorUser()))
                .andExpect(status().isOk());

        // 11. get my orders
        mockMvc.perform(get("/api/orders/my")
                        .with(moderatorUser()))
                .andExpect(status().isOk());

        // 12. get my stats
        mockMvc.perform(get("/api/orders/my-stats")
                        .with(moderatorUser()))
                .andExpect(status().isOk());

        // 13. get KPIs
        mockMvc.perform(get("/api/orders/kpis")
                        .with(moderatorUser()))
                .andExpect(status().isOk());

        // 14. customer-no-show
        order.setOrderStatus("DEPOSITED");
        orderRepository.save(order);

        paymentRepository.save(Payment.builder()
                .order(order)
                .paymentMethod("VNPAY")
                .paymentStatus("SUCCESS")
                .paymentType("DEPOSIT")
                .amount(new BigDecimal("500000"))
                .build());

        Map<String, String> noShowPayload = Map.of("notes", "Khách không nhận.");
        mockMvc.perform(post("/api/orders/" + order.getOrderCode() + "/customer-no-show")
                        .with(moderatorUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noShowPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
