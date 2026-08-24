package com.example.bonsai_shop.system;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3 SYSTEM TEST (E2E Automated Browser & RBAC Security Test - TC-L3-BF01-007)
 * Business Flow: BF-01 Order Processing RBAC and Session Security
 * Actors: Customer, Order Moderator A, Order Moderator B
 *
 * <p>
 * <strong>Security Checks:</strong>
 * <ol>
 * <li>Customer cannot access moderator protected pages (403 Forbidden or login
 * redirect).</li>
 * <li>Moderator B cannot view or process order claimed by Moderator A (UI
 * isolation + SecurityException on backend).</li>
 * <li>Unassigned order cannot be approved before claim (IllegalStateException
 * guard).</li>
 * <li>Logged out session cannot access protected pages via history / back
 * navigation.</li>
 * <li>Database integrity is completely preserved after all unauthorized
 * attempts.</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BF01OrderRbacAndSessionSecurityE2ETest {

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

    private User customerEntity;
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
        Playwright.CreateOptions createOptions = new Playwright.CreateOptions();
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        createOptions.setEnv(env);
        playwright = Playwright.create(createOptions);

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false)
                        .setSlowMo(1500));

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CUSTOMER").build()));
        Role moderatorRole = roleRepository.findByRoleName("MODERATOR")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("MODERATOR").build()));

        // Customer Account
        String custEmail = "customer.rbac.e2e@test.com";
        userRepository.findByEmail(custEmail).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(customerRole);
            userRepository.save(u);
        });
        customerEntity = userRepository.findByEmail(custEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Nguyễn Khách Hàng RBAC (TC-007)")
                        .email(custEmail)
                        .username("customer_rbac_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(customerRole)
                        .status("ACTIVE")
                        .phone("0911002233")
                        .build()));

        // Moderator A Account
        String modAEmail = "moderator.a.rbac.e2e@test.com";
        userRepository.findByEmail(modAEmail).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(moderatorRole);
            userRepository.save(u);
        });
        moderatorAEntity = userRepository.findByEmail(modAEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Order Moderator A (TC-007)")
                        .email(modAEmail)
                        .username("moderator_a_rbac_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(moderatorRole)
                        .status("ACTIVE")
                        .phone("0901112222")
                        .build()));

        // Moderator B Account
        String modBEmail = "moderator.b.rbac.e2e@test.com";
        userRepository.findByEmail(modBEmail).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(moderatorRole);
            userRepository.save(u);
        });
        moderatorBEntity = userRepository.findByEmail(modBEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Order Moderator B (TC-007)")
                        .email(modBEmail)
                        .username("moderator_b_rbac_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(moderatorRole)
                        .status("ACTIVE")
                        .phone("0903334444")
                        .build()));

        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("E2E Category TC007")
                        .description("Category for TC-L3-BF01-007")
                        .build()));

        Variety variety = varietyRepository.findAll().stream().findFirst()
                .orElseGet(() -> varietyRepository.save(Variety.builder()
                        .category(category)
                        .varietyName("Bonsai Tùng Tuyết TC007")
                        .description("Variety for TC-L3-BF01-007")
                        .build()));

        ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                        .segmentName("Standard Segment TC007")
                        .build()));

        testProduct = Product.builder()
                .productCode("TC007-TREE-" + System.currentTimeMillis())
                .productName("Cây Bonsai Tùng Tuyết TC-L3-BF01-007")
                .price(new BigDecimal("6000000"))
                .productStatus("RESERVED")
                .isVisible(true)
                .isPublicPrice(true)
                .variety(variety)
                .segment(segment)
                .age(22)
                .height(110.0f)
                .trunkDiameter(18.0f)
                .style("Tam đa")
                .description("Cây Tùng Tuyết test RBAC TC-L3-BF01-007")
                .createdBy(moderatorAEntity)
                .createdAt(LocalDateTime.now())
                .build();
        testProduct = productRepository.save(testProduct);

        testOrderCode = "BSMS-TC-BF01-007-" + System.currentTimeMillis();
        testOrder = Order.builder()
                .orderCode(testOrderCode)
                .customerName("Khách Hàng Security TC007")
                .customerPhone("0911223344")
                .customerEmail("customer.tc007@test.com")
                .shippingAddress("100 Đường Trần Duy Hưng, Quận Cầu Giấy, Hà Nội")
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("6000000"))
                .depositAmount(BigDecimal.ZERO)
                .orderStatus("PENDING")
                .orderType("ONLINE")
                .craneFee(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .notes("Đơn hàng test RBAC và Session Security TC-L3-BF01-007")
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
                .amount(new BigDecimal("6000000"))
                .build();
        paymentRepository.save(initialPayment);
    }

    @AfterAll
    void tearDownAll() {
        try {
            if (testOrderCode != null) {
                orderRepository.findByOrderCode(testOrderCode).ifPresent(o -> {
                    financialLedgerRepository.deleteAll(financialLedgerRepository
                            .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(o.getOrderId()));
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

    @Test
    @DisplayName("TC-L3-BF01-007: Order Processing RBAC and Session Security")
    void tcL3Bf01007_orderProcessingRbacAndSessionSecurity() {
        // =========================================================================
        // 1. Customer attempts to access Moderator URLs -> Blocked (403 or Access
        // Denied)
        // =========================================================================
        try (BrowserContext custContext = browser.newContext()) {
            Page custPage = custContext.newPage();
            login(custPage, customerEntity.getEmail(), "password123");
            evidencePause(custPage, "1. Customer Logged In");

            custPage.navigate(getBaseUrl() + "/moderator/orders/pool");
            custPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(custPage, "2. Customer Tried Accessing Moderator Orders Pool");

            // Assert Customer is forbidden / redirected
            boolean isBlocked = custPage.url().contains("access-denied")
                    || custPage.url().contains("login")
                    || custPage.url().contains("403")
                    || !custPage.content().contains("ordersTableBody");
            assertTrue(isBlocked, "Khách hàng không có quyền truy cập trang Kho đơn hàng của Moderator!");
        }

        // =========================================================================
        // 2. Unassigned Order Approval Guard: Cannot approve order before claim
        // =========================================================================
        assertThrows(SecurityException.class, () -> {
            orderService.verifyOrder(testOrderCode, new BigDecimal("500000"), new BigDecimal("300000"),
                    new BigDecimal("2000000"), moderatorAEntity);
        }, "Backend phải từ chối phê duyệt đơn hàng khi đơn chưa được tiếp nhận (assignedTo == null)!");

        // =========================================================================
        // 3. Moderator A claims Order -> Moderator B is blocked from viewing/processing
        // it
        // =========================================================================
        orderService.claimOrder(testOrderCode, moderatorAEntity);

        // Moderator B checks UI
        try (BrowserContext modBContext = browser.newContext()) {
            Page pageB = modBContext.newPage();
            login(pageB, moderatorBEntity.getEmail(), "password123");

            // Moderator B cannot see claimed order in Orders Pool
            pageB.navigate(getBaseUrl() + "/moderator/orders/pool");
            pageB.waitForLoadState(LoadState.DOMCONTENTLOADED);
            assertFalse(pageB.content().contains(testOrderCode),
                    "Moderator B không được thấy đơn của Moderator A trong Kho đơn chung!");

            // Moderator B cannot see claimed order in My Orders
            pageB.navigate(getBaseUrl() + "/moderator/orders/my");
            pageB.waitForLoadState(LoadState.DOMCONTENTLOADED);
            assertFalse(pageB.content().contains(testOrderCode),
                    "Moderator B không được thấy đơn của Moderator A trong Đơn hàng của tôi!");

            evidencePause(pageB, "3. Moderator B UI Isolation Verified");
        }

        // Moderator B direct backend claim & approve attempts
        assertThrows(IllegalStateException.class, () -> {
            orderService.claimOrder(testOrderCode, moderatorBEntity);
        }, "Backend phải từ chối khi Moderator B cố claim lại đơn đã thuộc Moderator A!");

        assertThrows(SecurityException.class, () -> {
            orderService.verifyOrder(testOrderCode, new BigDecimal("500000"), new BigDecimal("300000"),
                    new BigDecimal("2000000"), moderatorBEntity);
        }, "Backend phải từ chối duyệt (SecurityException) khi Moderator B duyệt đơn của Moderator A!");

        // =========================================================================
        // 4. Session Invalidation & History Navigation Guard
        // =========================================================================
        try (BrowserContext modAContext = browser.newContext()) {
            Page pageA = modAContext.newPage();
            login(pageA, moderatorAEntity.getEmail(), "password123");

            // Moderator A opens My Orders
            pageA.navigate(getBaseUrl() + "/moderator/orders/my");
            pageA.waitForLoadState(LoadState.DOMCONTENTLOADED);
            assertTrue(pageA.content().contains(testOrderCode), "Moderator A thấy đơn trong My Orders");
            evidencePause(pageA, "4. Moderator A in My Orders");

            // Logout
            pageA.navigate(getBaseUrl() + "/logout");
            pageA.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(pageA, "5. Moderator A Logged Out");

            // Attempt back navigation or navigating back to protected page
            pageA.navigate(getBaseUrl() + "/moderator/orders/my");
            pageA.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(pageA, "6. Post-Logout Protected Page Access Attempt");

            assertTrue(pageA.url().contains("/login") || !pageA.content().contains("ordersTableBody"),
                    "Sau khi đăng xuất, truy cập lại trang nội bộ phải bị chuyển hướng về màn hình đăng nhập!");
        }

        // =========================================================================
        // 5. Database State Integrity Verification
        // =========================================================================
        Order dbOrder = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
        assertEquals("PENDING", dbOrder.getOrderStatus(),
                "Trạng thái đơn không bị thay đổi bởi các hành động trái phép!");
        assertEquals(moderatorAEntity.getUserId(), dbOrder.getAssignedTo().getUserId(),
                "Đơn vẫn chỉ thuộc Moderator A!");

        Product dbProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
        assertEquals("RESERVED", dbProduct.getProductStatus(), "Trạng thái cây không bị ảnh hưởng!");

        List<FinancialLedger> ledgers = financialLedgerRepository
                .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(dbOrder.getOrderId());
        assertTrue(ledgers.isEmpty(), "Không được có bản ghi FinancialLedger phát sinh!");
    }
}
