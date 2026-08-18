package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.EmailService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
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
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3 SYSTEM TEST (E2E Automated Browser Test - TC-L3-BF01-002)
 * Business Flow: BF-01 Order Claim and Approval Authorization
 * Actors: Order Moderator A, Order Moderator B
 *
 * <p><strong>Test Coverage:</strong>
 * <ol>
 *   <li>Moderator A claims an unassigned PENDING order from Orders Pool.</li>
 *   <li>Verify exclusivity: Order disappears from Orders Pool, appears in Moderator A's My Orders, and Moderator B cannot claim or process it.</li>
 *   <li>Moderator A opens Order Detail, enters custom fees (Deposit, Shipping Fee, Crane Fee), and approves via UI.</li>
 *   <li>Assert fees are stored accurately (no automatic 30% deposit applied), order transitions to PENDING_PAYMENT, pending Payment record is created, and OrderLog/OrderHandling history is recorded.</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BF01OrderClaimAndApprovalAuthorizationE2ETest {

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
    private OrderHandlingRepository orderHandlingRepository;

    @Autowired
    private OrderLogRepository orderLogRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FinancialLedgerRepository financialLedgerRepository;

    @Autowired
    private OrderService orderService;

    private Playwright playwright;
    private Browser browser;

    private User moderatorAEntity;
    private User moderatorBEntity;
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

        // Create or sync Moderator A account
        String modAEmail = "moderator.a.claim.e2e@test.com";
        userRepository.findByEmail(modAEmail).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(moderatorRole);
            userRepository.save(u);
        });
        moderatorAEntity = userRepository.findByEmail(modAEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Order Moderator A (TC-002)")
                        .email(modAEmail)
                        .username("moderator_a_claim_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(moderatorRole)
                        .status("ACTIVE")
                        .phone("0901234567")
                        .build()));

        // Create or sync Moderator B account
        String modBEmail = "moderator.b.claim.e2e@test.com";
        userRepository.findByEmail(modBEmail).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(moderatorRole);
            userRepository.save(u);
        });
        moderatorBEntity = userRepository.findByEmail(modBEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Order Moderator B (TC-002)")
                        .email(modBEmail)
                        .username("moderator_b_claim_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(moderatorRole)
                        .status("ACTIVE")
                        .phone("0909998888")
                        .build()));

        // Create Category, Variety, Segment for test product
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("E2E Category TC002")
                        .description("Category for TC-L3-BF01-002")
                        .build()));

        Variety variety = varietyRepository.findAll().stream().findFirst()
                .orElseGet(() -> varietyRepository.save(Variety.builder()
                        .category(category)
                        .varietyName("Bonsai Sanh TC002")
                        .description("Variety for TC-L3-BF01-002")
                        .build()));

        ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                        .segmentName("Standard Segment TC002")
                        .build()));

        testProduct = Product.builder()
                .productCode("TC002-TREE-" + System.currentTimeMillis())
                .productName("Cây Bonsai Sanh Nam Điền TC-L3-BF01-002")
                .price(new BigDecimal("4500000")) // Base tree price: 4,500,000 VNĐ
                .productStatus("RESERVED")
                .isVisible(true)
                .isPublicPrice(true)
                .variety(variety)
                .segment(segment)
                .age(15)
                .height(80.0f)
                .trunkDiameter(12.0f)
                .style("Trực quân tử")
                .description("Cây Bonsai Sanh thử nghiệm claim & duyệt đơn TC-L3-BF01-002")
                .createdBy(moderatorAEntity)
                .createdAt(LocalDateTime.now())
                .build();
        testProduct = productRepository.save(testProduct);

        // Create 1 dedicated unassigned PENDING test order
        testOrderCode = "BSMS-TC-BF01-002-" + System.currentTimeMillis();
        testOrder = Order.builder()
                .orderCode(testOrderCode)
                .customerName("Nguyễn Văn Khách Claim TC002")
                .customerPhone("0988000111")
                .customerEmail("customer.tc002@test.com")
                .shippingAddress("789 Đường Lạc Long Quân, Quận Tây Hồ, Hà Nội")
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("4500000"))
                .depositAmount(BigDecimal.ZERO)
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .craneFee(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .notes("Đơn hàng test claim và approval cho TC-L3-BF01-002")
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
                .amount(new BigDecimal("4500000"))
                .build();
        paymentRepository.save(initialPayment);
    }

    @AfterAll
    void tearDownAll() {
        try {
            // Clean up isolated test data created by this test
            if (testOrderCode != null) {
                orderRepository.findByOrderCode(testOrderCode).ifPresent(o -> {
                    financialLedgerRepository.deleteAll(financialLedgerRepository.findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(o.getOrderId()));
                    orderRepository.delete(o);
                });
            }
            if (testProduct != null && testProduct.getProductId() != null) {
                productRepository.findById(testProduct.getProductId()).ifPresent(p -> productRepository.delete(p));
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
     * TC-L3-BF01-002: Order Claim and Approval Authorization.
     */
    @Test
    @DisplayName("TC-L3-BF01-002: Order Claim and Approval Authorization")
    void tcL3Bf01002_orderClaimAndApprovalAuthorization() {
        try (BrowserContext contextA = browser.newContext()) {
            Page pageA = contextA.newPage();

            // =========================================================================
            // 1. Moderator A logs in and navigates to Orders Pool
            // =========================================================================
            login(pageA, moderatorAEntity.getEmail(), "password123");
            evidencePause(pageA, "1. Moderator A Logged In Successfully");

            pageA.navigate(getBaseUrl() + "/moderator/orders/pool");
            pageA.waitForLoadState(LoadState.DOMCONTENTLOADED);

            pageA.waitForSelector("#ordersTableBody tr:has-text('" + testOrderCode + "')",
                    new Page.WaitForSelectorOptions().setTimeout(10000));

            Locator orderRow = pageA.locator("#ordersTableBody tr:has-text('" + testOrderCode + "')").first();
            assertTrue(orderRow.isVisible(), "Đơn hàng test TC-002 phải hiển thị trong Kho đơn chung (Orders Pool)!");

            // DB assertion before claim
            Order dbOrderBefore = orderRepository.findByOrderCode(testOrderCode)
                    .orElseThrow(() -> new AssertionError("Không tìm thấy đơn hàng " + testOrderCode + " trong DB!"));
            assertEquals("PENDING", dbOrderBefore.getOrderStatus(), "Trạng thái đơn trước khi claim phải là PENDING!");
            assertNull(dbOrderBefore.getAssignedTo(), "Đơn mới tạo chưa có Moderator phụ trách (assignedTo = null)!");

            evidencePause(pageA, "2. Orders Pool Loaded with Unassigned PENDING Order");

            // =========================================================================
            // 2. Moderator A claims the order
            // =========================================================================
            orderRow.locator("button.btn-claim-action").click();
            pageA.waitForTimeout(2000);

            // DB assertions after claim
            Order dbOrderAfterClaim = orderRepository.findByOrderCode(testOrderCode)
                    .orElseThrow(() -> new AssertionError("Không tìm thấy đơn hàng " + testOrderCode + " sau khi claim!"));

            assertNotNull(dbOrderAfterClaim.getAssignedTo(), "Đơn hàng phải được gán cho Moderator sau khi claim!");
            assertEquals(moderatorAEntity.getUserId(), dbOrderAfterClaim.getAssignedTo().getUserId(),
                    "UserId của Moderator phụ trách phải khớp với Moderator A!");
            assertEquals("PENDING", dbOrderAfterClaim.getOrderStatus(),
                    "Trạng thái đơn hàng sau claim giữ nguyên PENDING!");

            evidencePause(pageA, "3. Claim Action Completed by Moderator A");

            // Assert UI: order is no longer in Orders Pool
            pageA.navigate(getBaseUrl() + "/moderator/orders/pool");
            pageA.waitForLoadState(LoadState.DOMCONTENTLOADED);
            pageA.waitForTimeout(1000);
            assertFalse(pageA.content().contains(testOrderCode),
                    "Đơn hàng sau khi claim không còn hiển thị trong Kho đơn chung!");

            // Assert UI: order appears in Moderator A's My Orders
            pageA.navigate(getBaseUrl() + "/moderator/orders/my");
            pageA.waitForLoadState(LoadState.DOMCONTENTLOADED);
            pageA.waitForSelector("body", new Page.WaitForSelectorOptions().setTimeout(10000));
            assertTrue(pageA.content().contains(testOrderCode),
                    "Đơn hàng vừa claim phải xuất hiện trong 'Đơn hàng của tôi' của Moderator A!");

            evidencePause(pageA, "4. Order Appears in Moderator A's My Orders");

            // =========================================================================
            // 3. Verify Exclusivity & RBAC: Moderator B cannot claim or process Moderator A's order
            // =========================================================================
            try (BrowserContext contextB = browser.newContext()) {
                Page pageB = contextB.newPage();
                login(pageB, moderatorBEntity.getEmail(), "password123");

                // Moderator B checks Orders Pool -> Order not present
                pageB.navigate(getBaseUrl() + "/moderator/orders/pool");
                pageB.waitForLoadState(LoadState.DOMCONTENTLOADED);
                assertFalse(pageB.content().contains(testOrderCode),
                        "Moderator B không được thấy đơn của Moderator A trong Kho đơn chung!");

                // Moderator B checks My Orders -> Order not present
                pageB.navigate(getBaseUrl() + "/moderator/orders/my");
                pageB.waitForLoadState(LoadState.DOMCONTENTLOADED);
                assertFalse(pageB.content().contains(testOrderCode),
                        "Moderator B không được thấy đơn của Moderator A trong Đơn hàng của tôi!");

                evidencePause(pageB, "5. Exclusivity Verified: Moderator B Cannot View/Access Claimed Order");
            }

            // Exclusivity at Service layer
            assertThrows(IllegalStateException.class, () -> {
                orderService.claimOrder(testOrderCode, moderatorBEntity);
            }, "Moderator B không thể claim đơn đã được Moderator A nhận!");

            assertThrows(SecurityException.class, () -> {
                orderService.verifyOrder(testOrderCode, new BigDecimal("500000"), new BigDecimal("300000"), new BigDecimal("1500000"), moderatorBEntity);
            }, "Moderator B không thể duyệt đơn được gán cho Moderator A!");

            // =========================================================================
            // 4. Moderator A opens Order Detail, enters custom fees, and approves via UI
            // =========================================================================
            pageA.navigate(getBaseUrl() + "/moderator/orders/" + testOrderCode);
            pageA.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(pageA, "6. Moderator A Viewing Order Detail for Approval");

            // Enter valid custom fees:
            // - Deposit Amount: 1,500,000 VNĐ (Explicitly NOT the 30% automatic value 1,350,000 VNĐ)
            // - Shipping Fee: 300,000 VNĐ
            // - Crane Fee: 500,000 VNĐ
            pageA.fill("#approvalDepositAmount", "1500000");
            pageA.fill("#approvalShippingFee", "300000");
            pageA.fill("#approvalCraneFee", "500000");

            evidencePause(pageA, "7. Moderator A Entered Custom Deposit (1.5M), Shipping Fee (300k), Crane Fee (500k)");

            // Click "Duyệt đơn" button
            pageA.click("button.od-action-btn.approve");

            // Confirmation modal
            pageA.waitForSelector("#orderActionConfirmModal.show", new Page.WaitForSelectorOptions().setTimeout(5000));
            evidencePause(pageA, "8. Approval Confirmation Modal Displayed");

            // Confirm approval
            pageA.click("#confirmModalConfirm");

            // Wait for reload
            pageA.waitForTimeout(3000);
            evidencePause(pageA, "9. Order Approved via UI & Page Reloaded");

            // =========================================================================
            // 5. Assert Database & UI State after Approval
            // =========================================================================
            Order approvedOrder = orderRepository.findByOrderCodeWithDetails(testOrderCode).orElseThrow();

            // Status transitioned to PENDING_PAYMENT
            assertEquals("PENDING_PAYMENT", approvedOrder.getOrderStatus(),
                    "Trạng thái đơn hàng sau duyệt phải là PENDING_PAYMENT!");

            // Exclusivity preserved
            assertNotNull(approvedOrder.getAssignedTo());
            assertEquals(moderatorAEntity.getUserId(), approvedOrder.getAssignedTo().getUserId(),
                    "Đơn hàng vẫn thuộc quyền quản lý của Moderator A!");

            // Fees saved accurately as entered
            assertEquals(0, new BigDecimal("1500000").compareTo(approvedOrder.getDepositAmount()),
                    "Tiền đặt cọc phải chính xác là 1,500,000 VNĐ!");
            assertNotEquals(0, new BigDecimal("1350000").compareTo(approvedOrder.getDepositAmount()),
                    "Tiền đặt cọc không được bị tính tự động 30% (1,350,000 VNĐ)!");

            assertEquals(0, new BigDecimal("300000").compareTo(approvedOrder.getShippingFee()),
                    "Phí vận chuyển phải chính xác là 300,000 VNĐ!");
            assertEquals(0, new BigDecimal("500000").compareTo(approvedOrder.getCraneFee()),
                    "Phí xe cẩu phải chính xác là 500,000 VNĐ!");

            // Total Amount = 4.5M (Tree) + 300k (Ship) + 500k (Crane) = 5,300,000 VNĐ
            assertEquals(0, new BigDecimal("5300000").compareTo(approvedOrder.getTotalAmount()),
                    "Tổng tiền đơn hàng sau duyệt phải bằng 5,300,000 VNĐ!");

            // Product status remains RESERVED
            Product updatedProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("RESERVED", updatedProduct.getProductStatus(),
                    "Trạng thái cây vẫn là RESERVED chờ khách thanh toán cọc!");

            // Pending DEPOSIT Payment record created
            List<Payment> payments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(approvedOrder.getOrderId());
            assertFalse(payments.isEmpty(), "Bản ghi Payment phải được tạo sau khi duyệt đơn!");

            Payment depositPayment = payments.get(0);
            assertEquals("DEPOSIT", depositPayment.getPaymentType(), "Loại thanh toán phải là DEPOSIT!");
            assertEquals("PENDING", depositPayment.getPaymentStatus(), "Trạng thái Payment phải là PENDING!");
            assertEquals(0, new BigDecimal("1500000").compareTo(depositPayment.getAmount()),
                    "Số tiền thanh toán cọc phải là 1,500,000 VNĐ!");

            // OrderHandling history recorded
            List<OrderHandling> handlings = orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(approvedOrder.getOrderId());
            assertFalse(handlings.isEmpty(), "Phải ghi nhận lịch sử OrderHandling của Moderator A!");

            // OrderLog recorded VERIFY action
            List<OrderLog> logs = orderLogRepository.findByOrderOrderIdOrderByActionAtAsc(approvedOrder.getOrderId());
            boolean hasVerifyLog = logs.stream().anyMatch(l -> "VERIFY".equalsIgnoreCase(l.getActionType()));
            assertTrue(hasVerifyLog, "Phải có bản ghi OrderLog với actionType VERIFY!");

            // UI on Order Detail displays PENDING_PAYMENT status
            pageA.navigate(getBaseUrl() + "/moderator/orders/" + testOrderCode);
            pageA.waitForLoadState(LoadState.DOMCONTENTLOADED);
            assertTrue(pageA.content().contains("PENDING_PAYMENT") || pageA.content().contains("Chờ thanh toán"),
                    "Giao diện chi tiết đơn hàng phải hiển thị trạng thái Chờ thanh toán / PENDING_PAYMENT!");

            evidencePause(pageA, "10. All Claim & Approval DB and UI Assertions Passed Successfully");
        }
    }
}
