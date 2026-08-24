package com.example.bonsai_shop.system;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.customer.repository.RegisterOtpRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.customer.service.EmailService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.PasswordResetOtp;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.integration.support.TestDatabaseSafetyInitializer;
import com.example.bonsai_shop.product.dto.PurchaseOrderRequestDTO;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.OrderDetailRepository;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import com.example.bonsai_shop.product.service.MailService;
import com.example.bonsai_shop.product.service.OrderExpirationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * L3 SYSTEM TEST (HTTP Flow Suite via MockMvc - Batch 1, Batch 2 & Batch 3)
 * Feature: BF-01 Marketplace Order
 * Cases: TC-HTTP-BF01-001 through TC-HTTP-BF01-012
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
public class BF01MarketplaceOrderHttpFlowSystemTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private EmailService emailService;

        @MockitoBean
        private MailService mailService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private VarietyRepository varietyRepository;

        @Autowired
        private ProductSegmentRepository productSegmentRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private OrderDetailRepository orderDetailRepository;

        @Autowired
        private PaymentRepository paymentRepository;

        @Autowired
        private OrderLogRepository orderLogRepository;

        @Autowired
        private OrderHandlingRepository orderHandlingRepository;

        @Autowired
        private FinancialLedgerRepository financialLedgerRepository;

        @Autowired
        private RegisterOtpRepository registerOtpRepository;

        @Autowired
        private OrderExpirationService orderExpirationService;

        private User moderatorA;
        private User moderatorB;
        private User customer;
        private Category category;
        private Variety variety;
        private ProductSegment segment;

        private List<String> createdOrderCodes = new ArrayList<>();
        private List<Integer> createdProductIds = new ArrayList<>();

        @BeforeEach
        void setUp() {
                Role moderatorRole = roleRepository.findByRoleName("MODERATOR")
                                .orElseGet(() -> roleRepository.save(Role.builder().roleName("MODERATOR").build()));

                Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CUSTOMER").build()));

                moderatorA = userRepository.findByEmail("moderator.a.http@test.com")
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .fullName("Order Moderator A (HTTP Flow)")
                                                .email("moderator.a.http@test.com")
                                                .username("moderator_a_http")
                                                .password(passwordEncoder.encode("password123"))
                                                .role(moderatorRole)
                                                .status("ACTIVE")
                                                .phone("0901234567")
                                                .build()));

                moderatorB = userRepository.findByEmail("moderator.b.http@test.com")
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .fullName("Order Moderator B (HTTP Flow)")
                                                .email("moderator.b.http@test.com")
                                                .username("moderator_b_http")
                                                .password(passwordEncoder.encode("password123"))
                                                .role(moderatorRole)
                                                .status("ACTIVE")
                                                .phone("0907654321")
                                                .build()));

                customer = userRepository.findByEmail("customer.http@test.com")
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .fullName("Customer HTTP Flow")
                                                .email("customer.http@test.com")
                                                .username("customer_http")
                                                .password(passwordEncoder.encode("password123"))
                                                .role(customerRole)
                                                .status("ACTIVE")
                                                .phone("0988776655")
                                                .build()));

                category = categoryRepository.findAll().stream().findFirst()
                                .orElseGet(() -> categoryRepository.save(Category.builder()
                                                .categoryName("HTTP Flow Category")
                                                .description("Category for HTTP Flow Tests")
                                                .build()));

                variety = varietyRepository.findAll().stream().findFirst()
                                .orElseGet(() -> varietyRepository.save(Variety.builder()
                                                .category(category)
                                                .varietyName("Bonsai HTTP Flow")
                                                .description("Variety for HTTP Flow Tests")
                                                .build()));

                segment = productSegmentRepository.findAll().stream().findFirst()
                                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                                                .segmentName("Standard HTTP Segment")
                                                .build()));
        }

        @AfterEach
        void tearDown() {
                for (String orderCode : createdOrderCodes) {
                        try {
                                orderRepository.findByOrderCode(orderCode).ifPresent(o -> {
                                        financialLedgerRepository.deleteAll(financialLedgerRepository
                                                        .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                                        o.getOrderId()));
                                        orderRepository.delete(o);
                                });
                        } catch (Exception e) {
                                System.err.println("Clean order warning: " + e.getMessage());
                        }
                }
                createdOrderCodes.clear();

                for (Integer pId : createdProductIds) {
                        try {
                                productRepository.findById(pId).ifPresent(productRepository::delete);
                        } catch (Exception e) {
                                System.err.println("Clean product warning: " + e.getMessage());
                        }
                }
                createdProductIds.clear();
        }

        private RequestPostProcessor moderatorUser(User userEntity) {
                return user(new CustomUserDetails(userEntity, List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"))));
        }

        private RequestPostProcessor customerUser(User userEntity) {
                return user(new CustomUserDetails(userEntity, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        }

        private Product createProduct(String name, BigDecimal price, String status) {
                Product p = Product.builder()
                                .productCode("HTTP-TREE-" + System.currentTimeMillis() + "-"
                                                + new java.util.Random().nextInt(1000))
                                .productName(name)
                                .price(price)
                                .productStatus(status)
                                .isVisible(true)
                                .isPublicPrice(true)
                                .variety(variety)
                                .segment(segment)
                                .age(10)
                                .height(60.0f)
                                .trunkDiameter(8.0f)
                                .style("Trực")
                                .description("Cây thử nghiệm HTTP Flow")
                                .createdBy(moderatorA)
                                .createdAt(LocalDateTime.now())
                                .build();
                p = productRepository.save(p);
                createdProductIds.add(p.getProductId());
                return p;
        }

        /**
         * TC-HTTP-BF01-001: Guest Checkout and Order Creation
         * Covers: POST /api/orders/send-guest-otp, POST /api/orders/checkout
         */
        @Test
        @DisplayName("TC-HTTP-BF01-001: Guest Checkout and Order Creation")
        void tcHttpBf01001_guestCheckoutAndOrderCreation() throws Exception {
                Product product = createProduct("Bonsai Linh Sam TC-HTTP-001", new BigDecimal("3000000"), "AVAILABLE");
                String guestEmail = "guest.http001." + System.currentTimeMillis() + "@test.com";

                // 1. Request Guest OTP
                Map<String, Object> otpPayload = new HashMap<>();
                otpPayload.put("email", guestEmail);
                otpPayload.put("productIds", Collections.singletonList(product.getProductId()));

                mockMvc.perform(post("/api/orders/send-guest-otp")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(otpPayload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", containsString("Mã OTP")));

                // 2. Retrieve generated OTP from Database
                PasswordResetOtp otpRecord = registerOtpRepository.findTopByEmailOrderByCreatedAtDesc(guestEmail)
                                .orElseThrow(() -> new AssertionError("Không tìm thấy mã OTP trong Database!"));
                assertNotNull(otpRecord.getOtpCode(), "Mã OTP không được null");
                assertEquals(6, otpRecord.getOtpCode().length(), "Mã OTP phải gồm 6 chữ số");
                assertFalse(otpRecord.getIsUsed(), "OTP ban đầu chưa được sử dụng");

                // 3. Submit valid guest checkout
                PurchaseOrderRequestDTO checkoutDto = new PurchaseOrderRequestDTO();
                checkoutDto.setCustomerName("Trần Văn Guest HTTP001");
                checkoutDto.setCustomerPhone("0981112233");
                checkoutDto.setCustomerEmail(guestEmail);
                checkoutDto.setShippingAddress("123 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội");
                checkoutDto.setPaymentMethod("DEPOSIT");
                checkoutDto.setProductId(product.getProductId());
                checkoutDto.setOtpCode(otpRecord.getOtpCode());
                checkoutDto.setNotes("Kiểm thử HTTP flow đặt cọc");

                String responseContent = mockMvc.perform(post("/api/orders/checkout")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(checkoutDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.orderCode").isNotEmpty())
                                .andReturn().getResponse().getContentAsString();

                Map<?, ?> respMap = objectMapper.readValue(responseContent, Map.class);
                String orderCode = (String) respMap.get("orderCode");
                createdOrderCodes.add(orderCode);

                // 4. Assert Database State
                Order order = orderRepository.findByOrderCodeWithDetails(orderCode)
                                .orElseThrow(() -> new AssertionError("Không tìm thấy đơn hàng trong DB!"));

                assertEquals("PENDING", order.getOrderStatus(), "Trạng thái đơn mới tạo phải là PENDING");
                assertNull(order.getAssignedTo(), "Đơn mới tạo chưa có Moderator gán");
                assertEquals(0, new BigDecimal("3000000").compareTo(order.getTotalAmount()),
                                "Tổng tiền ban đầu bằng giá cây");

                // OrderDetail check
                List<OrderDetail> details = order.getOrderDetails();
                assertNotNull(details, "Danh sách OrderDetail không được null");
                assertEquals(1, details.size(), "Phải có đúng 1 bản ghi OrderDetail");
                assertEquals(product.getProductId(), details.get(0).getProduct().getProductId(),
                                "ProductId trong chi tiết khớp");
                assertEquals(0, new BigDecimal("3000000").compareTo(details.get(0).getPriceAtPurchase()),
                                "Giá mua snapshot đúng");

                // Product status updated to RESERVED
                Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("RESERVED", updatedProduct.getProductStatus(), "Sản phẩm phải chuyển sang RESERVED");

                // Payment record created
                List<Payment> payments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(order.getOrderId());
                assertEquals(1, payments.size(), "Phải có 1 bản ghi Payment ban đầu");
                assertEquals("PENDING", payments.get(0).getPaymentStatus(), "Payment trạng thái ban đầu PENDING");
                assertEquals("DEPOSIT", payments.get(0).getPaymentType(), "PaymentType là DEPOSIT");

                // OTP is marked used
                PasswordResetOtp usedOtp = registerOtpRepository.findById(otpRecord.getOtpId()).orElseThrow();
                assertTrue(usedOtp.getIsUsed(), "OTP phải được đánh dấu đã sử dụng");
        }

        /**
         * TC-HTTP-BF01-002: Invalid OTP and Checkout Rejection
         * Covers: POST /api/orders/send-guest-otp, POST /api/orders/checkout with
         * invalid/expired/missing OTP
         */
        @Test
        @DisplayName("TC-HTTP-BF01-002: Invalid OTP and Checkout Rejection")
        void tcHttpBf01002_invalidOtpAndCheckoutRejection() throws Exception {
                Product product = createProduct("Bonsai Si TC-HTTP-002", new BigDecimal("4500000"), "AVAILABLE");
                String guestEmail = "guest.http002." + System.currentTimeMillis() + "@test.com";

                // 1. Request OTP
                Map<String, Object> otpPayload = new HashMap<>();
                otpPayload.put("email", guestEmail);
                otpPayload.put("productIds", Collections.singletonList(product.getProductId()));

                mockMvc.perform(post("/api/orders/send-guest-otp")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(otpPayload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)));

                // 2. Submit checkout with invalid OTP code (999999)
                PurchaseOrderRequestDTO checkoutDto = new PurchaseOrderRequestDTO();
                checkoutDto.setCustomerName("Lê Văn Sai OTP HTTP002");
                checkoutDto.setCustomerPhone("0977889900");
                checkoutDto.setCustomerEmail(guestEmail);
                checkoutDto.setShippingAddress("456 Đường Nguyễn Trãi, Hà Nội");
                checkoutDto.setPaymentMethod("DEPOSIT");
                checkoutDto.setProductId(product.getProductId());
                checkoutDto.setOtpCode("999999"); // Wrong OTP
                checkoutDto.setNotes("Thử nghiệm OTP sai");

                mockMvc.perform(post("/api/orders/checkout")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(checkoutDto)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message", containsString("Mã OTP không hợp lệ")));

                // 3. Submit checkout with empty OTP code
                checkoutDto.setOtpCode("");
                mockMvc.perform(post("/api/orders/checkout")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(checkoutDto)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.requireOtp", is(true)));

                // 4. Assert Database State: No order created, Product remains AVAILABLE
                Product intactProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("AVAILABLE", intactProduct.getProductStatus(),
                                "Sản phẩm vẫn phải là AVAILABLE khi checkout thất bại!");

                List<Order> ordersForEmail = orderRepository.findAll().stream()
                                .filter(o -> guestEmail.equalsIgnoreCase(o.getCustomerEmail()))
                                .toList();
                assertTrue(ordersForEmail.isEmpty(), "Không được tạo bất kỳ đơn hàng nào!");

                PasswordResetOtp otpRecord = registerOtpRepository.findTopByEmailOrderByCreatedAtDesc(guestEmail)
                                .orElseThrow();
                assertFalse(otpRecord.getIsUsed(), "OTP không được đánh dấu là đã dùng khi xác thực sai!");
        }

        /**
         * TC-HTTP-BF01-003: Moderator Claim and Approval Flow
         * Covers: POST /api/orders/{orderCode}/claim, POST
         * /api/orders/{orderCode}/verify
         */
        @Test
        @DisplayName("TC-HTTP-BF01-003: Moderator Claim and Approval Flow")
        void tcHttpBf01003_moderatorClaimAndApprovalFlow() throws Exception {
                Product product = createProduct("Bonsai Tùng Thác Đổ TC-HTTP-003", new BigDecimal("5000000"),
                                "RESERVED");

                // Seed an unassigned PENDING order
                String orderCode = "BSMS-HTTP-003-" + System.currentTimeMillis();
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Khách Hàng HTTP 003")
                                .customerPhone("0912334455")
                                .customerEmail("customer.http003@test.com")
                                .shippingAddress("78 Phố Huế, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("5000000"))
                                .depositAmount(BigDecimal.ZERO)
                                .orderStatus("PENDING")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .assignedTo(null)
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                Payment initialPayment = Payment.builder()
                                .order(order)
                                .paymentType("DEPOSIT")
                                .paymentMethod("DEPOSIT")
                                .paymentStatus("PENDING")
                                .amount(new BigDecimal("5000000"))
                                .build();
                paymentRepository.save(initialPayment);

                // 1. Moderator A claims the order
                mockMvc.perform(post("/api/orders/" + orderCode + "/claim")
                                .with(moderatorUser(moderatorA))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", containsString("thành công")));

                Order claimedOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertNotNull(claimedOrder.getAssignedTo(), "Đơn phải có Moderator phụ trách");
                assertEquals(moderatorA.getUserId(), claimedOrder.getAssignedTo().getUserId(),
                                "Đơn gán cho Moderator A");

                List<OrderHandling> handlings = orderHandlingRepository
                                .findByOrderOrderIdOrderByHandledAtDesc(claimedOrder.getOrderId());
                assertFalse(handlings.isEmpty(), "OrderHandling phải được ghi nhận");
                assertTrue(handlings.get(0).getIsActive(), "Phiên xử lý OrderHandling phải đang Active");

                // 2. Moderator B attempts to claim the already claimed order -> 409 Conflict
                mockMvc.perform(post("/api/orders/" + orderCode + "/claim")
                                .with(moderatorUser(moderatorB))
                                .with(csrf()))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success", is(false)));

                // 3. Moderator B attempts to verify / approve Moderator A's order -> 403
                // Forbidden
                Map<String, Object> verifyPayload = new HashMap<>();
                verifyPayload.put("craneFee", 500000);
                verifyPayload.put("shippingFee", 300000);
                verifyPayload.put("depositAmount", 1800000);

                mockMvc.perform(post("/api/orders/" + orderCode + "/verify")
                                .with(moderatorUser(moderatorB))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(verifyPayload)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message", containsString("không có quyền")));

                // 4. Moderator A approves order with custom fees: Crane 500k, Ship 300k,
                // Deposit 1.8M (not auto 30% = 1.5M)
                mockMvc.perform(post("/api/orders/" + orderCode + "/verify")
                                .with(moderatorUser(moderatorA))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(verifyPayload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", containsString("Duyệt đơn hàng thành công")));

                // 5. Assert Database State after approval
                Order verifiedOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("PENDING_PAYMENT", verifiedOrder.getOrderStatus(), "Trạng thái đơn là PENDING_PAYMENT");
                assertEquals(0, new BigDecimal("500000").compareTo(verifiedOrder.getCraneFee()), "Phí cẩu là 500,000");
                assertEquals(0, new BigDecimal("300000").compareTo(verifiedOrder.getShippingFee()),
                                "Phí ship là 300,000");
                assertEquals(0, new BigDecimal("1800000").compareTo(verifiedOrder.getDepositAmount()),
                                "Tiền cọc đúng 1,800,000 (không tự động 30%)");
                // Total = 5,000,000 (cây) + 500,000 (cẩu) + 300,000 (ship) = 5,800,000 VNĐ
                assertEquals(0, new BigDecimal("5800000").compareTo(verifiedOrder.getTotalAmount()),
                                "Tổng tiền là 5,800,000 VNĐ");

                // Payment check
                List<Payment> payments = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(verifiedOrder.getOrderId());
                assertEquals("PENDING", payments.get(0).getPaymentStatus(), "Payment ở trạng thái PENDING");
                assertEquals(0, new BigDecimal("1800000").compareTo(payments.get(0).getAmount()),
                                "Số tiền thanh toán nấc 1 (cọc) là 1,800,000");

                // OrderLog check
                List<OrderLog> logs = orderLogRepository
                                .findByOrderOrderIdOrderByActionAtAsc(verifiedOrder.getOrderId());
                boolean hasVerifyLog = logs.stream().anyMatch(l -> "VERIFY".equalsIgnoreCase(l.getActionType())
                                && "PENDING".equalsIgnoreCase(l.getFromStatus())
                                && "PENDING_PAYMENT".equalsIgnoreCase(l.getToStatus()));
                assertTrue(hasVerifyLog, "OrderLog phải ghi nhận VERIFY từ PENDING sang PENDING_PAYMENT");
        }

        /**
         * TC-HTTP-BF01-004: Moderator Rejection and Product Release
         * Covers: POST /api/orders/{orderCode}/claim, POST
         * /api/orders/{orderCode}/reject
         */
        @Test
        @DisplayName("TC-HTTP-BF01-004: Moderator Rejection and Product Release")
        void tcHttpBf01004_moderatorRejectionAndProductRelease() throws Exception {
                Product product = createProduct("Bonsai Sanh Cổ TC-HTTP-004", new BigDecimal("7000000"), "RESERVED");

                // Seed an unassigned PENDING order
                String orderCode = "BSMS-HTTP-004-" + System.currentTimeMillis();
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Khách Hàng Reject HTTP 004")
                                .customerPhone("0933221100")
                                .customerEmail("customer.http004@test.com")
                                .shippingAddress("12 Hoàng Diệu, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("7000000"))
                                .depositAmount(BigDecimal.ZERO)
                                .orderStatus("PENDING")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .assignedTo(null)
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                // 1. Moderator A claims order
                mockMvc.perform(post("/api/orders/" + orderCode + "/claim")
                                .with(moderatorUser(moderatorA))
                                .with(csrf()))
                                .andExpect(status().isOk());

                // 2. Moderator B attempts to reject Moderator A's order -> 403 Forbidden
                Map<String, String> rejectPayload = new HashMap<>();
                rejectPayload.put("reason", "Cây bị sâu bệnh trong quá trình lưu kho, không đủ tiêu chuẩn xuất vườn");

                mockMvc.perform(post("/api/orders/" + orderCode + "/reject")
                                .with(moderatorUser(moderatorB))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rejectPayload)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success", is(false)));

                // 3. Moderator A submits rejection with valid reason
                mockMvc.perform(post("/api/orders/" + orderCode + "/reject")
                                .with(moderatorUser(moderatorA))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rejectPayload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", containsString("Từ chối duyệt đơn hàng thành công")));

                // 4. Assert Database State
                Order rejectedOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("CANCELLED", rejectedOrder.getOrderStatus(),
                                "Trạng thái đơn hàng sau từ chối phải là CANCELLED");
                assertTrue(rejectedOrder.getNotes() != null && rejectedOrder.getNotes().contains("sâu bệnh"),
                                "Notes đơn hàng phải ghi nhận lý do từ chối");

                // Product released back to AVAILABLE
                Product releasedProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("AVAILABLE", releasedProduct.getProductStatus(), "Sản phẩm phải quay về AVAILABLE");

                // No successful Payment or FinancialLedger records
                List<Payment> payments = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(rejectedOrder.getOrderId());
                boolean hasSuccessPayment = payments.stream()
                                .anyMatch(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()));
                assertFalse(hasSuccessPayment, "Không được có Payment SUCCESS cho đơn bị từ chối");

                List<FinancialLedger> ledgers = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                rejectedOrder.getOrderId());
                assertTrue(ledgers.isEmpty(), "Không được có bản ghi FinancialLedger phát sinh");

                // OrderLog check
                List<OrderLog> logs = orderLogRepository
                                .findByOrderOrderIdOrderByActionAtAsc(rejectedOrder.getOrderId());
                boolean hasRejectLog = logs.stream().anyMatch(l -> "REJECT".equalsIgnoreCase(l.getActionType())
                                && "CANCELLED".equalsIgnoreCase(l.getToStatus()));
                assertTrue(hasRejectLog, "OrderLog phải ghi nhận action REJECT sang trạng thái CANCELLED");
        }

        private Map<String, String> buildSignedVnPayParams(String orderCode, BigDecimal amount, String responseCode,
                        String transactionStatus) {
                Map<String, String> params = new TreeMap<>();
                params.put("vnp_Amount", amount.multiply(new BigDecimal("100")).toBigInteger().toString());
                params.put("vnp_BankCode", "NCB");
                params.put("vnp_CardType", "ATM");
                params.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);
                params.put("vnp_PayDate", "20260818220000");
                params.put("vnp_ResponseCode", responseCode);
                params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode != null ? VNPayConfig.vnp_TmnCode : "BONSAISHOP");
                params.put("vnp_TransactionNo", "14000000");
                params.put("vnp_TransactionStatus", transactionStatus);
                params.put("vnp_TxnRef", orderCode);

                StringBuilder sb = new StringBuilder();
                Iterator<Map.Entry<String, String>> itr = params.entrySet().iterator();
                while (itr.hasNext()) {
                        Map.Entry<String, String> entry = itr.next();
                        try {
                                sb.append(entry.getKey()).append('=').append(URLEncoder.encode(entry.getValue(),
                                                StandardCharsets.US_ASCII.toString()));
                                if (itr.hasNext()) {
                                        sb.append('&');
                                }
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                }

                String secureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());
                params.put("vnp_SecureHash", secureHash);
                return params;
        }

        /**
         * TC-HTTP-BF01-005: Successful VNPay Result Processing and Idempotency
         * Covers: GET /vnpay/payment-callback, GET /vnpay/ipn
         */
        @Test
        @DisplayName("TC-HTTP-BF01-005: Successful VNPay Result Processing and Idempotency")
        void tcHttpBf01005_successfulVnPayResultProcessingAndIdempotency() throws Exception {
                Product product = createProduct("Bonsai Sung TC-HTTP-005", new BigDecimal("3000000"), "RESERVED");

                String orderCode = "BSMS-HTTP-005-" + System.currentTimeMillis();
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Khách Hàng VNPay Success HTTP 005")
                                .customerPhone("0911223344")
                                .customerEmail("customer.http005@test.com")
                                .shippingAddress("100 Phố Huế, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("3000000"))
                                .depositAmount(BigDecimal.ZERO)
                                .orderStatus("PENDING_PAYMENT")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .assignedTo(moderatorA)
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                Payment pendingPayment = Payment.builder()
                                .order(order)
                                .paymentType("FULL_PAYMENT")
                                .paymentMethod("VNPAY")
                                .paymentStatus("PENDING")
                                .amount(new BigDecimal("3000000"))
                                .build();
                paymentRepository.save(pendingPayment);

                // 1. Send first successful VNPay callback
                Map<String, String> signedParams = buildSignedVnPayParams(orderCode, new BigDecimal("3000000"), "00",
                                "00");
                MockHttpServletRequestBuilder callbackReq = get("/vnpay/payment-callback");
                signedParams.forEach(callbackReq::param);

                mockMvc.perform(callbackReq)
                                .andExpect(status().isOk())
                                .andExpect(view().name("payment-result"))
                                .andExpect(model().attribute("status", "SUCCESS"))
                                .andExpect(model().attribute("txnRef", orderCode));

                // 2. Assert Database State after first callback
                Order paidOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("PAID", paidOrder.getOrderStatus(), "Đơn hàng thanh toán 100% phải chuyển sang PAID");

                List<Payment> payments = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(paidOrder.getOrderId());
                assertEquals(1, payments.size(), "Phải có đúng 1 bản ghi Payment");
                assertEquals("SUCCESS", payments.get(0).getPaymentStatus(), "Payment phải là SUCCESS");
                assertNotNull(payments.get(0).getPaymentDate(), "PaymentDate phải được cập nhật");

                // 3. Send exact same callback a second time (Browser Refresh / Duplicate
                // callback simulation)
                mockMvc.perform(callbackReq)
                                .andExpect(status().isOk())
                                .andExpect(view().name("payment-result"))
                                .andExpect(model().attribute("status", "SUCCESS"));

                // 4. Assert Database Idempotency
                List<Payment> paymentsAfterSecond = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(paidOrder.getOrderId());
                assertEquals(1, paymentsAfterSecond.size(), "Không được tạo thêm bản ghi Payment trùng lặp");

                Order recheckedOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("PAID", recheckedOrder.getOrderStatus(), "Trạng thái đơn giữ nguyên PAID");

                List<FinancialLedger> ledgers = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                recheckedOrder.getOrderId());
                assertTrue(ledgers.isEmpty(),
                                "Chưa ghi nhận doanh thu vào Ledger ở bước PAID (chỉ ghi khi Moderator xác nhận hoàn tất giao nhận)");
        }

        /**
         * TC-HTTP-BF01-006: Invalid or Unsuccessful VNPay Result
         * Covers: Failure return codes, tampered/corrupted signature, amount mismatch
         * in IPN
         */
        @Test
        @DisplayName("TC-HTTP-BF01-006: Invalid or Unsuccessful VNPay Result")
        void tcHttpBf01006_invalidOrUnsuccessfulVnPayResult() throws Exception {
                // --- Branch 1: User cancelled transaction (vnp_ResponseCode = 24) ---
                Product product1 = createProduct("Bonsai Mai Vàng TC-HTTP-006-1", new BigDecimal("2500000"),
                                "RESERVED");
                String orderCode1 = "BSMS-HTTP-006-1-" + System.currentTimeMillis();
                Order order1 = Order.builder()
                                .orderCode(orderCode1)
                                .customerName("Khách Hàng Hủy VNPay")
                                .customerPhone("0922334455")
                                .customerEmail("customer.http006.1@test.com")
                                .shippingAddress("50 Cầu Giấy, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("2500000"))
                                .depositAmount(new BigDecimal("1000000"))
                                .orderStatus("PENDING_PAYMENT")
                                .orderType("ONLINE")
                                .assignedTo(moderatorA)
                                .build();
                order1 = orderRepository.save(order1);
                createdOrderCodes.add(orderCode1);

                Payment payment1 = Payment.builder()
                                .order(order1)
                                .paymentType("DEPOSIT")
                                .paymentMethod("VNPAY")
                                .paymentStatus("PENDING")
                                .amount(new BigDecimal("1000000"))
                                .build();
                paymentRepository.save(payment1);

                Map<String, String> cancelledParams = buildSignedVnPayParams(orderCode1, new BigDecimal("1000000"),
                                "24", "02");
                MockHttpServletRequestBuilder cancelReq = get("/vnpay/payment-callback");
                cancelledParams.forEach(cancelReq::param);

                mockMvc.perform(cancelReq)
                                .andExpect(status().isOk())
                                .andExpect(view().name("payment-result"))
                                .andExpect(model().attribute("status", "FAILED"));

                Order order1AfterCancel = orderRepository.findByOrderCode(orderCode1).orElseThrow();
                assertEquals("PENDING_PAYMENT", order1AfterCancel.getOrderStatus(),
                                "Đơn hàng vẫn ở PENDING_PAYMENT để khách có thể thanh toán lại");

                Payment payment1AfterCancel = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(order1AfterCancel.getOrderId()).get(0);
                assertEquals("FAILED", payment1AfterCancel.getPaymentStatus(), "Payment record chuyển sang FAILED");

                Product product1AfterCancel = productRepository.findById(product1.getProductId()).orElseThrow();
                assertEquals("RESERVED", product1AfterCancel.getProductStatus(),
                                "Cây vẫn giữ trạng thái RESERVED trong thời gian chờ");

                String orderCode2 = "BSMS-HTTP-006-2-" + System.currentTimeMillis();
                Order order2 = Order.builder()
                                .orderCode(orderCode2)
                                .customerName("Khách Hàng Chữ Ký Sai")
                                .customerPhone("0933445566")
                                .customerEmail("customer.http006.2@test.com")
                                .shippingAddress("60 Trần Duy Hưng, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("3500000"))
                                .depositAmount(new BigDecimal("1500000"))
                                .orderStatus("PENDING_PAYMENT")
                                .orderType("ONLINE")
                                .assignedTo(moderatorA)
                                .build();
                order2 = orderRepository.save(order2);
                createdOrderCodes.add(orderCode2);

                Payment payment2 = Payment.builder()
                                .order(order2)
                                .paymentType("DEPOSIT")
                                .paymentMethod("VNPAY")
                                .paymentStatus("PENDING")
                                .amount(new BigDecimal("1500000"))
                                .build();
                paymentRepository.save(payment2);

                Map<String, String> invalidSignatureParams = buildSignedVnPayParams(orderCode2,
                                new BigDecimal("1500000"), "00", "00");
                invalidSignatureParams.put("vnp_SecureHash", "INVALID_TAMPERED_HMAC_SIGNATURE_666");

                MockHttpServletRequestBuilder invalidSigReq = get("/vnpay/payment-callback");
                invalidSignatureParams.forEach(invalidSigReq::param);

                mockMvc.perform(invalidSigReq)
                                .andExpect(status().isOk())
                                .andExpect(view().name("payment-result"))
                                .andExpect(model().attribute("status", "INVALID_SIGNATURE"));

                Order order2AfterInvalid = orderRepository.findByOrderCode(orderCode2).orElseThrow();
                assertEquals("PENDING_PAYMENT", order2AfterInvalid.getOrderStatus(),
                                "Đơn hàng không bị thay đổi khi sai chữ ký");

                Payment payment2AfterInvalid = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(order2AfterInvalid.getOrderId()).get(0);
                assertEquals("PENDING", payment2AfterInvalid.getPaymentStatus(),
                                "Payment không bị thay đổi sang SUCCESS khi sai chữ ký");

                // --- Branch 3: Webhook IPN with mismatched amount ---
                Map<String, String> mismatchedAmountParams = buildSignedVnPayParams(orderCode2,
                                new BigDecimal("500000"), "00", "00"); // 500k instead of 1.5M
                MockHttpServletRequestBuilder ipnAmountMismatchReq = get("/vnpay/ipn");
                mismatchedAmountParams.forEach(ipnAmountMismatchReq::param);

                mockMvc.perform(ipnAmountMismatchReq)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.RspCode", is("04")))
                                .andExpect(jsonPath("$.Message", containsString("Invalid Amount")));
        }

        /**
         * TC-HTTP-BF01-007: Payment Expiration and Product Release
         * Covers: Real OrderExpirationService execution, timeout cancellation, late
         * callback rejection
         */
        @Test
        @DisplayName("TC-HTTP-BF01-007: Payment Expiration and Product Release")
        void tcHttpBf01007_paymentExpirationAndProductRelease() throws Exception {
                Product product = createProduct("Bonsai Cần Thăng TC-HTTP-007", new BigDecimal("4000000"), "RESERVED");

                String orderCode = "BSMS-HTTP-007-" + System.currentTimeMillis();
                // Seed order with orderDate 30 minutes in the past
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Khách Hàng Quá Hạn HTTP 007")
                                .customerPhone("0944556677")
                                .customerEmail("customer.http007@test.com")
                                .shippingAddress("80 Láng Hạ, Hà Nội")
                                .orderDate(LocalDateTime.now().minusMinutes(30))
                                .totalAmount(new BigDecimal("4000000"))
                                .depositAmount(new BigDecimal("1200000"))
                                .orderStatus("PENDING_PAYMENT")
                                .orderType("ONLINE")
                                .assignedTo(moderatorA)
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                Payment pendingPayment = Payment.builder()
                                .order(order)
                                .paymentType("DEPOSIT")
                                .paymentMethod("VNPAY")
                                .paymentStatus("PENDING")
                                .amount(new BigDecimal("1200000"))
                                .build();
                paymentRepository.save(pendingPayment);

                // 1. Run real OrderExpirationService
                orderExpirationService.cancelExpiredOrders();

                // 2. Assert Database State after expiration
                Order expiredOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("CANCELLED", expiredOrder.getOrderStatus(), "Đơn hàng phải bị CANCELLED do quá hạn");
                assertTrue(expiredOrder.getNotes() != null && expiredOrder.getNotes().contains("quá hạn"),
                                "Notes phải ghi nhận đơn quá hạn");

                Payment expiredPayment = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(expiredOrder.getOrderId()).get(0);
                assertEquals("EXPIRED", expiredPayment.getPaymentStatus(),
                                "Payment record PENDING phải chuyển sang EXPIRED");

                Product releasedProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("AVAILABLE", releasedProduct.getProductStatus(),
                                "Sản phẩm phải được giải phóng về AVAILABLE");

                // 3. Simulate a late VNPay callback arriving after order has expired
                Map<String, String> lateCallbackParams = buildSignedVnPayParams(orderCode, new BigDecimal("1200000"),
                                "00", "00");
                MockHttpServletRequestBuilder lateCallbackReq = get("/vnpay/payment-callback");
                lateCallbackParams.forEach(lateCallbackReq::param);

                mockMvc.perform(lateCallbackReq)
                                .andExpect(status().isOk())
                                .andExpect(view().name("payment-result"));

                // 4. Assert that product is STILL AVAILABLE and no revenue is recognized
                Product productAfterLateCallback = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("AVAILABLE", productAfterLateCallback.getProductStatus(),
                                "Cây vẫn phải là AVAILABLE, không được đổi sang SOLD");

                List<FinancialLedger> ledgers = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                expiredOrder.getOrderId());
                assertTrue(ledgers.isEmpty(), "Không được ghi nhận bất kỳ doanh thu nào vào Sổ cái");
        }

        /**
         * TC-HTTP-BF01-008: Guest Order Lookup
         * Covers: GET /order/lookup, GET /api/orders/{orderCode}
         */
        @Test
        @DisplayName("TC-HTTP-BF01-008: Guest Order Lookup")
        void tcHttpBf01008_guestOrderLookup() throws Exception {
                Product product = createProduct("Bonsai Trà Phúc Kiến TC-HTTP-008", new BigDecimal("3000000"),
                                "AVAILABLE");

                String orderCode = "BSMS-HTTP-008-" + System.currentTimeMillis();
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Nguyễn Thị Khách Vãng Lai")
                                .customerPhone("0955667788")
                                .customerEmail("guest.lookup008@test.com")
                                .shippingAddress("200 Nguyễn Trãi, Thanh Xuân, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("3300000"))
                                .depositAmount(new BigDecimal("1000000"))
                                .craneFee(new BigDecimal("200000"))
                                .shippingFee(new BigDecimal("100000"))
                                .orderStatus("PENDING_PAYMENT")
                                .orderType("ONLINE")
                                .assignedTo(moderatorA)
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                // 1. Public lookup via /order/lookup without authentication
                mockMvc.perform(get("/order/lookup").param("orderCode", orderCode))
                                .andExpect(status().isOk())
                                .andExpect(view().name("customer/order_lookup"))
                                .andExpect(model().attribute("searched", is(true)))
                                .andExpect(model().attributeExists("order"))
                                .andExpect(model().attribute("searchCode", is(orderCode)));

                // 2. Lookup via REST API /api/orders/{orderCode}
                mockMvc.perform(get("/api/orders/" + orderCode))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderCode", is(orderCode)))
                                .andExpect(jsonPath("$.customer.name", containsString("Khách Vãng Lai")))
                                .andExpect(jsonPath("$.customer.email", is("guest.lookup008@test.com")))
                                .andExpect(jsonPath("$.totalAmount", is(3300000.0)))
                                .andExpect(jsonPath("$.depositAmount", is(1000000.0)))
                                .andExpect(jsonPath("$.craneFee", is(200000.0)))
                                .andExpect(jsonPath("$.shippingFee", is(100000.0)))
                                .andExpect(jsonPath("$.orderStatus", is("PENDING_PAYMENT")));

                // 3. Lookup non-existent order code via /order/lookup
                mockMvc.perform(get("/order/lookup").param("orderCode", "BSMS-NONEXISTENT-999"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("customer/order_lookup"))
                                .andExpect(model().attribute("searched", is(true)))
                                .andExpect(model().attribute("order", org.hamcrest.Matchers.nullValue()));

                // 4. Lookup non-existent order code via REST API /api/orders/{orderCode}
                mockMvc.perform(get("/api/orders/BSMS-NONEXISTENT-999"))
                                .andExpect(status().isNotFound());

                // 5. Assert Database is intact (no modifications during lookup)
                Order intactOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("PENDING_PAYMENT", intactOrder.getOrderStatus(),
                                "Lookup không làm thay đổi trạng thái đơn");
                assertEquals(0, new BigDecimal("3300000").compareTo(intactOrder.getTotalAmount()),
                                "Lookup không làm thay đổi tổng tiền");
        }

        /**
         * TC-HTTP-BF01-009: Authorization and Session Enforcement
         * Covers: RBAC guards, unassigned verification rejection, multi-moderator
         * isolation, session expiration/logout
         */
        @Test
        @DisplayName("TC-HTTP-BF01-009: Authorization and Session Enforcement")
        void tcHttpBf01009_authorizationAndSessionEnforcement() throws Exception {
                Product product = createProduct("Bonsai Linh Sam TC-HTTP-009", new BigDecimal("3500000"), "RESERVED");

                String orderCode = "BSMS-HTTP-009-" + System.currentTimeMillis();
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Khách Hàng RBAC HTTP 009")
                                .customerPhone("0977889900")
                                .customerEmail("customer.http009@test.com")
                                .shippingAddress("123 Phố Huế, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("3500000"))
                                .depositAmount(new BigDecimal("1000000"))
                                .orderStatus("PENDING")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .assignedTo(null) // unassigned in pool
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                // 1. Customer attempts to access moderator endpoints -> 403 Forbidden
                Map<String, Object> verifyPayload = new HashMap<>();
                verifyPayload.put("craneFee", 200000);
                verifyPayload.put("shippingFee", 100000);
                verifyPayload.put("depositAmount", 1000000);

                mockMvc.perform(post("/api/orders/" + orderCode + "/verify")
                                .with(customerUser(customer))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(verifyPayload)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success", is(false)));

                Map<String, String> customerRejectPayload = Map.of("reason", "Customer unauthorized reject");
                mockMvc.perform(post("/api/orders/" + orderCode + "/reject")
                                .with(customerUser(customer))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(customerRejectPayload)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success", is(false)));

                // 2. Moderator A attempts to verify unassigned order -> 403 Forbidden
                mockMvc.perform(post("/api/orders/" + orderCode + "/verify")
                                .with(moderatorUser(moderatorA))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(verifyPayload)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success", is(false)));

                // 3. Moderator A claims the order successfully
                mockMvc.perform(post("/api/orders/" + orderCode + "/claim")
                                .with(moderatorUser(moderatorA))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)));

                // 4. Moderator B attempts to claim Moderator A's order -> 409 Conflict
                mockMvc.perform(post("/api/orders/" + orderCode + "/claim")
                                .with(moderatorUser(moderatorB))
                                .with(csrf()))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success", is(false)));

                // 5. Moderator B attempts to verify Moderator A's order -> 403 Forbidden
                mockMvc.perform(post("/api/orders/" + orderCode + "/verify")
                                .with(moderatorUser(moderatorB))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(verifyPayload)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success", is(false)));

                // 6. Moderator B attempts to reject Moderator A's order -> 403 Forbidden
                Map<String, String> modBRejectPayload = Map.of("reason", "Từ chối trái phép");
                mockMvc.perform(post("/api/orders/" + orderCode + "/reject")
                                .with(moderatorUser(moderatorB))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(modBRejectPayload)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success", is(false)));

                // 7. Unauthenticated request (expired / logged out session simulation) -> 401
                // Unauthorized
                mockMvc.perform(post("/api/orders/" + orderCode + "/verify")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(verifyPayload)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success", is(false)));

                // 8. Assert Database State remains protected
                Order guardedOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("PENDING", guardedOrder.getOrderStatus(),
                                "Trạng thái đơn vẫn là PENDING, không bị verify trái phép");
                assertEquals(moderatorA.getUserId(), guardedOrder.getAssignedTo().getUserId(),
                                "Đơn vẫn chỉ phụ trách bởi Moderator A");

                Product guardedProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("RESERVED", guardedProduct.getProductStatus(), "Sản phẩm vẫn là RESERVED");

                List<FinancialLedger> ledgers = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                guardedOrder.getOrderId());
                assertTrue(ledgers.isEmpty(), "Không có bất kỳ Ledger nào phát sinh từ các request bị cấm");
        }

        /**
         * TC-HTTP-BF01-010: Remaining Payment and Order Completion
         * Covers: POST /api/orders/{orderCode}/confirm-remaining-payment, cash
         * collection, ledger recording, retry idempotency
         */
        @Test
        @DisplayName("TC-HTTP-BF01-010: Remaining Payment and Order Completion")
        void tcHttpBf01010_remainingPaymentAndOrderCompletion() throws Exception {
                Product product = createProduct("Bonsai Tùng La Hán TC-HTTP-010", new BigDecimal("4000000"),
                                "RESERVED");

                String orderCode = "BSMS-HTTP-010-" + System.currentTimeMillis();
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Khách Hàng Thu Tiền Mặt HTTP 010")
                                .customerPhone("0988112233")
                                .customerEmail("customer.http010@test.com")
                                .shippingAddress("456 Giải Phóng, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("4000000"))
                                .depositAmount(new BigDecimal("1000000"))
                                .orderStatus("DEPOSITED")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .assignedTo(moderatorA)
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                Payment depositPayment = Payment.builder()
                                .order(order)
                                .paymentType("DEPOSIT")
                                .paymentMethod("VNPAY")
                                .paymentStatus("SUCCESS")
                                .amount(new BigDecimal("1000000"))
                                .paymentDate(LocalDateTime.now().minusHours(1))
                                .build();
                paymentRepository.save(depositPayment);

                // 1. Moderator A confirms remaining cash payment
                Map<String, String> confirmPayload = Map.of("notes",
                                "Đã giao cây tận nhà và thu 3.000.000 VNĐ tiền mặt");
                mockMvc.perform(post("/api/orders/" + orderCode + "/confirm-remaining-payment")
                                .with(moderatorUser(moderatorA))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(confirmPayload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", containsString("thanh toán đầy đủ thành công")));

                // 2. Assert Database State after completion
                Order completedOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("COMPLETED", completedOrder.getOrderStatus(), "Đơn hàng phải chuyển sang COMPLETED");
                assertNotNull(completedOrder.getCompletedAt(), "completedAt phải có giá trị");

                // Product marked as SOLD
                Product soldProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("SOLD", soldProduct.getProductStatus(), "Cây phải chuyển sang SOLD");

                // Assert 2 Payments: 1 DEPOSIT (1M), 1 REMAINING_PAYMENT (3M)
                List<Payment> payments = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(completedOrder.getOrderId());
                assertEquals(2, payments.size(), "Phải có đúng 2 bản ghi Payment");
                Payment remainingPayment = payments.stream()
                                .filter(p -> "REMAINING_PAYMENT".equalsIgnoreCase(p.getPaymentType()))
                                .findFirst().orElseThrow();
                assertEquals("SUCCESS", remainingPayment.getPaymentStatus(), "Remaining Payment phải là SUCCESS");
                assertEquals("CASH", remainingPayment.getPaymentMethod(), "Phương thức thanh toán còn lại là CASH");
                assertEquals(0, new BigDecimal("3000000").compareTo(remainingPayment.getAmount()),
                                "Số tiền thu đợt 2 phải là 3.000.000 VNĐ");

                // Assert Financial Ledger
                List<FinancialLedger> ledgers = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                completedOrder.getOrderId());
                assertEquals(1, ledgers.size(), "Phải có đúng 1 bản ghi FinancialLedger ghi nhận doanh thu");
                assertEquals(FinancialLedgerType.COMPLETED_ORDER_REVENUE, ledgers.get(0).getLedgerType());
                assertEquals(0, new BigDecimal("4000000").compareTo(ledgers.get(0).getAmount()),
                                "Doanh thu ghi nhận phải là 4.000.000 VNĐ");

                // Assert OrderLog
                List<OrderLog> logs = orderLogRepository
                                .findByOrderOrderIdOrderByActionAtAsc(completedOrder.getOrderId());
                boolean hasConfirmLog = logs.stream()
                                .anyMatch(l -> "REMAINING_PAYMENT_CONFIRMED".equalsIgnoreCase(l.getActionType())
                                                && "COMPLETED".equalsIgnoreCase(l.getToStatus()));
                assertTrue(hasConfirmLog, "OrderLog phải ghi nhận REMAINING_PAYMENT_CONFIRMED sang COMPLETED");

                // 3. Retry request simulation -> 409 Conflict
                mockMvc.perform(post("/api/orders/" + orderCode + "/confirm-remaining-payment")
                                .with(moderatorUser(moderatorA))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(confirmPayload)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success", is(false)));

                // 4. Assert Idempotency in DB
                List<Payment> paymentsAfterRetry = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(completedOrder.getOrderId());
                assertEquals(2, paymentsAfterRetry.size(), "Retry không được tạo thêm Payment");

                List<FinancialLedger> ledgersAfterRetry = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                completedOrder.getOrderId());
                assertEquals(1, ledgersAfterRetry.size(), "Retry không được duplicate FinancialLedger");
        }

        /**
         * TC-HTTP-BF01-011: Full-Payment Delivery Completion
         * Covers: POST /api/orders/{orderCode}/complete, full VNPay paid order
         * completion, ledger revenue, idempotency
         */
        @Test
        @DisplayName("TC-HTTP-BF01-011: Full-Payment Delivery Completion")
        void tcHttpBf01011_fullPaymentDeliveryCompletion() throws Exception {
                Product product = createProduct("Bonsai Si Quả TC-HTTP-011", new BigDecimal("5000000"), "RESERVED");

                String orderCode = "BSMS-HTTP-011-" + System.currentTimeMillis();
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Khách Hàng Full VNPay HTTP 011")
                                .customerPhone("0966778899")
                                .customerEmail("customer.http011@test.com")
                                .shippingAddress("789 Hoàng Hoa Thám, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("5000000"))
                                .depositAmount(BigDecimal.ZERO)
                                .orderStatus("PAID")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .assignedTo(moderatorA)
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                Payment fullPayment = Payment.builder()
                                .order(order)
                                .paymentType("FULL_PAYMENT")
                                .paymentMethod("VNPAY")
                                .paymentStatus("SUCCESS")
                                .amount(new BigDecimal("5000000"))
                                .paymentDate(LocalDateTime.now().minusHours(1))
                                .build();
                paymentRepository.save(fullPayment);

                // 1. Assert pre-condition: order is PAID, tree is RESERVED, 0 revenue in ledger
                // yet
                Order preOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("PAID", preOrder.getOrderStatus(), "Đơn phải là PAID trước khi giao");
                Product preProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("RESERVED", preProduct.getProductStatus(), "Cây vẫn phải là RESERVED trước khi giao");
                assertTrue(financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(preOrder.getOrderId())
                                .isEmpty(),
                                "Chưa ghi nhận doanh thu vào Sổ cái trước khi giao cây hoàn tất");

                // 2. Moderator A confirms customer received tree -> POST
                // /api/orders/{orderCode}/complete
                mockMvc.perform(post("/api/orders/" + orderCode + "/complete")
                                .with(moderatorUser(moderatorA))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", containsString("hoàn thành")));

                // 3. Assert Database State after completion
                Order completedOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("COMPLETED", completedOrder.getOrderStatus(), "Đơn hàng phải chuyển sang COMPLETED");
                assertNotNull(completedOrder.getCompletedAt(), "completedAt phải có giá trị");

                // Product marked as SOLD
                Product soldProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("SOLD", soldProduct.getProductStatus(), "Cây phải chuyển sang SOLD");

                // No new payment record created (still only the 1 original full payment)
                List<Payment> payments = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(completedOrder.getOrderId());
                assertEquals(1, payments.size(), "Không được tạo thêm Payment mới");

                // Financial Ledger recorded exactly once
                List<FinancialLedger> ledgers = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                completedOrder.getOrderId());
                assertEquals(1, ledgers.size(), "Phải có đúng 1 bản ghi FinancialLedger ghi nhận doanh thu");
                assertEquals(FinancialLedgerType.COMPLETED_ORDER_REVENUE, ledgers.get(0).getLedgerType());
                assertEquals(0, new BigDecimal("5000000").compareTo(ledgers.get(0).getAmount()),
                                "Doanh thu ghi nhận phải là 5.000.000 VNĐ");

                // OrderLog recorded
                List<OrderLog> logs = orderLogRepository
                                .findByOrderOrderIdOrderByActionAtAsc(completedOrder.getOrderId());
                boolean hasCompleteLog = logs.stream()
                                .anyMatch(l -> "ORDER_COMPLETED".equalsIgnoreCase(l.getActionType())
                                                && "COMPLETED".equalsIgnoreCase(l.getToStatus()));
                assertTrue(hasCompleteLog, "OrderLog phải ghi nhận ORDER_COMPLETED sang COMPLETED");

                // 4. Retry request simulation -> 409 Conflict
                mockMvc.perform(post("/api/orders/" + orderCode + "/complete")
                                .with(moderatorUser(moderatorA))
                                .with(csrf()))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success", is(false)));

                // 5. Assert Idempotency
                List<FinancialLedger> ledgersAfterRetry = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                completedOrder.getOrderId());
                assertEquals(1, ledgersAfterRetry.size(), "Retry không được duplicate FinancialLedger");
        }

        /**
         * TC-HTTP-BF01-012: Customer No-Show After Deposit
         * Covers: POST /api/orders/{orderCode}/customer-no-show, deposit forfeiture,
         * product release, ledger recording, idempotency
         */
        @Test
        @DisplayName("TC-HTTP-BF01-012: Customer No-Show After Deposit")
        void tcHttpBf01012_customerNoShowAfterDeposit() throws Exception {
                Product product = createProduct("Bonsai Đa Búp Đỏ TC-HTTP-012", new BigDecimal("6000000"), "RESERVED");

                String orderCode = "BSMS-HTTP-012-" + System.currentTimeMillis();
                Order order = Order.builder()
                                .orderCode(orderCode)
                                .customerName("Khách Hàng Bùng Cọc HTTP 012")
                                .customerPhone("0912345678")
                                .customerEmail("customer.http012@test.com")
                                .shippingAddress("123 Lê Duẩn, Hoàn Kiếm, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("6000000"))
                                .depositAmount(new BigDecimal("1800000"))
                                .orderStatus("DEPOSITED")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .assignedTo(moderatorA)
                                .build();
                order = orderRepository.save(order);
                createdOrderCodes.add(orderCode);

                OrderDetail detail = OrderDetail.builder()
                                .order(order)
                                .product(product)
                                .priceAtPurchase(product.getPrice())
                                .quantity(1)
                                .build();
                orderDetailRepository.save(detail);

                Payment depositPayment = Payment.builder()
                                .order(order)
                                .paymentType("DEPOSIT")
                                .paymentMethod("VNPAY")
                                .paymentStatus("SUCCESS")
                                .amount(new BigDecimal("1800000"))
                                .paymentDate(LocalDateTime.now().minusHours(2))
                                .build();
                paymentRepository.save(depositPayment);

                // 1. Moderator A marks customer no-show with required reason
                Map<String, String> noShowPayload = Map.of("notes",
                                "Khách hàng không nghe máy và từ chối nhận cây khi xe cẩu đến giao");
                mockMvc.perform(post("/api/orders/" + orderCode + "/customer-no-show")
                                .with(moderatorUser(moderatorA))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(noShowPayload)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.message", containsString("Đã hủy đơn vì khách không nhận")));

                // 2. Assert Database State
                Order cancelledOrder = orderRepository.findByOrderCode(orderCode).orElseThrow();
                assertEquals("CANCELLED", cancelledOrder.getOrderStatus(), "Đơn hàng phải chuyển sang CANCELLED");
                assertTrue(cancelledOrder.getNotes() != null && cancelledOrder.getNotes().contains("từ chối nhận cây"),
                                "Notes đơn hàng phải ghi nhận lý do hủy do khách không nhận");
                assertTrue(cancelledOrder.getNotes().contains("Tiền cọc được ghi nhận giữ lại"),
                                "Notes phải ghi nhận tiền cọc được giữ lại");

                // Product released back to AVAILABLE
                Product releasedProduct = productRepository.findById(product.getProductId()).orElseThrow();
                assertEquals("AVAILABLE", releasedProduct.getProductStatus(),
                                "Sản phẩm phải quay về AVAILABLE để bán cho người khác");

                // Deposit payment remains intact (not refunded, not deleted)
                List<Payment> payments = paymentRepository
                                .findByOrderOrderIdOrderByPaymentIdAsc(cancelledOrder.getOrderId());
                assertEquals(1, payments.size(), "Không tạo refund toàn bộ giá cây, vẫn giữ bản ghi deposit");
                assertEquals("SUCCESS", payments.get(0).getPaymentStatus(), "Payment deposit vẫn là SUCCESS");
                assertEquals(0, new BigDecimal("1800000").compareTo(payments.get(0).getAmount()),
                                "Số tiền cọc giữ nguyên 1.800.000 VNĐ");

                // Financial Ledger recorded FORFEITED_DEPOSIT_INCOME
                List<FinancialLedger> ledgers = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                cancelledOrder.getOrderId());
                assertEquals(1, ledgers.size(), "Phải có đúng 1 bản ghi FinancialLedger");
                assertEquals(FinancialLedgerType.FORFEITED_DEPOSIT_INCOME, ledgers.get(0).getLedgerType());
                assertEquals(FaultParty.CUSTOMER, ledgers.get(0).getFaultParty(), "Bên chịu lỗi phải là CUSTOMER");
                assertEquals(0, new BigDecimal("1800000").compareTo(ledgers.get(0).getAmount()),
                                "Số tiền tịch thu ghi vào Ledger là 1.800.000 VNĐ");

                // OrderLog check
                List<OrderLog> logs = orderLogRepository
                                .findByOrderOrderIdOrderByActionAtAsc(cancelledOrder.getOrderId());
                boolean hasNoShowLog = logs.stream()
                                .anyMatch(l -> "FORFEITED_DEPOSIT_INCOME_RECORDED".equalsIgnoreCase(l.getActionType())
                                                && "CANCELLED".equalsIgnoreCase(l.getToStatus()));
                assertTrue(hasNoShowLog, "OrderLog phải ghi nhận FORFEITED_DEPOSIT_INCOME_RECORDED sang CANCELLED");

                // 3. Retry request simulation -> 409 Conflict
                mockMvc.perform(post("/api/orders/" + orderCode + "/customer-no-show")
                                .with(moderatorUser(moderatorA))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(noShowPayload)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success", is(false)));

                // 4. Assert Idempotency in DB
                List<FinancialLedger> ledgersAfterRetry = financialLedgerRepository
                                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                cancelledOrder.getOrderId());
                assertEquals(1, ledgersAfterRetry.size(), "Retry không được duplicate FinancialLedger");
        }
}
