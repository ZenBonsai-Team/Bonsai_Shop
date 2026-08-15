package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.FinancialLedger;
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
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.OrderDetailRepository;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import com.example.bonsai_shop.integration.support.TestDatabaseSafetyInitializer;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3 SYSTEM TEST (E2E Automated Browser Test - TC-L3-BF01-003)
 * Business Flow: BF-01 Order Rejection and Product Release
 * Actor: Order Moderator
 *
 * <p><strong>Three Test Steps:</strong>
 * <ol>
 *   <li><strong>Step 1:</strong> Claim a PENDING order whose product is RESERVED and verify order appears in My Orders with RESERVED product.</li>
 *   <li><strong>Step 2:</strong> Open rejection action, enter a valid reason, and confirm rejection.</li>
 *   <li><strong>Step 3:</strong> Review final order status (CANCELLED), rejection reason, moderator processing history, product release (AVAILABLE), and verify no successful payment/ledger exists.</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BF01OrderRejectionAndProductReleaseE2ETest {

    @LocalServerPort
    private int port;

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
        // Configure Playwright in headed mode with slowMo
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

        // Create or sync Moderator account for TC-L3-BF01-003
        String modEmail = "moderator.rejection.e2e@test.com";
        userRepository.findByEmail(modEmail).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(moderatorRole);
            userRepository.save(u);
        });
        moderatorEntity = userRepository.findByEmail(modEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Order Moderator Rejection (TC-003)")
                        .email(modEmail)
                        .username("moderator_rejection_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(moderatorRole)
                        .status("ACTIVE")
                        .phone("0903334444")
                        .build()));

        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("E2E Category TC003")
                        .description("Category for TC-L3-BF01-003")
                        .build()));

        Variety variety = varietyRepository.findAll().stream().findFirst()
                .orElseGet(() -> varietyRepository.save(Variety.builder()
                        .category(category)
                        .varietyName("Bonsai Cần Thăng TC003")
                        .description("Variety for TC-L3-BF01-003")
                        .build()));

        ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                        .segmentName("Standard Segment TC003")
                        .build()));

        testProduct = Product.builder()
                .productCode("TC003-TREE-" + System.currentTimeMillis())
                .productName("Cây Bonsai Cần Thăng Dáng Thẳng TC-L3-BF01-003")
                .price(new BigDecimal("3500000"))
                .productStatus("RESERVED")
                .isVisible(true)
                .isPublicPrice(true)
                .variety(variety)
                .segment(segment)
                .age(10)
                .height(60.0f)
                .trunkDiameter(8.0f)
                .style("Dáng thẳng")
                .description("Cây Bonsai Cần Thăng đang giữ chỗ thử nghiệm TC-L3-BF01-003")
                .createdBy(moderatorEntity)
                .createdAt(LocalDateTime.now())
                .build();
        testProduct = productRepository.save(testProduct);

        testOrderCode = "BSMS-TC-BF01-003-" + System.currentTimeMillis();
        testOrder = Order.builder()
                .orderCode(testOrderCode)
                .customerName("Nguyễn Văn Khách TC003")
                .customerPhone("0977333222")
                .customerEmail("customer.tc003@test.com")
                .shippingAddress("101 Đường Hoàng Quốc Việt, Quận Cầu Giấy, Hà Nội")
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("3500000"))
                .depositAmount(BigDecimal.ZERO)
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .craneFee(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .notes("Đơn hàng test rejection cho TC-L3-BF01-003")
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
    }

    @AfterAll
    void tearDownAll() {
        try {
            // Clean up isolated test data created by this test class
            if (testOrderCode != null) {
                orderRepository.findByOrderCode(testOrderCode).ifPresent(o -> {
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
     * TC-L3-BF01-003: Order Rejection and Product Release.
     *
     * <p><strong>Preconditions:</strong>
     * <ul>
     *   <li>Order Moderator account exists with email "moderator.rejection.e2e@test.com" and status ACTIVE.</li>
     *   <li>A dedicated test product exists with productStatus = "RESERVED".</li>
     *   <li>A dedicated unassigned PENDING order (orderCode: BSMS-TC-BF01-003-...) exists in database with assignedTo = null, linked to the RESERVED product.</li>
     * </ul>
     *
     * <p><strong>Step 1:</strong> Claim a PENDING order whose product is RESERVED and verify order appears in My Orders with RESERVED product.
     * <p><strong>Step 2:</strong> Open rejection action, enter a valid reason, and confirm rejection.
     * <p><strong>Step 3:</strong> Review final order status (CANCELLED), rejection reason, moderator processing history, product release (AVAILABLE), and verify no successful payment/ledger exists.
     *
     * <p><strong>Expected Result:</strong>
     * The order changes to status CANCELLED, the rejection reason is recorded in order notes, product status changes to AVAILABLE, processing history records the moderator action, and no successful Payment or Financial Ledger revenue entry is created.
     */
    @Test
    @DisplayName("TC-L3-BF01-003: Reject Claimed Order and Verify Final Status")
    void tcL3Bf01003_rejectClaimedOrderAndVerifyFinalStatus() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // =========================================================================
            // STEP 1: Claim a PENDING order whose product is RESERVED
            // =========================================================================
            login(page, moderatorEntity.getEmail(), "password123");
            evidencePause(page, "1. Moderator Logged In Successfully");

            page.navigate(getBaseUrl() + "/moderator/orders/pool");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            page.waitForSelector("#ordersTableBody tr:has-text('" + testOrderCode + "')",
                    new Page.WaitForSelectorOptions().setTimeout(10000));

            Locator orderRow = page.locator("#ordersTableBody tr:has-text('" + testOrderCode + "')").first();
            assertTrue(orderRow.isVisible(), "Đơn hàng test TC-003 phải hiển thị trong Kho đơn chung!");

            // DB assertions before claim
            Order dbOrderBefore = orderRepository.findByOrderCode(testOrderCode)
                    .orElseThrow(() -> new AssertionError("Không tìm thấy đơn hàng " + testOrderCode + " trong DB!"));
            assertEquals("PENDING", dbOrderBefore.getOrderStatus(), "Trạng thái đơn trước khi claim phải là PENDING!");
            assertNull(dbOrderBefore.getAssignedTo(), "assignedTo trước khi claim phải là null!");

            Product dbProductBefore = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("RESERVED", dbProductBefore.getProductStatus(), "Trạng thái cây trước claim phải là RESERVED!");

            evidencePause(page, "2. Orders Pool Loaded with Unassigned PENDING Order (RESERVED Product)");

            // Click Claim
            orderRow.locator("button.btn-claim-action").click();
            page.waitForTimeout(2000);

            // DB assertions after claim
            Order dbOrderAfterClaim = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
            assertNotNull(dbOrderAfterClaim.getAssignedTo(), "Đơn hàng phải được gán cho Moderator!");
            assertEquals(moderatorEntity.getUserId(), dbOrderAfterClaim.getAssignedTo().getUserId());
            assertEquals("PENDING", dbOrderAfterClaim.getOrderStatus(), "Trạng thái đơn giữ nguyên PENDING!");

            Product dbProductAfterClaim = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("RESERVED", dbProductAfterClaim.getProductStatus(), "Trạng thái cây giữ nguyên RESERVED trước từ chối!");

            // Verify in My Orders
            page.navigate(getBaseUrl() + "/moderator/orders/my");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForSelector("body", new Page.WaitForSelectorOptions().setTimeout(10000));
            assertTrue(page.content().contains(testOrderCode), "Đơn hàng vừa claim phải hiển thị trong 'Đơn hàng của tôi'!");

            evidencePause(page, "3. Step 1 Complete: Order Claimed & Appears in My Orders (Product RESERVED)");

            // =========================================================================
            // STEP 2: Open rejection action, enter valid reason, and confirm
            // =========================================================================
            page.navigate(getBaseUrl() + "/moderator/orders/" + testOrderCode);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // Click "Từ chối đơn" button
            page.click("button.od-action-btn.reject");

            // Wait for confirmation modal
            page.waitForSelector("#orderActionConfirmModal.show", new Page.WaitForSelectorOptions().setTimeout(5000));

            // Enter valid rejection reason
            String rejectionReason = "Khách hàng thay đổi ý định không mua cây nữa (TC-L3-BF01-003)";
            page.fill("#confirmModalInput", rejectionReason);

            // Confirm rejection in modal
            page.click("#confirmModalConfirm");

            // Wait for processing & page reload
            page.waitForTimeout(3000);

            evidencePause(page, "4. Step 2 Complete: Rejection Confirmed with Valid Reason");

            // =========================================================================
            // STEP 3: Review final order status, reason, moderator history & product release
            // =========================================================================
            // 1. Database assertions:
            Order dbOrderFinal = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
            // Actual implemented rejection status set by OrderService.rejectOrder is "CANCELLED"
            assertEquals("CANCELLED", dbOrderFinal.getOrderStatus(),
                    "Trạng thái DB thực tế của đơn hàng bị từ chối là CANCELLED!");
            assertTrue(dbOrderFinal.getNotes() != null && dbOrderFinal.getNotes().contains(rejectionReason),
                    "Ghi chú đơn hàng phải lưu lại lý do từ chối!");

            // Product status changes from RESERVED to AVAILABLE
            Product dbProductFinal = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("AVAILABLE", dbProductFinal.getProductStatus(),
                    "Trạng thái cây phải được giải phóng từ RESERVED sang AVAILABLE sau khi từ chối đơn!");

            // OrderLog records REJECT action
            List<OrderLog> logs = orderLogRepository.findByOrderOrderIdOrderByActionAtAsc(dbOrderFinal.getOrderId());
            boolean hasRejectLog = logs.stream().anyMatch(l -> "REJECT".equalsIgnoreCase(l.getActionType()));
            assertTrue(hasRejectLog, "Phải có bản ghi OrderLog với actionType REJECT!");

            // OrderHandling history contains moderator processing record
            List<OrderHandling> handlings = orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(dbOrderFinal.getOrderId());
            assertFalse(handlings.isEmpty(), "Phải ghi nhận lịch sử xử lý của Moderator trong OrderHandling!");

            // No SUCCESS Payment record
            List<Payment> payments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(dbOrderFinal.getOrderId());
            boolean hasSuccessPayment = payments.stream().anyMatch(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()));
            assertFalse(hasSuccessPayment, "Đơn hàng bị từ chối không được có bản ghi Payment SUCCESS!");

            // No Financial Ledger entry
            List<FinancialLedger> ledgers = financialLedgerRepository.findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(dbOrderFinal.getOrderId());
            assertTrue(ledgers.isEmpty(), "Đơn hàng bị từ chối không được tạo bản ghi Financial Ledger doanh thu!");

            // 2. UI assertions on Order Detail page
            page.navigate(getBaseUrl() + "/moderator/orders/" + testOrderCode);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            assertTrue(page.content().contains("CANCELLED") || page.content().contains("Hủy đơn") || page.content().contains("chấm dứt"),
                    "Giao diện Chi tiết đơn hàng phải hiển thị trạng thái đã từ chối/hủy!");
            assertTrue(page.content().contains(rejectionReason),
                    "Giao diện phải hiển thị lý do từ chối đã nhập!");

            // 3. UI assertions on My Orders page
            page.navigate(getBaseUrl() + "/moderator/orders/my");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForSelector("body", new Page.WaitForSelectorOptions().setTimeout(10000));
            assertTrue(page.content().contains(testOrderCode),
                    "Đơn hàng phải xuất hiện trong danh sách 'Đơn hàng của tôi' với trạng thái từ chối/hủy!");

            evidencePause(page, "5. Step 3 Complete: Visually Verified Final Status, Reason & History in UI");
            evidencePause(page, "6. All 3-Step DB & UI Assertions Passed Successfully");
        }
    }
}
