package com.example.bonsai_shop.system;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.customer.repository.RegisterOtpRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.PasswordResetOtp;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import com.example.bonsai_shop.integration.support.TestDatabaseSafetyInitializer;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3 SYSTEM TEST (E2E Business Flow Testing - §3b) cho BF-01: Marketplace Order
 * – Deposit Payment.
 * TC-L3-BF01-001: Quy trình đặt mua cây Bonsai cọc trước (Deposit Flow):
 * 1. Khách xem sản phẩm & thực hiện checkout vắng lai qua OTP Email.
 * 2. Order Moderator tiếp nhận đơn từ Kho đơn chung (Orders Pool).
 * 3. Order Moderator kiểm duyệt đơn, cập nhật Phí cẩu (Crane Fee), Phí ship
 * (Shipping Fee) & Tiền cọc (Deposit Amount).
 * 4. Khách hàng thanh toán tiền cọc qua cổng VNPay (giả lập callback kết quả
 * thành công).
 * 5. Order Moderator xác nhận đã thu đủ số tiền còn lại khi giao cây (Remaining
 * Payment) & hoàn thành đơn hàng.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BF01MarketplaceOrderDepositPaymentE2ETest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderHandlingRepository orderHandlingRepository;

    @Autowired
    private OrderLogRepository orderLogRepository;

    @Autowired
    private FinancialLedgerRepository financialLedgerRepository;

    @Autowired
    private RegisterOtpRepository registerOtpRepository;

    private Playwright playwright;
    private Browser browser;
    private User customerEntity;
    private User moderatorEntity;
    private Product testProduct;
    private String createdOrderCode;

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
        // Dọn dẹp sạch sẽ database của phần Order / Payment / Financial Ledger trước
        // khi chạy test
        financialLedgerRepository.deleteAll();
        orderLogRepository.deleteAll();
        orderHandlingRepository.deleteAll();
        paymentRepository.deleteAll();
        orderDetailRepository.deleteAll();
        orderRepository.deleteAll();
        registerOtpRepository.deleteAll();

        // Cấu hình Playwright: Chạy chế độ HEADED (setHeadless(false)) & SLOW_MO
        // (setSlowMo(1500))
        Playwright.CreateOptions createOptions = new Playwright.CreateOptions();
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        createOptions.setEnv(env);
        playwright = Playwright.create(createOptions);

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false)
                        .setSlowMo(1500));

        // Khởi tạo Role CUSTOMER và MODERATOR
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CUSTOMER").build()));

        Role moderatorRole = roleRepository.findByRoleName("MODERATOR")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("MODERATOR").build()));

        // Thiết lập / Đồng bộ tài khoản khách hàng test
        userRepository.findByEmail("customer.bf01.e2e@test.com").ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(customerRole);
            userRepository.save(u);
        });
        customerEntity = userRepository.findByEmail("customer.bf01.e2e@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("E2E Customer BF01")
                        .email("customer.bf01.e2e@test.com")
                        .username("customer_bf01_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(customerRole)
                        .status("ACTIVE")
                        .phone("0912345678")
                        .build()));

        // Thiết lập / Đồng bộ tài khoản Moderator test
        userRepository.findByEmail("moderator.bf01.e2e@test.com").ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(moderatorRole);
            userRepository.save(u);
        });
        moderatorEntity = userRepository.findByEmail("moderator.bf01.e2e@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("E2E Order Moderator BF01")
                        .email("moderator.bf01.e2e@test.com")
                        .username("moderator_bf01_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(moderatorRole)
                        .status("ACTIVE")
                        .phone("0988776655")
                        .build()));

        // Khởi tạo Danh mục, Chủng loại, Phân khúc sản phẩm cho cây test
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("E2E Bonsai Category")
                        .description("Category for E2E testing")
                        .build()));

        Variety variety = varietyRepository.findAll().stream().findFirst()
                .orElseGet(() -> varietyRepository.save(Variety.builder()
                        .category(category)
                        .varietyName("Tùng La Hán E2E")
                        .description("Variety for E2E testing")
                        .build()));

        ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                        .segmentName("Luxury Segment")
                        .build()));

        // Tạo cây test sẵn sàng bán (AVAILABLE)
        String productCode = "BF01-TREE-" + System.currentTimeMillis();
        testProduct = Product.builder()
                .productCode(productCode)
                .productName("Bonsai Tùng La Hán Thượng Uyển E2E")
                .price(new BigDecimal("10000000")) // Giá cây: 10,000,000 VNĐ
                .productStatus("AVAILABLE")
                .isVisible(true)
                .isPublicPrice(true)
                .variety(variety)
                .segment(segment)
                .age(25)
                .height(150.0f)
                .trunkDiameter(20.0f)
                .style("Thác đổ")
                .description("Tác phẩm Bonsai Tùng La Hán tạo dáng cổ thụ phục vụ kiểm thử E2E.")
                .createdBy(moderatorEntity)
                .createdAt(LocalDateTime.now())
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @AfterAll
    void tearDownAll() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("TC-E2E-BF01-001 Step 1: Guest Browse & Checkout with OTP Verification")
    void step1_guestBrowseAndCheckoutWithOtp() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // 1. Khách vãng lai xem chi tiết sản phẩm trên Marketplace
            page.navigate(getBaseUrl() + "/product/" + testProduct.getProductId());
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "1.1 Guest Viewing Product Detail Page");

            assertTrue(page.content().contains("Bonsai Tùng La Hán Thượng Uyển E2E"));

            // 2. Chuyển sang trang Checkout với tham số productId
            page.navigate(getBaseUrl() + "/checkout?productId=" + testProduct.getProductId());
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForSelector("#checkoutSummaryItems .order-summary-item",
                    new Page.WaitForSelectorOptions().setTimeout(10000));
            evidencePause(page, "1.2 Guest Loaded Checkout Form Page");

            // 3. Điền thông tin giao hàng
            page.fill("#custName", "Nguyễn Văn Khách E2E");
            page.fill("#custPhone", "0912345678");
            page.fill("#custEmail", "guest.bf01@test.com");
            page.fill("#custAddress", "123 Phố Bonsai, Phường Nghĩa Đô, Quận Cầu Giấy, Hà Nội");
            page.fill("#orderNotes", "Cẩu cây cẩn thận vào sân vườn biệt thự");

            // Chọn phương thức thanh toán "Cọc trước" (DEPOSIT)
            page.click("#payDeposit");

            evidencePause(page, "1.3 Guest Filled Checkout Form");

            // 4. Nhấn nút Xác nhận đặt hàng (Trigger gửi OTP)
            page.click("#btnPlaceOrder");

            // Chờ modal nhập OTP xuất hiện
            page.waitForSelector("#guestOtpModal.show", new Page.WaitForSelectorOptions().setTimeout(10000));
            evidencePause(page, "1.4 Guest OTP Verification Modal Prompted");

            // 5. Lấy mã OTP thực tế được sinh từ Repository
            PasswordResetOtp otpRecord = registerOtpRepository.findTopByEmailOrderByCreatedAtDesc("guest.bf01@test.com")
                    .orElseThrow(() -> new AssertionError("Không tìm thấy mã OTP trong Database!"));

            assertNotNull(otpRecord.getOtpCode(), "Mã OTP phải khác null!");
            assertEquals(6, otpRecord.getOtpCode().length(), "Mã OTP phải đủ 6 chữ số!");

            // 6. Nhập mã OTP vào input và xác nhận
            page.fill("#guestOtpInput", otpRecord.getOtpCode());
            evidencePause(page, "1.5 Filled Valid OTP Code into Modal");

            page.click("#btnConfirmGuestOtp");

            // Chờ chuyển hướng đến trang tạo đơn thành công (/order/success)
            page.waitForURL(url -> url.contains("/order/success"), new Page.WaitForURLOptions().setTimeout(10000));
            evidencePause(page, "1.6 Order Successfully Placed - Redirected to Success Page");

            assertTrue(page.url().contains("/order/success"));

            // 7. Lấy mã orderCode từ URL tham số (/order/success?orderCode=BSMS-XXXXXX)
            String currentUrl = page.url();
            if (currentUrl.contains("orderCode=")) {
                createdOrderCode = currentUrl.substring(currentUrl.indexOf("orderCode=") + 10);
                if (createdOrderCode.contains("&")) {
                    createdOrderCode = createdOrderCode.substring(0, createdOrderCode.indexOf("&"));
                }
            } else {
                List<Order> orders = orderRepository.findAll();
                assertFalse(orders.isEmpty(), "Danh sách đơn hàng không được rỗng!");
                createdOrderCode = orders.get(orders.size() - 1).getOrderCode();
            }

            assertNotNull(createdOrderCode, "Mã đơn hàng phải được sinh!");

            // 8. Kiểm tra trạng thái đơn hàng trong Database
            Order createdOrder = orderRepository.findByOrderCode(createdOrderCode)
                    .orElseThrow(
                            () -> new AssertionError("Không tìm thấy đơn hàng " + createdOrderCode + " trong DB!"));

            assertEquals("PENDING", createdOrder.getOrderStatus(), "Trạng thái đơn mới tạo phải là PENDING!");
            assertNull(createdOrder.getAssignedTo(), "Đơn mới tạo chưa có Moderator phụ trách (assignedTo = null)!");

            // Kiểm tra trạng thái cây đã chuyển sang RESERVED
            Product updatedProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("RESERVED", updatedProduct.getProductStatus(),
                    "Sản phẩm phải chuyển sang trạng thái RESERVED!");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("TC-E2E-BF01-001 Step 2: Order Moderator claims order from Orders Pool")
    void step2_moderatorClaimOrderFromPool() {
        assertNotNull(createdOrderCode, "Mã đơn hàng từ Bước 1 không được rỗng!");

        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // 1. Moderator đăng nhập hệ thống
            login(page, moderatorEntity.getEmail(), "password123");
            evidencePause(page, "2.1 Moderator Logged In Successfully");

            // 2. Moderator truy cập Kho đơn hàng chung (Orders Pool)
            page.navigate(getBaseUrl() + "/moderator/orders/pool");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "2.2 Moderator Browsing Orders Pool");

            assertTrue(page.url().contains("/moderator/orders/pool"));

            // 3. Tìm hàng chứa mã đơn vừa tạo và nhấn "Tiếp nhận đơn hàng"
            page.waitForSelector("#ordersTableBody tr:has-text('" + createdOrderCode + "')");
            Locator orderRow = page.locator("#ordersTableBody tr:has-text('" + createdOrderCode + "')").first();
            assertTrue(orderRow.isVisible(), "Hàng đơn hàng phải hiển thị trong Kho chung!");

            orderRow.locator("button.btn-claim-action").click();

            // Wait 2 seconds for AJAX request completion and table re-render
            page.waitForTimeout(2000);
            evidencePause(page, "2.3 Moderator Clicked Claim Action Button");

            // 4. Kiểm tra Database: Đơn hàng đã được gán cho Moderator
            Order updatedOrder = orderRepository.findByOrderCode(createdOrderCode).orElseThrow();
            assertNotNull(updatedOrder.getAssignedTo(), "Đơn hàng phải được gán cho Moderator!");
            assertEquals(moderatorEntity.getUserId(), updatedOrder.getAssignedTo().getUserId(),
                    "UserId Moderator phụ trách phải chính xác!");

            // 5. Kiểm tra giao diện: Đơn hàng xuất hiện trong "Đơn hàng của tôi"
            page.navigate(getBaseUrl() + "/moderator/orders/my");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "2.4 Order Appears in 'My Orders' Page");

            assertTrue(page.content().contains(createdOrderCode));
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("TC-E2E-BF01-001 Step 3: Moderator approves order with Crane Fee, Shipping Fee, and Deposit Amount")
    void step3_moderatorApproveOrderWithFeesAndDeposit() {
        assertNotNull(createdOrderCode, "Mã đơn hàng từ Bước 1 không được rỗng!");

        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // 1. Moderator đăng nhập và truy cập trang Chi tiết đơn hàng
            login(page, moderatorEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/moderator/orders/" + createdOrderCode);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "3.1 Moderator Viewing Order Detail Page");

            // 2. Nhập các phụ phí & tiền đặt cọc:
            // - Tiền đặt cọc: 2,000,000 VNĐ (#approvalDepositAmount)
            // - Phí vận chuyển: 500,000 VNĐ (#approvalShippingFee)
            // - Phí xe cẩu: 1,000,000 VNĐ (#approvalCraneFee)
            page.fill("#approvalDepositAmount", "2000000");
            page.fill("#approvalShippingFee", "500000");
            page.fill("#approvalCraneFee", "1000000");

            evidencePause(page, "3.2 Moderator Entered Crane Fee (1M), Shipping Fee (500k), Deposit Amount (2M)");

            // 3. Click nút "Duyệt đơn"
            page.click("button.od-action-btn.approve");

            // Modal xác nhận xuất hiện (#orderActionConfirmModal)
            page.waitForSelector("#orderActionConfirmModal.show", new Page.WaitForSelectorOptions().setTimeout(5000));
            evidencePause(page, "3.3 Approval Confirmation Modal Displayed");

            // Click "Xác nhận" trong modal
            page.click("#confirmModalConfirm");

            // Wait for reload
            page.waitForTimeout(3000);
            evidencePause(page, "3.4 Order Approved & Page Reloaded");

            // 4. Kiểm tra Database sau khi duyệt:
            Order approvedOrder = orderRepository.findByOrderCodeWithDetails(createdOrderCode).orElseThrow();
            assertEquals("PENDING_PAYMENT", approvedOrder.getOrderStatus(),
                    "Trạng thái đơn sau duyệt phải là PENDING_PAYMENT!");
            assertEquals(0, new BigDecimal("1000000").compareTo(approvedOrder.getCraneFee()),
                    "Phí xe cẩu phải bằng 1,000,000 VNĐ!");
            assertEquals(0, new BigDecimal("500000").compareTo(approvedOrder.getShippingFee()),
                    "Phí vận chuyển phải bằng 500,000 VNĐ!");
            assertEquals(0, new BigDecimal("2000000").compareTo(approvedOrder.getDepositAmount()),
                    "Tiền đặt cọc phải bằng 2,000,000 VNĐ!");

            // Tổng giá trị đơn hàng mới = 10M (cây) + 1M (cẩu) + 500k (ship) = 11,500,000
            // VNĐ
            assertEquals(0, new BigDecimal("11500000").compareTo(approvedOrder.getTotalAmount()),
                    "Tổng tiền đơn hàng phải bằng 11,500,000 VNĐ!");

            // Kiểm tra bản ghi Payment PENDING duy nhất được tạo
            List<Payment> payments = paymentRepository
                    .findByOrderOrderIdOrderByPaymentIdAsc(approvedOrder.getOrderId());
            assertFalse(payments.isEmpty(), "Phải tạo bản ghi Payment!");

            Payment depositPayment = payments.get(0);
            assertEquals("DEPOSIT", depositPayment.getPaymentType(), "PaymentType phải là DEPOSIT!");
            assertEquals("PENDING", depositPayment.getPaymentStatus(), "PaymentStatus ban đầu phải là PENDING!");
            assertEquals(0, new BigDecimal("2000000").compareTo(depositPayment.getAmount()),
                    "Số tiền thanh toán nấc 1 (cọc) phải là 2,000,000 VNĐ!");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("TC-E2E-BF01-001 Step 4: VNPay Deposit Payment Callback Processing (Success)")
    void step4_vnpayDepositPaymentSuccess() throws Exception {
        assertNotNull(createdOrderCode, "Mã đơn hàng không được rỗng!");

        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // 1. Tạo chuỗi tham số callback VNPay hợp lệ kèm chữ ký checksum HmacSHA512
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Amount", "200000000"); // 2,000,000 VNĐ * 100
            params.put("vnp_BankCode", "NCB");
            params.put("vnp_CardType", "ATM");
            params.put("vnp_OrderInfo", "Thanh toan don hang BSMS:" + createdOrderCode);
            params.put("vnp_PayDate", "20260813160000");
            params.put("vnp_ResponseCode", "00");
            params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode != null ? VNPayConfig.vnp_TmnCode : "TEST_TMN");
            params.put("vnp_TransactionNo", "14000000");
            params.put("vnp_TransactionStatus", "00");
            params.put("vnp_TxnRef", createdOrderCode);

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

            String callbackUrl = getBaseUrl() + "/vnpay/payment-callback?" + sb.toString() + "&vnp_SecureHash="
                    + secureHash;

            // 2. Giả lập trình duyệt khách chuyển hướng về trang Callback của hệ thống
            page.navigate(callbackUrl);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "4.1 VNPay Payment Callback Result Page Displayed");

            assertTrue(page.content().contains("Thanh toán") || page.content().contains("thành công")
                    || page.content().contains("SUCCESS"));

            // 3. Kiểm tra Database sau khi thanh toán cọc thành công:
            Order depositedOrder = orderRepository.findByOrderCode(createdOrderCode).orElseThrow();
            assertEquals("DEPOSITED", depositedOrder.getOrderStatus(),
                    "Trạng thái đơn hàng phải chuyển sang DEPOSITED!");

            Payment payment = paymentRepository
                    .findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(depositedOrder.getOrderId(), "SUCCESS")
                    .orElseThrow(() -> new AssertionError("Phải có bản ghi Payment trạng thái SUCCESS!"));
            assertEquals("DEPOSIT", payment.getPaymentType(),
                    "PaymentType của khoản tiền đã thanh toán phải là DEPOSIT!");
            assertNotNull(payment.getPaymentDate(), "PaymentDate phải được cập nhật thời gian!");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("TC-E2E-BF01-001 Step 5: Moderator confirms remaining payment & completes order")
    void step5_moderatorConfirmRemainingPaymentAndComplete() {
        assertNotNull(createdOrderCode, "Mã đơn hàng không được rỗng!");

        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // 1. Moderator đăng nhập và mở lại trang Chi tiết đơn hàng
            login(page, moderatorEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/moderator/orders/" + createdOrderCode);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "5.1 Moderator Viewing Deposited Order Detail Page");

            // 2. Click nút "Xác nhận thu phần còn lại" (button.od-action-btn.approve khi ở
            // trạng thái DEPOSITED)
            page.click("button.od-action-btn.approve");

            // Modal xác nhận xuất hiện (#orderActionConfirmModal)
            page.waitForSelector("#orderActionConfirmModal.show", new Page.WaitForSelectorOptions().setTimeout(5000));
            evidencePause(page, "5.2 Confirmation Modal for Remaining Payment Prompted");

            // Click "Xác nhận"
            page.click("#confirmModalConfirm");

            // Wait for reload
            page.waitForTimeout(3000);
            evidencePause(page, "5.3 Order Completed & Remaining Payment Confirmed");

            // 3. Kiểm tra Database sau khi hoàn thành đơn:
            Order completedOrder = orderRepository.findByOrderCodeWithDetails(createdOrderCode).orElseThrow();
            assertEquals("COMPLETED", completedOrder.getOrderStatus(),
                    "Trạng thái đơn hàng cuối cùng phải là COMPLETED!");
            assertNotNull(completedOrder.getCompletedAt(), "CompletedAt phải được ghi nhận thời điểm hoàn thành!");

            // Kiểm tra bản ghi Payment #2: REMAINING_PAYMENT
            List<Payment> payments = paymentRepository
                    .findByOrderOrderIdOrderByPaymentIdAsc(completedOrder.getOrderId());
            assertEquals(2, payments.size(), "Phải có đúng 2 bản ghi Payment (1 DEPOSIT + 1 REMAINING_PAYMENT)!");

            Payment depositPayment = payments.get(0);
            assertEquals("DEPOSIT", depositPayment.getPaymentType());
            assertEquals("SUCCESS", depositPayment.getPaymentStatus());

            Payment remainingPayment = payments.get(1);
            assertEquals("REMAINING_PAYMENT", remainingPayment.getPaymentType(),
                    "Payment #2 phải là REMAINING_PAYMENT!");
            assertEquals("CASH", remainingPayment.getPaymentMethod(), "Phương thức phần còn lại phải là CASH!");
            assertEquals("SUCCESS", remainingPayment.getPaymentStatus(),
                    "Trạng thái khoản tiền còn lại phải là SUCCESS!");

            // Phần còn lại = Total Amount (11.5M) - Deposit Paid (2M) = 9,500,000 VNĐ
            assertEquals(0, new BigDecimal("9500000").compareTo(remainingPayment.getAmount()),
                    "Số tiền còn lại thu bằng tiền mặt phải là 9,500,000 VNĐ!");

            // Kiểm tra trạng thái cây chuyển sang SOLD
            Product soldProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("SOLD", soldProduct.getProductStatus(),
                    "Sản phẩm Bonsai sau khi hoàn thành đơn phải có trạng thái SOLD!");

            // Kiểm tra sổ sách tài chính (Financial Ledger) đã ghi nhận doanh thu đơn hoàn
            // thành
            List<FinancialLedger> ledgers = financialLedgerRepository
                    .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(completedOrder.getOrderId());
            assertFalse(ledgers.isEmpty(), "Sổ sách tài chính Financial Ledger phải ghi nhận doanh thu!");
            boolean hasCompletedRevenue = ledgers.stream()
                    .anyMatch(l -> "COMPLETED_ORDER_REVENUE".equalsIgnoreCase(l.getLedgerType().name()));
            assertTrue(hasCompletedRevenue, "Phải có bản ghi COMPLETED_ORDER_REVENUE trong Financial Ledger!");
        }
    }
}