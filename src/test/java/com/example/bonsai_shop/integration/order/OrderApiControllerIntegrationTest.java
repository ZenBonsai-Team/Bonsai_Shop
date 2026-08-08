package com.example.bonsai_shop.integration.order;

import com.example.bonsai_shop.customer.repository.RegisterOtpRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.integration.support.BaseControllerIntegrationTest;
import com.example.bonsai_shop.product.dto.PurchaseOrderRequestDTO;
import com.example.bonsai_shop.product.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

public class OrderApiControllerIntegrationTest extends BaseControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RegisterOtpRepository registerOtpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository productSegmentRepository;

    private User createTestUser(String email, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(roleName).description(roleName).build()));
        User user = new User();
        user.setEmail(email);
        user.setFullName("Test " + roleName);
        user.setPhone("0987654321");
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

    private Product createTestProduct(String code, String name, BigDecimal price) {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().categoryName("General Category").build()));
        Variety variety = varietyRepository.findAll().stream().findFirst()
                .orElseGet(() -> varietyRepository.save(Variety.builder().category(category).varietyName("General Variety").build()));
        ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder().segmentName("General Segment").build()));

        Product product = new Product();
        product.setProductCode(code);
        product.setProductName(name);
        product.setPrice(price);
        product.setProductStatus("AVAILABLE");
        product.setVariety(variety);
        product.setSegment(segment);
        return productRepository.save(product);
    }

    @DisplayName("TC-IT-ORD-01: Gửi OTP thành công cho khách vãng lai")
    @Test
    void testSendGuestOtpSuccess() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "guest.test@example.com");
        payload.put("productIds", Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/send-guest-otp").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Optional<PasswordResetOtp> otpOpt = registerOtpRepository.findTopByEmailOrderByCreatedAtDesc("guest.test@example.com");
        assertTrue(otpOpt.isPresent());
        assertFalse(otpOpt.get().getIsUsed());
    }

    @DisplayName("TC-IT-ORD-02: Vi phạm rate limit / OTP spam (60s cooldown)")
    @Test
    void testSendGuestOtpRateLimit() throws Exception {
        PasswordResetOtp recentOtp = new PasswordResetOtp();
        recentOtp.setEmail("spam.guest@example.com");
        recentOtp.setOtpCode("123456");
        recentOtp.setCreatedAt(LocalDateTime.now());
        recentOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        recentOtp.setIsUsed(false);
        registerOtpRepository.save(recentOtp);

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "spam.guest@example.com");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/send-guest-otp").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests());
    }

    @DisplayName("TC-IT-ORD-03: Logged-in Customer Checkout thành công")
    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void testCustomerCheckoutSuccess() throws Exception {
        createTestUser("customer@example.com", "CUSTOMER");
        Product product = createTestProduct("TREE-001", "Tùng La Hán Top", new BigDecimal("1000000"));

        PurchaseOrderRequestDTO requestDTO = new PurchaseOrderRequestDTO();
        requestDTO.setCustomerName("Customer Test");
        requestDTO.setCustomerPhone("0987654321");
        requestDTO.setCustomerEmail("customer@example.com");
        requestDTO.setShippingAddress("123 Test Street");
        requestDTO.setPaymentMethod("COD");
        requestDTO.setProductIds(List.of(product.getProductId()));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/checkout").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.orderCode").exists());

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("RESERVED", updatedProduct.getProductStatus());
    }

    @DisplayName("TC-IT-ORD-04: Guest Checkout thành công với OTP đúng")
    @Test
    void testGuestCheckoutWithValidOtp() throws Exception {
        Product product = createTestProduct("TREE-002", "Sanh Cổ Nét", new BigDecimal("2000000"));

        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail("guest.checkout@example.com");
        otp.setOtpCode("654321");
        otp.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        otp.setIsUsed(false);
        registerOtpRepository.save(otp);

        PurchaseOrderRequestDTO requestDTO = new PurchaseOrderRequestDTO();
        requestDTO.setCustomerName("Guest User");
        requestDTO.setCustomerPhone("0912345678");
        requestDTO.setCustomerEmail("guest.checkout@example.com");
        requestDTO.setShippingAddress("456 Guest Way");
        requestDTO.setPaymentMethod("VNPAY");
        requestDTO.setOtpCode("654321");
        requestDTO.setProductIds(List.of(product.getProductId()));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/checkout").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.orderCode").exists());
    }

    @DisplayName("TC-IT-ORD-05: Guest Checkout với OTP sai hoặc hết hạn")
    @Test
    void testGuestCheckoutWithInvalidOtp() throws Exception {
        Product product = createTestProduct("TREE-003", "Mai Chế", new BigDecimal("500000"));

        PurchaseOrderRequestDTO requestDTO = new PurchaseOrderRequestDTO();
        requestDTO.setCustomerName("Guest User Invalid OTP");
        requestDTO.setCustomerPhone("0912345678");
        requestDTO.setCustomerEmail("guest.invalid@example.com");
        requestDTO.setShippingAddress("789 Invalid St");
        requestDTO.setPaymentMethod("VNPAY");
        requestDTO.setOtpCode("999999");
        requestDTO.setProductIds(List.of(product.getProductId()));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/checkout").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @DisplayName("TC-IT-ORD-06: Checkout thiếu trường bắt buộc (Bad Request)")
    @Test
    void testCheckoutMissingRequiredFields() throws Exception {
        PurchaseOrderRequestDTO requestDTO = new PurchaseOrderRequestDTO();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/checkout").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @DisplayName("TC-IT-ORD-08: DEFECT-SEC-01 Characterization Test - Public GET /api/orders")
    @Test
    void testGetOrdersPublicExposure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @DisplayName("TC-IT-ORD-09: DEFECT-SEC-02 Characterization Test - Public GET /api/orders/pool")
    @Test
    void testGetPoolOrdersPublicExposure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/pool"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @DisplayName("TC-IT-ORD-10: DEFECT-SEC-03 Characterization Test - Public GET /api/orders/kpis")
    @Test
    void testGetKpisPublicExposure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/kpis"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @DisplayName("TC-IT-ORD-11: Moderator lấy danh sách My Orders & Stats")
    @Test
    @WithMockUser(username = "mod1@example.com", roles = {"MODERATOR"})
    void testGetMyOrdersAndStats() throws Exception {
        createTestUser("mod1@example.com", "MODERATOR");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/my"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/my-stats"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @DisplayName("TC-IT-ORD-12: DEFECT-SEC-04 Characterization Test - GET /api/orders/{code}")
    @Test
    void testGetOrderDetailByCode() throws Exception {
        Order order = new Order();
        order.setOrderCode("ORD-TEST-12");
        order.setOrderStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/ORD-TEST-12"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.orderCode").value("ORD-TEST-12"));
    }

    @DisplayName("TC-IT-ORD-13: Moderator Claim Order từ Pool")
    @Test
    @WithMockUser(username = "mod.claim@example.com", roles = {"MODERATOR"})
    void testClaimOrderSuccess() throws Exception {
        User modUser = createTestUser("mod.claim@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-CLAIM-13");
        order.setOrderStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/ORD-CLAIM-13/claim").with(csrf()))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Order updatedOrder = orderRepository.findByOrderCode("ORD-CLAIM-13").orElseThrow();
        assertNotNull(updatedOrder.getAssignedTo());
        assertEquals(modUser.getUserId(), updatedOrder.getAssignedTo().getUserId());
    }

    @DisplayName("TC-IT-ORD-14: Moderator B cố unclaim đơn của Moderator A (Bad Request)")
    @Test
    @WithMockUser(username = "modB@example.com", roles = {"MODERATOR"})
    void testUnclaimOrderByWrongModerator() throws Exception {
        User modA = createTestUser("modA@example.com", "MODERATOR");
        createTestUser("modB@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-UNCLAIM-14");
        order.setOrderStatus("PENDING");
        order.setAssignedTo(modA);
        order.setAssignedAt(LocalDateTime.now());
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/ORD-UNCLAIM-14/unclaim").with(csrf()))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @DisplayName("TC-IT-ORD-15: Moderator Verify Order & gán phí ship, cẩu, cọc")
    @Test
    @WithMockUser(username = "mod.verify@example.com", roles = {"MODERATOR"})
    void testVerifyOrderSuccess() throws Exception {
        User mod = createTestUser("mod.verify@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-VERIFY-15");
        order.setOrderStatus("PENDING");
        order.setAssignedTo(mod);
        order.setTotalAmount(new BigDecimal("1000000"));
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, Object> payload = new HashMap<>();
        payload.put("shippingFee", 50000);
        payload.put("craneFee", 100000);
        payload.put("depositAmount", 300000);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/ORD-VERIFY-15/verify").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Order updated = orderRepository.findByOrderCode("ORD-VERIFY-15").orElseThrow();
        assertEquals("PENDING_PAYMENT", updated.getOrderStatus());
    }

    @DisplayName("TC-IT-ORD-16: Verify Order với phí/cọc không hợp lệ (Bad Request)")
    @Test
    @WithMockUser(username = "mod.verify.bad@example.com", roles = {"MODERATOR"})
    void testVerifyOrderInvalidFees() throws Exception {
        User mod = createTestUser("mod.verify.bad@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-VERIFY-16");
        order.setOrderStatus("PENDING");
        order.setAssignedTo(mod);
        order.setTotalAmount(new BigDecimal("1000000"));
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, Object> payload = new HashMap<>();
        payload.put("shippingFee", -10000); // Invalid negative fee

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/ORD-VERIFY-16/verify").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @DisplayName("TC-IT-ORD-17: Moderator Reject Order kèm lý do")
    @Test
    @WithMockUser(username = "mod.reject@example.com", roles = {"MODERATOR"})
    void testRejectOrderSuccess() throws Exception {
        User mod = createTestUser("mod.reject@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-REJECT-17");
        order.setOrderStatus("PENDING");
        order.setAssignedTo(mod);
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, Object> payload = new HashMap<>();
        payload.put("reason", "Cây hư hỏng trong kho");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/ORD-REJECT-17/reject").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Order updated = orderRepository.findByOrderCode("ORD-REJECT-17").orElseThrow();
        assertEquals("CANCELLED", updated.getOrderStatus());
    }

    @DisplayName("TC-IT-ORD-18: Moderator Complete Order đã trả tiền")
    @Test
    @WithMockUser(username = "mod.complete@example.com", roles = {"MODERATOR"})
    void testCompletePaidOrderSuccess() throws Exception {
        User mod = createTestUser("mod.complete@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-COMPLETE-18");
        order.setOrderStatus("PAID");
        order.setAssignedTo(mod);
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/ORD-COMPLETE-18/complete").with(csrf()))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Order updated = orderRepository.findByOrderCode("ORD-COMPLETE-18").orElseThrow();
        assertEquals("COMPLETED", updated.getOrderStatus());
    }

    @DisplayName("TC-IT-ORD-19: Moderator Confirm Remaining Payment nấc 2")
    @Test
    @WithMockUser(username = "mod.confirm@example.com", roles = {"MODERATOR"})
    void testConfirmRemainingPaymentSuccess() throws Exception {
        User mod = createTestUser("mod.confirm@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-CONFIRM-19");
        order.setOrderStatus("DEPOSITED");
        order.setTotalAmount(new BigDecimal("2000000"));
        order.setDepositAmount(new BigDecimal("500000"));
        order.setShippingFee(BigDecimal.ZERO);
        order.setCraneFee(BigDecimal.ZERO);
        order.setAssignedTo(mod);
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", 1500000);
        payload.put("paymentMethod", "CASH");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/ORD-CONFIRM-19/confirm-remaining-payment").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Order updated = orderRepository.findByOrderCode("ORD-CONFIRM-19").orElseThrow();
        assertEquals("COMPLETED", updated.getOrderStatus());
    }

    @DisplayName("TC-IT-ORD-20: Moderator xử lý Customer No-Show (bùng cọc)")
    @Test
    @WithMockUser(username = "mod.noshow@example.com", roles = {"MODERATOR"})
    void testMarkCustomerNoShowSuccess() throws Exception {
        User mod = createTestUser("mod.noshow@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-NOSHOW-20");
        order.setOrderStatus("DEPOSITED");
        order.setDepositAmount(new BigDecimal("500000"));
        order.setAssignedTo(mod);
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        Payment deposit = new Payment();
        deposit.setOrder(savedOrder);
        deposit.setAmount(new BigDecimal("500000"));
        deposit.setPaymentStatus("SUCCESS");
        deposit.setPaymentType("DEPOSIT");
        paymentRepository.save(deposit);

        Map<String, Object> payload = new HashMap<>();
        payload.put("notes", "Khách quá 48h không đến nhận cây");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/ORD-NOSHOW-20/customer-no-show").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Order updated = orderRepository.findByOrderCode("ORD-NOSHOW-20").orElseThrow();
        assertEquals("CANCELLED", updated.getOrderStatus());
    }
}
