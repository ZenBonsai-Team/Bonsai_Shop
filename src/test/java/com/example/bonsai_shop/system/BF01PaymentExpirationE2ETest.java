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
import com.example.bonsai_shop.product.service.OrderExpirationService;

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
 * L3 SYSTEM TEST (E2E Automated Browser & Expiration Scheduler Test -
 * TC-L3-BF01-005)
 * Business Flow: BF-01 Payment Expiration after 15 Minutes
 *
 * <p>
 * <strong>Test Scenario:</strong>
 * <ol>
 * <li>Create an order in PENDING_PAYMENT with product RESERVED and orderDate
 * past the 15-minute cutoff.</li>
 * <li>Invoke the real system expiration service
 * (OrderExpirationService.cancelExpiredOrders()).</li>
 * <li>Verify on UI: Order Lookup shows CANCELLED; Marketplace Product Detail
 * shows tree is AVAILABLE.</li>
 * <li>Assert database: Order is CANCELLED, Payment is EXPIRED, Product is
 * AVAILABLE, no FinancialLedger revenue.</li>
 * <li>Verify that an expired order's payment callback cannot transition it to
 * PAID.</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BF01PaymentExpirationE2ETest {

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
        private OrderExpirationService orderExpirationService;

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

        @BeforeAll
        void setUpAll() {
                Playwright.CreateOptions createOptions = new Playwright.CreateOptions();
                Map<String, String> env = new HashMap<>(System.getenv());
                env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
                createOptions.setEnv(env);
                playwright = Playwright.create(createOptions);

                browser = playwright.chromium().launch(
                                new BrowserType.LaunchOptions()
                                                .setHeadless(false)
                                                .setSlowMo(1500));

                Role moderatorRole = roleRepository.findByRoleName("MODERATOR")
                                .orElseGet(() -> roleRepository.save(Role.builder().roleName("MODERATOR").build()));

                String modEmail = "moderator.expire.e2e@test.com";
                userRepository.findByEmail(modEmail).ifPresent(u -> {
                        u.setPassword(passwordEncoder.encode("password123"));
                        u.setStatus("ACTIVE");
                        u.setRole(moderatorRole);
                        userRepository.save(u);
                });
                moderatorEntity = userRepository.findByEmail(modEmail)
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .fullName("Order Moderator (TC-005)")
                                                .email(modEmail)
                                                .username("moderator_expire_e2e")
                                                .password(passwordEncoder.encode("password123"))
                                                .role(moderatorRole)
                                                .status("ACTIVE")
                                                .phone("0905554444")
                                                .build()));

                Category category = categoryRepository.findAll().stream().findFirst()
                                .orElseGet(() -> categoryRepository.save(Category.builder()
                                                .categoryName("E2E Category TC005")
                                                .description("Category for TC-L3-BF01-005")
                                                .build()));

                Variety variety = varietyRepository.findAll().stream().findFirst()
                                .orElseGet(() -> varietyRepository.save(Variety.builder()
                                                .category(category)
                                                .varietyName("Bonsai Linh Sam TC005")
                                                .description("Variety for TC-L3-BF01-005")
                                                .build()));

                ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                                                .segmentName("Standard Segment TC005")
                                                .build()));

                testProduct = Product.builder()
                                .productCode("TC005-TREE-" + System.currentTimeMillis())
                                .productName("Cây Bonsai Linh Sam Sông Hinh TC-L3-BF01-005")
                                .price(new BigDecimal("3500000"))
                                .productStatus("RESERVED")
                                .isVisible(true)
                                .isPublicPrice(true)
                                .variety(variety)
                                .segment(segment)
                                .age(12)
                                .height(65.0f)
                                .trunkDiameter(10.0f)
                                .style("Thác đổ")
                                .description("Cây Linh Sam thử nghiệm hết hạn thanh toán 15 phút TC-L3-BF01-005")
                                .createdBy(moderatorEntity)
                                .createdAt(LocalDateTime.now())
                                .build();
                testProduct = productRepository.save(testProduct);

                // Order created 20 minutes ago (> 15 minutes cutoff)
                testOrderCode = "BSMS-TC-BF01-005-" + System.currentTimeMillis();
                testOrder = Order.builder()
                                .orderCode(testOrderCode)
                                .customerName("Lê Khách Hàng Expire TC005")
                                .customerPhone("0933445566")
                                .customerEmail("customer.tc005@test.com")
                                .shippingAddress("12 Đường Láng, Quận Đống Đa, Hà Nội")
                                .orderDate(LocalDateTime.now().minusMinutes(20))
                                .totalAmount(new BigDecimal("3500000"))
                                .depositAmount(new BigDecimal("1000000"))
                                .orderStatus("PENDING_PAYMENT")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .notes("Đơn hàng test hết hạn thanh toán sau 15 phút TC-L3-BF01-005")
                                .assignedTo(moderatorEntity)
                                .assignedAt(LocalDateTime.now().minusMinutes(18))
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

                Payment pendingPayment = Payment.builder()
                                .order(testOrder)
                                .paymentType("DEPOSIT")
                                .paymentMethod("DEPOSIT")
                                .paymentStatus("PENDING")
                                .amount(new BigDecimal("1000000"))
                                .build();
                paymentRepository.save(pendingPayment);
        }

        @AfterAll
        void tearDownAll() {
                try {
                        if (testOrderCode != null) {
                                orderRepository.findByOrderCode(testOrderCode).ifPresent(o -> {
                                        financialLedgerRepository.deleteAll(financialLedgerRepository
                                                        .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                                        o.getOrderId()));
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

        @Test
        @DisplayName("TC-L3-BF01-005: Payment Expiration after 15 Minutes")
        void tcL3Bf01005_paymentExpirationAfter15Minutes() {
                try (BrowserContext context = browser.newContext()) {
                        Page page = context.newPage();

                        // 1. Initial State Assertions (Pre-expiration)
                        Order preOrder = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
                        assertEquals("PENDING_PAYMENT", preOrder.getOrderStatus(),
                                        "Đơn ban đầu ở trạng thái PENDING_PAYMENT");
                        Product preProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
                        assertEquals("RESERVED", preProduct.getProductStatus(), "Cây đang được giữ hàng (RESERVED)");

                        // 2. Trigger real system expiration logic
                        orderExpirationService.cancelExpiredOrders();

                        // 3. UI Verification: Guest Order Lookup
                        page.navigate(getBaseUrl() + "/order/lookup?orderCode=" + testOrderCode);
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                        evidencePause(page, "1. Guest Order Lookup Loaded for Expired Order");

                        assertTrue(page.content().contains(testOrderCode),
                                        "Trang tra cứu tìm thấy mã đơn " + testOrderCode);
                        assertTrue(page.content().contains("Đã hủy") || page.content().contains("CANCELLED"),
                                        "Trang tra cứu đơn hàng phải hiển thị trạng thái 'Đã hủy' (CANCELLED) do quá hạn!");

                        // 4. UI Verification: Marketplace Product Detail Page
                        page.navigate(getBaseUrl() + "/product/" + testProduct.getProductId());
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                        evidencePause(page, "2. Marketplace Product Detail Page - Product Released to Public");

                        assertFalse(page.content().contains("Đang giữ hàng") && !page.content().contains("Còn hàng"),
                                        "Cây đã được giải phóng kho về trạng thái mở bán công khai!");

                        // 5. Database Assertions
                        Order postOrder = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
                        assertEquals("CANCELLED", postOrder.getOrderStatus(),
                                        "Đơn hàng sau khi quá hạn 15 phút phải chuyển sang trạng thái CANCELLED!");
                        assertTrue(postOrder.getNotes() != null && postOrder.getNotes().contains("quá hạn"),
                                        "Ghi chú đơn hàng phải lưu rõ lý do tự động hủy do quá hạn thanh toán!");

                        Product postProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
                        assertEquals("AVAILABLE", postProduct.getProductStatus(),
                                        "Sản phẩm sau khi hủy đơn quá hạn phải quay về trạng thái AVAILABLE!");

                        List<Payment> postPayments = paymentRepository
                                        .findByOrderOrderIdOrderByPaymentIdAsc(postOrder.getOrderId());
                        assertFalse(postPayments.isEmpty(), "Danh sách thanh toán không được rỗng");
                        assertEquals("EXPIRED", postPayments.get(0).getPaymentStatus(),
                                        "Bản ghi Payment PENDING phải chuyển sang EXPIRED!");

                        List<FinancialLedger> ledgers = financialLedgerRepository
                                        .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                        postOrder.getOrderId());
                        assertTrue(ledgers.isEmpty(),
                                        "Không được ghi nhận bất kỳ khoản doanh thu FinancialLedger nào cho đơn hết hạn!");

                        // 6. Security Check: Expired Payment Link Replay cannot revive order to PAID
                        Map<String, String> params = new TreeMap<>();
                        params.put("vnp_Amount", "100000000"); // 1,000,000 * 100
                        params.put("vnp_BankCode", "NCB");
                        params.put("vnp_CardType", "ATM");
                        params.put("vnp_OrderInfo", "Thanh toan don hang BSMS:" + testOrderCode);
                        params.put("vnp_PayDate", "20260813200000");
                        params.put("vnp_ResponseCode", "00");
                        params.put("vnp_TmnCode",
                                        VNPayConfig.vnp_TmnCode != null ? VNPayConfig.vnp_TmnCode : "TEST_TMN");
                        params.put("vnp_TransactionNo", "14000001");
                        params.put("vnp_TransactionStatus", "00");
                        params.put("vnp_TxnRef", testOrderCode);

                        StringBuilder sb = new StringBuilder();
                        Iterator<String> itr = params.keySet().iterator();
                        while (itr.hasNext()) {
                                String key = itr.next();
                                String val = params.get(key);
                                sb.append(key).append('=').append(URLEncoder.encode(val, StandardCharsets.US_ASCII));
                                if (itr.hasNext())
                                        sb.append('&');
                        }
                        String secureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());
                        String callbackUrl = getBaseUrl() + "/vnpay/payment-callback?" + sb.toString()
                                        + "&vnp_SecureHash=" + secureHash;

                        page.navigate(callbackUrl);
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                        evidencePause(page, "3. Late Payment Callback Replay on Expired Order");

                        Order afterReplayOrder = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
                        assertNotEquals("DEPOSITED", afterReplayOrder.getOrderStatus(),
                                        "Đơn đã hết hạn không được đổi sang DEPOSITED!");
                        assertNotEquals("COMPLETED", afterReplayOrder.getOrderStatus(),
                                        "Đơn đã hết hạn không được đổi sang COMPLETED!");

                        Product afterReplayProduct = productRepository.findById(testProduct.getProductId())
                                        .orElseThrow();
                        assertEquals("AVAILABLE", afterReplayProduct.getProductStatus(),
                                        "Cây vẫn phải giữ nguyên trạng thái AVAILABLE!");

                        evidencePause(page, "4. All TC-005 DB and UI Assertions Passed");
                }
        }
}
