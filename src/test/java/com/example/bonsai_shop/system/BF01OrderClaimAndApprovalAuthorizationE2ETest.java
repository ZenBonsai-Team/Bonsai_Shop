package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.OrderDetailRepository;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3 SYSTEM TEST (E2E Automated Browser Test - TC-L3-BF01-002)
 * Business Flow: BF-01 Order Claim and Approval Authorization
 * Actor: Order Moderator A
 * Target: Open Orders Pool and claim an unassigned PENDING order.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BF01OrderClaimAndApprovalAuthorizationE2ETest {

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
    private OrderService orderService;

    private Playwright playwright;
    private Browser browser;

    private User moderatorAEntity;
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

        // Create or sync Moderator A account for TC-L3-BF01-002
        String modEmail = "moderator.a.claim.e2e@test.com";
        userRepository.findByEmail(modEmail).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(moderatorRole);
            userRepository.save(u);
        });
        moderatorAEntity = userRepository.findByEmail(modEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Order Moderator A (TC-002)")
                        .email(modEmail)
                        .username("moderator_a_claim_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(moderatorRole)
                        .status("ACTIVE")
                        .phone("0901234567")
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
                .price(new BigDecimal("4500000"))
                .productStatus("AVAILABLE")
                .isVisible(true)
                .isPublicPrice(true)
                .variety(variety)
                .segment(segment)
                .age(15)
                .height(80.0f)
                .trunkDiameter(12.0f)
                .style("Trực quân tử")
                .description("Cây Bonsai Sanh thử nghiệm claim đơn TC-L3-BF01-002")
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
                .notes("Đơn hàng test tiếp nhận (Claim) cho TC-L3-BF01-002")
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
            // Clean up isolated test data created by this test
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
     * TC-L3-BF01-002: Order Claim and Approval Authorization.
     *
     * <p><strong>Preconditions:</strong>
     * <ul>
     *   <li>Moderator A account exists with email "moderator.a.claim.e2e@test.com" and status ACTIVE.</li>
     *   <li>A dedicated unassigned PENDING order (orderCode: BSMS-TC-BF01-002-...) exists in database with assignedTo = null.</li>
     * </ul>
     *
     * <p><strong>Steps:</strong>
     * <ol>
     *   <li>Start Playwright browser and log in as Moderator A.</li>
     *   <li>Navigate to Orders Pool page (/moderator/orders/pool).</li>
     *   <li>Assert order is displayed in Orders Pool and database state is PENDING with assignedTo = null.</li>
     *   <li>Click "Tiếp nhận đơn hàng" (Claim) button for the order.</li>
     *   <li>Wait for AJAX claim request and UI refresh to finish.</li>
     *   <li>Assert database order is assigned to Moderator A (assignedTo = Moderator A's ID) and state remains PENDING.</li>
     *   <li>Assert order is no longer displayed in Orders Pool list.</li>
     *   <li>Navigate to My Orders page (/moderator/orders/my) and verify order code is visible there.</li>
     *   <li>Verify exclusivity by asserting that claiming an already-assigned order again throws IllegalStateException.</li>
     * </ol>
     *
     * <p><strong>Expected Result:</strong>
     * The order is assigned exclusively to Moderator A, updated in database with assignedTo = Moderator A, disappears from Orders Pool, and appears in My Orders.
     */
    @Test
    @DisplayName("TC-L3-BF01-002: Order Claim and Approval Authorization")
    void tcL3Bf01002_orderClaimAndApprovalAuthorization() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // 1. Log in as Moderator A
            login(page, moderatorAEntity.getEmail(), "password123");
            evidencePause(page, "1. Moderator A Logged In Successfully");

            // 2. Open Orders Pool page
            page.navigate(getBaseUrl() + "/moderator/orders/pool");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // Wait for orders table to load and contain test order
            page.waitForSelector("#ordersTableBody tr:has-text('" + testOrderCode + "')",
                    new Page.WaitForSelectorOptions().setTimeout(10000));

            Locator orderRow = page.locator("#ordersTableBody tr:has-text('" + testOrderCode + "')").first();
            assertTrue(orderRow.isVisible(), "Đơn hàng test TC-002 phải hiển thị trong Kho đơn chung (Orders Pool)!");

            // Assert database state BEFORE claim
            Order dbOrderBefore = orderRepository.findByOrderCode(testOrderCode)
                    .orElseThrow(() -> new AssertionError("Không tìm thấy đơn hàng " + testOrderCode + " trong DB!"));
            assertEquals("PENDING", dbOrderBefore.getOrderStatus(), "Trạng thái đơn trước khi claim phải là PENDING!");
            assertNull(dbOrderBefore.getAssignedTo(), "Đơn mới tạo chưa có Moderator phụ trách (assignedTo = null)!");

            evidencePause(page, "2. Orders Pool Loaded with Unassigned PENDING Order");

            // 3. Click Claim / "Tiếp nhận đơn hàng" action button
            orderRow.locator("button.btn-claim-action").click();

            // Wait for AJAX claim completion & table re-render
            page.waitForTimeout(2000);

            // Assert database state AFTER claim
            Order dbOrderAfter = orderRepository.findByOrderCode(testOrderCode)
                    .orElseThrow(() -> new AssertionError("Không tìm thấy đơn hàng " + testOrderCode + " sau khi claim!"));

            assertNotNull(dbOrderAfter.getAssignedTo(), "Đơn hàng phải được gán cho Moderator sau khi claim!");
            assertEquals(moderatorAEntity.getUserId(), dbOrderAfter.getAssignedTo().getUserId(),
                    "UserId của Moderator phụ trách phải khớp với Moderator A!");
            assertEquals("PENDING", dbOrderAfter.getOrderStatus(),
                    "Trạng thái đơn hàng sau claim giữ nguyên PENDING!");

            evidencePause(page, "3. Claim Action Completed & Orders Pool Refreshed");

            // 4. Assert UI: order is no longer in Orders Pool list
            page.navigate(getBaseUrl() + "/moderator/orders/pool");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1000);
            assertFalse(page.content().contains(testOrderCode),
                    "Đơn hàng sau khi đã được claim không được còn hiển thị trong Kho đơn chung!");

            // 5. Navigate to My Orders page and assert order code is visible there
            page.navigate(getBaseUrl() + "/moderator/orders/my");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForSelector("body", new Page.WaitForSelectorOptions().setTimeout(10000));
            assertTrue(page.content().contains(testOrderCode),
                    "Đơn hàng vừa claim phải xuất hiện trong danh sách 'Đơn hàng của tôi' (My Orders)!");

            evidencePause(page, "4. My Orders Page Loaded with Claimed Order");

            // 6. Verify Exclusivity: Attempting to claim the already claimed order again throws IllegalStateException
            assertThrows(IllegalStateException.class, () -> {
                orderService.claimOrder(testOrderCode, moderatorAEntity);
            }, "Không thể claim đơn hàng đã được gán cho nhân viên khác/đã được nhận trước đó!");

            evidencePause(page, "5. All DB & UI Assertions Passed Successfully");
        }
    }
}
