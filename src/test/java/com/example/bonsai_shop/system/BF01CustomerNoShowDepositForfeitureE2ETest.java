package com.example.bonsai_shop.system;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.EmailService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderLog;
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
import com.example.bonsai_shop.product.repository.OrderLogRepository;
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
import com.microsoft.playwright.Locator;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3 SYSTEM TEST (E2E Automated Browser Test - TC-L3-BF01-006)
 * Business Flow: BF-01 Customer No-Show after Successful Deposit Payment
 * Actor: Order Moderator
 *
 * <p>
 * <strong>Test Scenario:</strong>
 * <ol>
 * <li>Create an order, approve deposit amount (1,200,000 VNĐ) and process valid
 * VNPay callback to reach DEPOSITED status.</li>
 * <li>Moderator navigates to Order Detail via UI and triggers "Khách từ chối
 * nhận hàng" action.</li>
 * <li>Moderator enters mandatory justification in confirmation modal and
 * confirms cancellation.</li>
 * <li>Assert DB & UI: Order is CANCELLED, Product is released back to
 * AVAILABLE, deposit is forfeited into FinancialLedger as INCOME, no refund is
 * generated, and OrderLog records FORFEITED_DEPOSIT_INCOME_RECORDED.</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BF01CustomerNoShowDepositForfeitureE2ETest {

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
        private OrderLogRepository orderLogRepository;

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

                String modEmail = "moderator.noshow.e2e@test.com";
                userRepository.findByEmail(modEmail).ifPresent(u -> {
                        u.setPassword(passwordEncoder.encode("password123"));
                        u.setStatus("ACTIVE");
                        u.setRole(moderatorRole);
                        userRepository.save(u);
                });
                moderatorEntity = userRepository.findByEmail(modEmail)
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .fullName("Order Moderator (TC-006)")
                                                .email(modEmail)
                                                .username("moderator_noshow_e2e")
                                                .password(passwordEncoder.encode("password123"))
                                                .role(moderatorRole)
                                                .status("ACTIVE")
                                                .phone("0907776666")
                                                .build()));

                Category category = categoryRepository.findAll().stream().findFirst()
                                .orElseGet(() -> categoryRepository.save(Category.builder()
                                                .categoryName("E2E Category TC006")
                                                .description("Category for TC-L3-BF01-006")
                                                .build()));

                Variety variety = varietyRepository.findAll().stream().findFirst()
                                .orElseGet(() -> varietyRepository.save(Variety.builder()
                                                .category(category)
                                                .varietyName("Bonsai Si Cổ TC006")
                                                .description("Variety for TC-L3-BF01-006")
                                                .build()));

                ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                                                .segmentName("Standard Segment TC006")
                                                .build()));

                testProduct = Product.builder()
                                .productCode("TC006-TREE-" + System.currentTimeMillis())
                                .productName("Cây Bonsai Si Búp Đỏ TC-L3-BF01-006")
                                .price(new BigDecimal("4000000"))
                                .productStatus("RESERVED")
                                .isVisible(true)
                                .isPublicPrice(true)
                                .variety(variety)
                                .segment(segment)
                                .age(18)
                                .height(75.0f)
                                .trunkDiameter(14.0f)
                                .style("Dáng võng")
                                .description("Cây Si test bùng cọc TC-L3-BF01-006")
                                .createdBy(moderatorEntity)
                                .createdAt(LocalDateTime.now())
                                .build();
                testProduct = productRepository.save(testProduct);

                testOrderCode = "BSMS-TC-BF01-006-" + System.currentTimeMillis();
                testOrder = Order.builder()
                                .orderCode(testOrderCode)
                                .customerName("Phạm Văn Bùng Cọc TC006")
                                .customerPhone("0944556677")
                                .customerEmail("customer.tc006@test.com")
                                .shippingAddress("88 Phố Huế, Quận Hai Bà Trưng, Hà Nội")
                                .orderDate(LocalDateTime.now())
                                .totalAmount(new BigDecimal("4000000"))
                                .depositAmount(BigDecimal.ZERO)
                                .orderStatus("PENDING")
                                .orderType("ONLINE")
                                .craneFee(BigDecimal.ZERO)
                                .shippingFee(BigDecimal.ZERO)
                                .notes("Đơn hàng test khách không nhận hàng & tịch thu cọc TC-L3-BF01-006")
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

                Payment initialPayment = Payment.builder()
                                .order(testOrder)
                                .paymentType("DEPOSIT")
                                .paymentMethod("DEPOSIT")
                                .paymentStatus("PENDING")
                                .amount(new BigDecimal("4000000"))
                                .build();
                paymentRepository.save(initialPayment);
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
        @DisplayName("TC-L3-BF01-006: Customer No-Show After Deposit Payment & Deposit Forfeiture")
        void tcL3Bf01006_customerNoShowDepositForfeiture() {
                try (BrowserContext context = browser.newContext()) {
                        Page page = context.newPage();

                        // 1. Moderator claims and approves order with deposit 1,200,000 VNĐ
                        orderService.claimOrder(testOrderCode, moderatorEntity);
                        orderService.verifyOrder(
                                        testOrderCode,
                                        new BigDecimal("500000"), // Crane fee
                                        new BigDecimal("300000"), // Shipping fee
                                        new BigDecimal("1200000"), // Deposit amount
                                        moderatorEntity);

                        // 2. Process VNPay success callback for deposit payment
                        Map<String, String> params = new TreeMap<>();
                        params.put("vnp_Amount", "120000000"); // 1,200,000 * 100
                        params.put("vnp_BankCode", "NCB");
                        params.put("vnp_CardType", "ATM");
                        params.put("vnp_OrderInfo", "Thanh toan tien coc BSMS:" + testOrderCode);
                        params.put("vnp_PayDate", "20260814100000");
                        params.put("vnp_ResponseCode", "00");
                        params.put("vnp_TmnCode",
                                        VNPayConfig.vnp_TmnCode != null ? VNPayConfig.vnp_TmnCode : "TEST_TMN");
                        params.put("vnp_TransactionNo", "14000002");
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
                        evidencePause(page, "1. Deposit Payment Callback Successful -> Status DEPOSITED");

                        // Verify order transitioned to DEPOSITED
                        Order depositedOrder = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
                        assertEquals("DEPOSITED", depositedOrder.getOrderStatus(),
                                        "Đơn hàng phải ở trạng thái DEPOSITED sau khi thanh toán cọc");

                        // 3. Moderator logs in and navigates to Order Detail
                        login(page, moderatorEntity.getEmail(), "password123");
                        page.navigate(getBaseUrl() + "/moderator/orders/" + testOrderCode);
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                        evidencePause(page, "2. Moderator Viewing DEPOSITED Order Detail");

                        // 4. Click action button: "Khách từ chối nhận hàng"
                        Locator noShowBtn = page.locator("button.od-action-btn:has-text('Khách từ chối nhận hàng')")
                                        .first();
                        assertTrue(noShowBtn.isVisible(),
                                        "Nút 'Khách từ chối nhận hàng' phải hiển thị cho đơn DEPOSITED!");
                        noShowBtn.click();

                        // Wait for confirmation modal
                        page.waitForSelector("#orderActionConfirmModal.show",
                                        new Page.WaitForSelectorOptions().setTimeout(5000));
                        evidencePause(page, "3. Customer No-Show Confirmation Modal Opened");

                        // Enter mandatory reason
                        page.fill("#confirmModalInput", "Khách hàng từ chối nhận cây khi xe giao đến chân công trình");
                        evidencePause(page, "4. Reason Entered for Customer No-Show");

                        // Confirm action
                        page.click("#confirmModalConfirm");
                        page.waitForTimeout(3000);
                        evidencePause(page, "5. Customer No-Show Confirmed & Order Cancelled via UI");

                        // 5. UI Verification: Order Detail displays CANCELLED
                        page.navigate(getBaseUrl() + "/moderator/orders/" + testOrderCode);
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                        assertTrue(page.content().contains("CANCELLED") || page.content().contains("Đã hủy"),
                                        "Chi tiết đơn hàng phải hiển thị trạng thái Đã hủy / CANCELLED!");

                        // 6. Database Assertions
                        Order cancelledOrder = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
                        assertEquals("CANCELLED", cancelledOrder.getOrderStatus(),
                                        "Trạng thái đơn hàng phải chuyển sang CANCELLED!");
                        assertTrue(cancelledOrder.getNotes() != null && cancelledOrder.getNotes().contains("giữ lại"),
                                        "Ghi chú đơn hàng phải lưu rõ việc giữ lại tiền cọc do lỗi khách hàng!");

                        // Product released back to AVAILABLE
                        Product releasedProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
                        assertEquals("AVAILABLE", releasedProduct.getProductStatus(),
                                        "Cây phải được giải phóng về trạng thái AVAILABLE sau khi hủy đơn bùng cọc!");

                        // FinancialLedger records forfeited deposit income
                        List<FinancialLedger> ledgers = financialLedgerRepository
                                        .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(
                                                        cancelledOrder.getOrderId());
                        assertFalse(ledgers.isEmpty(), "Phải có bản ghi FinancialLedger ghi nhận tiền cọc tịch thu!");

                        FinancialLedger forfeitureLedger = ledgers.stream()
                                        .filter(l -> l.getLedgerType().name().contains("FORFEITED")
                                                        || "INCOME".equalsIgnoreCase(l.getDirection().name()))
                                        .findFirst()
                                        .orElseThrow(() -> new AssertionError(
                                                        "Không tìm thấy bản ghi Sổ cái loại FORFEITED_DEPOSIT_INCOME!"));

                        assertEquals("INCOME", forfeitureLedger.getDirection().name(),
                                        "Khoản tiền cọc tịch thu phải là dòng tiền INCOME!");
                        assertEquals(0, new BigDecimal("1200000").compareTo(forfeitureLedger.getAmount()),
                                        "Số tiền cọc tịch thu vào Sổ cái phải chính xác là 1,200,000 VNĐ (đúng số tiền đã cọc)!");

                        // Assert no refund entry exists in FinancialLedger
                        boolean hasRefund = ledgers.stream()
                                        .anyMatch(l -> "REFUND".equalsIgnoreCase(l.getLedgerType().name())
                                                        || "EXPENSE".equalsIgnoreCase(l.getDirection().name()));
                        assertFalse(hasRefund,
                                        "Không được tạo bản ghi hoàn tiền (REFUND) khi lỗi thuộc về khách hàng!");

                        // OrderLog records FORFEITED_DEPOSIT_INCOME_RECORDED
                        List<OrderLog> logs = orderLogRepository
                                        .findByOrderOrderIdOrderByActionAtAsc(cancelledOrder.getOrderId());
                        boolean hasForfeitLog = logs.stream().anyMatch(
                                        l -> "FORFEITED_DEPOSIT_INCOME_RECORDED".equalsIgnoreCase(l.getActionType()));
                        assertTrue(hasForfeitLog, "OrderLog phải ghi nhận action FORFEITED_DEPOSIT_INCOME_RECORDED!");

                        evidencePause(page, "6. All TC-006 DB and UI Assertions Passed");
                }
        }
}
