package com.example.bonsai_shop.system;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.EmailService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.integration.support.TestDatabaseSafetyInitializer;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.OrderDetailRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import com.example.bonsai_shop.product.service.MailService;
import com.example.bonsai_shop.product.service.OrderService;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3 SYSTEM TEST (Automated Cryptographic Callback & Browser UI Test - TC-L3-BF01-004)
 * Business Flow: BF-01 Unsuccessful VNPay Deposit Payment
 * Actor: Guest Customer
 *
 * <p><strong>Purpose & Automation Architecture:</strong>
 * This automated system test verifies the internal BSMS failure handling when a customer cancels
 * or fails a VNPay payment transaction. To maintain 100% deterministic test execution in offline/CI
 * environments without external third-party network dependency on {@code sandbox.vnpayment.vn},
 * the callback URL is generated with exact parameters (vnp_ResponseCode = 24, vnp_TransactionStatus = 02)
 * and signed cryptographically using HMAC-SHA512 with the configured secret key.
 *
 * <p><strong>Assertions:</strong>
 * <ol>
 *   <li>Payment Result UI informs user of payment failure/cancellation.</li>
 *   <li>Order Lookup UI displays status 'Chờ thanh toán' (PENDING_PAYMENT).</li>
 *   <li>Database Order status remains PENDING_PAYMENT (not DEPOSITED, PAID, or COMPLETED).</li>
 *   <li>Product status remains RESERVED.</li>
 *   <li>Deposit Payment is marked as FAILED (no SUCCESS payment exists).</li>
 *   <li>FinancialLedger has 0 revenue records.</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BF01UnsuccessfulVnPayDepositPaymentE2ETest {

        @LocalServerPort
        private int port;

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
        private FinancialLedgerRepository financialLedgerRepository;

        @Autowired
        private OrderService orderService;

        private Playwright playwright;
        private Browser browser;

        private User moderatorEntity;
        private Product testProduct;
        private Order testOrder;
        private String testOrderCode;

        private String getBaseUrl() {
                return "http://localhost:" + port;
        }

        private void evidencePause(Page page, String stepName) {
                System.out.println("[E2E PAUSE] " + stepName);
                page.waitForTimeout(3000);
        }

        private void login(Page page, String email, String password) {
                page.navigate(getBaseUrl() + "/login");
                page.fill("#email", email);
                page.fill("#password", password);
                page.click("button.btn-signin");
                page.waitForURL(url -> !url.contains("/login"));
        }

        @BeforeAll
        void setUpAll() {
                // Setup Playwright in headed mode with slowMo
                Playwright.CreateOptions createOptions = new Playwright.CreateOptions();
                Map<String, String> env = new HashMap<>(System.getenv());
                env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
                createOptions.setEnv(env);
                playwright = Playwright.create(createOptions);

                browser = playwright.chromium().launch(
                                new BrowserType.LaunchOptions()
                                                .setHeadless(false)
                                                .setSlowMo(1500));

                // Get/Create MODERATOR role
                Role moderatorRole = roleRepository.findByRoleName("MODERATOR")
                                .orElseGet(() -> roleRepository.save(Role.builder().roleName("MODERATOR").build()));

                // Create or sync Moderator account for TC-L3-BF01-004
                String modEmail = "moderator.vnpay.fail.e2e@test.com";
                userRepository.findByEmail(modEmail).ifPresent(u -> {
                        u.setPassword(passwordEncoder.encode("password123"));
                        u.setStatus("ACTIVE");
                        u.setRole(moderatorRole);
                        userRepository.save(u);
                });
                moderatorEntity = userRepository.findByEmail(modEmail)
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .fullName("Order Moderator (TC-004)")
                                                .email(modEmail)
                                                .username("moderator_vnpay_fail_e2e")
                                                .password(passwordEncoder.encode("password123"))
                                                .role(moderatorRole)
                                                .status("ACTIVE")
                                                .phone("0908887777")
                                                .build()));

                // Create Category, Variety, Segment for test product
                Category category = categoryRepository.findAll().stream().findFirst()
                                .orElseGet(() -> categoryRepository.save(Category.builder()
                                                .categoryName("E2E Category TC004")
                                                .description("Category for TC-L3-BF01-004")
                                                .build()));

                Variety variety = varietyRepository.findAll().stream().findFirst()
                                .orElseGet(() -> varietyRepository.save(Variety.builder()
                                                .category(category)
                                                .varietyName("Bonsai Mai Vàng TC004")
                                                .description("Variety for TC-L3-BF01-004")
                                                .build()));

                ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                                                .segmentName("Standard Segment TC004")
                                                .build()));

                testProduct = Product.builder()
                                .productCode("TC004-TREE-" + System.currentTimeMillis())
                                .productName("Cây Bonsai Mai Vàng Thất Thốn TC-L3-BF01-004")
                                .price(new BigDecimal("5000000"))
                                .productStatus("RESERVED")
                                .isVisible(true)
                                .isPublicPrice(true)
                                .variety(variety)
                                .segment(segment)
                                .age(20)
                                .height(95.0f)
                                .trunkDiameter(15.0f)
                                .style("Dáng huyền")
                                .description("Cây Bonsai Mai Vàng test hủy thanh toán cọc VNPay TC-L3-BF01-004")
                                .createdBy(moderatorEntity)
                                .createdAt(LocalDateTime.now())
                                .build();
                testProduct = productRepository.save(testProduct);

                // Create dedicated unassigned PENDING test order
                testOrderCode = "BSMS-TC-BF01-004-" + System.currentTimeMillis();
                testOrder = Order.builder()
                                .orderCode(testOrderCode)
                                .customerName("Trần Khách Hàng Fail VNPay TC004")
                                .customerPhone("0977112233")
                                .customerEmail("customer.tc004@test.com")
                                .shippingAddress("456 Đường Nguyễn Trãi, Quận Thanh Xuân, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("5000000"))
                                .depositAmount(BigDecimal.ZERO)
                                .orderStatus("PENDING")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .notes("Đơn hàng test hủy thanh toán cọc VNPay TC-L3-BF01-004")
                                .assignedTo(null)
                                .assignedAt(null)
                                .build();
                testOrder = orderRepository.save(testOrder);

                OrderDetail detail = OrderDetail.builder()
                                .order(testOrder)
                                .product(testProduct)
                                .priceAtPurchase(testProduct.getPrice())
                                .quantity(1)
                                .build();

                List<OrderDetail> details = new ArrayList<>();
                details.add(detail);
                testOrder.setOrderDetails(details);
                orderDetailRepository.save(detail);

                // Pre-create initial DEPOSIT payment for the order
                Payment initialDepositPayment = Payment.builder()
                                .order(testOrder)
                                .paymentType("DEPOSIT")
                                .paymentMethod("VNPAY")
                                .paymentStatus("PENDING")
                                .amount(new BigDecimal("2000000"))
                                .build();
                paymentRepository.save(initialDepositPayment);
        }

        @AfterAll
        void tearDownAll() {
                try {
                        if (testOrderCode != null) {
                                orderRepository.findByOrderCode(testOrderCode).ifPresent(o -> {
                                        financialLedgerRepository.deleteAll(financialLedgerRepository.findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(o.getOrderId()));
                                        orderRepository.delete(o);
                                });
                        }
                        if (testProduct != null && testProduct.getProductId() != null) {
                                productRepository.findById(testProduct.getProductId())
                                                .ifPresent(p -> productRepository.delete(p));
                        }
                } catch (Exception e) {
                        System.err.println("Clean up warning: " + e.getMessage());
                }

                if (browser != null) {
                        browser.close();
                }
                if (playwright != null) {
                        playwright.close();
                }
        }

        /**
         * TC-L3-BF01-004: Unsuccessful VNPay Deposit Payment Keeps Order Pending Payment.
         */
        @Test
        @DisplayName("TC-L3-BF01-004: Unsuccessful VNPay Deposit Payment Keeps Order Pending Payment")
        void tcL3Bf01004_unsuccessfulVnPayDepositKeepsOrderPendingPayment() {
                try (BrowserContext context = browser.newContext()) {
                        Page page = context.newPage();

                        // 1. Moderator claims and approves order with depositAmount = 2,000,000 VNĐ
                        orderService.claimOrder(testOrderCode, moderatorEntity);
                        boolean approved = orderService.verifyOrder(
                                        testOrderCode,
                                        new BigDecimal("300000"), // Crane Fee
                                        new BigDecimal("500000"), // Shipping Fee
                                        new BigDecimal("2000000"), // Deposit Amount
                                        moderatorEntity);
                        assertTrue(approved, "Duyệt đơn hàng trực tiếp qua service phải thành công!");

                        // Pre-payment assertions in DB
                        Order approvedOrder = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
                        assertEquals("PENDING_PAYMENT", approvedOrder.getOrderStatus(),
                                        "Trạng thái đơn sau duyệt phải là PENDING_PAYMENT!");
                        assertEquals(0, new BigDecimal("2000000").compareTo(approvedOrder.getDepositAmount()),
                                        "Tiền đặt cọc là 2,000,000 VNĐ!");

                        Product dbProductPre = productRepository.findById(testProduct.getProductId()).orElseThrow();
                        assertEquals("RESERVED", dbProductPre.getProductStatus(),
                                        "Trạng thái sản phẩm trước khi thanh toán phải là RESERVED!");

                        List<Payment> prePayments = paymentRepository
                                        .findByOrderOrderIdOrderByPaymentIdAsc(approvedOrder.getOrderId());
                        assertFalse(prePayments.isEmpty(), "Phải có bản ghi Payment khởi tạo!");
                        boolean preHasSuccess = prePayments.stream()
                                        .anyMatch(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()));
                        assertFalse(preHasSuccess, "Payment trước khi thanh toán không được là SUCCESS!");

                        List<FinancialLedger> preLedgers = financialLedgerRepository
                                        .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                        approvedOrder.getOrderId());
                        assertTrue(preLedgers.isEmpty(),
                                        "Không được có bản ghi Financial Ledger trước khi thanh toán!");

                        login(page, moderatorEntity.getEmail(), "password123");
                        page.navigate(getBaseUrl() + "/moderator/orders/" + testOrderCode);
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                        evidencePause(page, "1. BSMS Payment Link Generated & Order Approved in PENDING_PAYMENT");

                        // 2. Simulate VNPay cancellation / failure callback (ResponseCode = "24":
                        // Customer cancelled transaction)
                        Map<String, String> params = new TreeMap<>();
                        params.put("vnp_Amount", "200000000"); // 2,000,000 * 100
                        params.put("vnp_BankCode", "NCB");
                        params.put("vnp_CardType", "ATM");
                        params.put("vnp_OrderInfo", "Thanh toan don hang BSMS:" + testOrderCode);
                        params.put("vnp_PayDate", "20260813200000");
                        params.put("vnp_ResponseCode", "24"); // 24 = Giao dịch bị hủy bởi khách hàng
                        params.put("vnp_TmnCode",
                                        VNPayConfig.vnp_TmnCode != null ? VNPayConfig.vnp_TmnCode : "TEST_TMN");
                        params.put("vnp_TransactionNo", "0");
                        params.put("vnp_TransactionStatus", "02"); // 02 = Không thành công
                        params.put("vnp_TxnRef", testOrderCode);

                        StringBuilder sb = new StringBuilder();
                        Iterator<String> itr = params.keySet().iterator();
                        while (itr.hasNext()) {
                                String key = itr.next();
                                String val = params.get(key);
                                sb.append(key).append('=').append(URLEncoder.encode(val, StandardCharsets.US_ASCII));
                                if (itr.hasNext()) {
                                        sb.append('&');
                                }
                        }
                        String secureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());
                        String callbackUrl = getBaseUrl() + "/vnpay/payment-callback?" + sb.toString()
                                        + "&vnp_SecureHash="
                                        + secureHash;

                        page.navigate(callbackUrl);
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

                        evidencePause(page, "2. Unsuccessful / Cancelled VNPay Callback Executed");

                        // Assert payment-result page displays unsuccessful message
                        assertTrue(
                                        page.content().contains("thất bại") || page.content().contains("hủy")
                                                        || page.content().contains("FAILED"),
                                         "Trang kết quả thanh toán phải thông báo thanh toán thất bại/bị hủy!");

                        evidencePause(page, "3. BSMS Payment Result Page Displays Unsuccessful Payment Result");

                        // 3. Open Guest Order Lookup UI and verify order remains unpaid in PENDING_PAYMENT status
                        page.navigate(getBaseUrl() + "/order/lookup?orderCode=" + testOrderCode);
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

                        assertTrue(page.content().contains(testOrderCode),
                                        "Trang tra cứu phải tìm thấy mã đơn " + testOrderCode);
                        assertTrue(page.content().contains("Chờ thanh toán")
                                        || page.content().contains("PENDING_PAYMENT"),
                                        "Trang tra cứu đơn hàng phải hiển thị trạng thái 'Chờ thanh toán' (PENDING_PAYMENT)!");

                        evidencePause(page,
                                        "4. Guest Order Lookup Loaded & Order Status Verified as Non-Paid (PENDING_PAYMENT)");

                        // 4. Final DB assertions
                        Order dbOrderPost = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
                        assertEquals("PENDING_PAYMENT", dbOrderPost.getOrderStatus(),
                                        "Trạng thái DB thực tế của đơn sau khi hủy thanh toán cọc phải giữ nguyên PENDING_PAYMENT!");
                        assertNotEquals("DEPOSITED", dbOrderPost.getOrderStatus(),
                                        "Đơn hàng không được chuyển sang DEPOSITED!");
                        assertNotEquals("PAID", dbOrderPost.getOrderStatus(), "Đơn hàng không được chuyển sang PAID!");
                        assertNotEquals("COMPLETED", dbOrderPost.getOrderStatus(),
                                        "Đơn hàng không được chuyển sang COMPLETED!");

                        Product dbProductPost = productRepository.findById(testProduct.getProductId()).orElseThrow();
                        assertEquals("RESERVED", dbProductPost.getProductStatus(),
                                        "Sản phẩm phải tiếp tục giữ trạng thái RESERVED!");

                        List<Payment> postPayments = paymentRepository
                                        .findByOrderOrderIdOrderByPaymentIdAsc(dbOrderPost.getOrderId());
                        boolean postHasSuccess = postPayments.stream()
                                        .anyMatch(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()));
                        assertFalse(postHasSuccess, "Không được có bản ghi Payment nào ở trạng thái SUCCESS!");

                        boolean postHasFailed = postPayments.stream()
                                        .anyMatch(p -> "FAILED".equalsIgnoreCase(p.getPaymentStatus()));
                        assertTrue(postHasFailed, "Bản ghi Payment đặt cọc phải được chuyển sang trạng thái FAILED!");

                        List<FinancialLedger> postLedgers = financialLedgerRepository
                                        .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                        dbOrderPost.getOrderId());
                        assertTrue(postLedgers.isEmpty(),
                                        "Không được ghi nhận bất kỳ bản ghi doanh thu Financial Ledger nào!");

                        evidencePause(page, "5. All DB & UI Assertions Passed Successfully");
                }
        }
}
