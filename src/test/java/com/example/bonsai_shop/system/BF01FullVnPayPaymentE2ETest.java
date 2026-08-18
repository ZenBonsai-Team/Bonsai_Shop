package com.example.bonsai_shop.system;

import com.example.bonsai_shop.config.VNPayConfig;
import com.example.bonsai_shop.customer.repository.RegisterOtpRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.EmailService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.PasswordResetOtp;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3 SYSTEM TEST (E2E Automated Browser Test - TC-L3-BF01-008)
 * Business Flow: BF-01 Full VNPay Payment (100%) and Delivery Completion
 * Actors: Guest Customer, Order Moderator
 *
 * <p><strong>Test Workflow:</strong>
 * <ol>
 *   <li>Guest customer checks out with 100% full VNPay payment method and OTP verification.</li>
 *   <li>Moderator claims the order from Orders Pool and approves shipping/crane fees without deposit amount.</li>
 *   <li>Simulate 100% full amount VNPay payment success callback.</li>
 *   <li>Assert Order transitions to PAID (not auto-completed), Payment is SUCCESS with full total amount, and Product remains RESERVED.</li>
 *   <li>Moderator confirms tree delivery via UI ("Xác nhận khách đã nhận cây").</li>
 *   <li>Assert Order becomes COMPLETED, Product becomes SOLD, and FinancialLedger records revenue exactly once.</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BF01FullVnPayPaymentE2ETest {

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
    private RegisterOtpRepository registerOtpRepository;

    @Autowired
    private OrderService orderService;

    private Playwright playwright;
    private Browser browser;

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

        String modEmail = "moderator.fullpay.e2e@test.com";
        userRepository.findByEmail(modEmail).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode("password123"));
            u.setStatus("ACTIVE");
            u.setRole(moderatorRole);
            userRepository.save(u);
        });
        moderatorEntity = userRepository.findByEmail(modEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Order Moderator (TC-008)")
                        .email(modEmail)
                        .username("moderator_fullpay_e2e")
                        .password(passwordEncoder.encode("password123"))
                        .role(moderatorRole)
                        .status("ACTIVE")
                        .phone("0908889999")
                        .build()));

        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("E2E Category TC008")
                        .description("Category for TC-L3-BF01-008")
                        .build()));

        Variety variety = varietyRepository.findAll().stream().findFirst()
                .orElseGet(() -> varietyRepository.save(Variety.builder()
                        .category(category)
                        .varietyName("Bonsai Tùng Đen TC008")
                        .description("Variety for TC-L3-BF01-008")
                        .build()));

        ProductSegment segment = productSegmentRepository.findAll().stream().findFirst()
                .orElseGet(() -> productSegmentRepository.save(ProductSegment.builder()
                        .segmentName("Standard Segment TC008")
                        .build()));

        testProduct = Product.builder()
                .productCode("TC008-TREE-" + System.currentTimeMillis())
                .productName("Cây Bonsai Tùng Đen Thượng Hạng TC-L3-BF01-008")
                .price(new BigDecimal("5000000"))
                .productStatus("AVAILABLE")
                .isVisible(true)
                .isPublicPrice(true)
                .variety(variety)
                .segment(segment)
                .age(25)
                .height(120.0f)
                .trunkDiameter(20.0f)
                .style("Trực quân tử")
                .description("Cây Tùng Đen thử nghiệm thanh toán 100% VNPay TC-L3-BF01-008")
                .createdBy(moderatorEntity)
                .createdAt(LocalDateTime.now())
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @AfterAll
    void tearDownAll() {
        try {
            if (createdOrderCode != null) {
                orderRepository.findByOrderCode(createdOrderCode).ifPresent(o -> {
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

    @Test
    @DisplayName("TC-L3-BF01-008: Full VNPay Payment and Delivery Completion")
    void tcL3Bf01008_fullVnPayPaymentAndDeliveryCompletion() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            // =========================================================================
            // 1. Guest Browse and Checkout with Full Payment (VNPAY) and OTP
            // =========================================================================
            page.navigate(getBaseUrl() + "/checkout?productId=" + testProduct.getProductId());
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForSelector("#checkoutSummaryItems .order-summary-item",
                    new Page.WaitForSelectorOptions().setTimeout(10000));
            evidencePause(page, "1. Guest Checkout Page Loaded");

            page.fill("#custName", "Hoàng Văn FullPay TC008");
            page.fill("#custPhone", "0988776655");
            page.fill("#custEmail", "guest.fullpay.tc008@test.com");
            page.fill("#custAddress", "200 Hoàng Hoa Thám, Quận Ba Đình, Hà Nội");
            page.fill("#orderNotes", "Giao cây và lắp đặt hoàn thiện sân thượng");

            // Select Full Payment Option (#payVNPay)
            page.click("#payVNPay");
            evidencePause(page, "2. Full VNPay Payment Option Selected");

            // Trigger OTP
            page.click("#btnPlaceOrder");
            page.waitForSelector("#guestOtpModal.show", new Page.WaitForSelectorOptions().setTimeout(10000));

            // Retrieve OTP
            PasswordResetOtp otpRecord = registerOtpRepository
                    .findTopByEmailOrderByCreatedAtDesc("guest.fullpay.tc008@test.com")
                    .orElseThrow(() -> new AssertionError("Không tìm thấy mã OTP trong Database!"));

            page.fill("#guestOtpInput", otpRecord.getOtpCode());
            evidencePause(page, "3. OTP Filled into Modal");

            page.click("#btnConfirmGuestOtp");
            page.waitForURL(url -> url.contains("/order/success"), new Page.WaitForURLOptions().setTimeout(10000));
            evidencePause(page, "4. Order Successfully Created -> Success Page");

            String currentUrl = page.url();
            if (currentUrl.contains("orderCode=")) {
                createdOrderCode = currentUrl.substring(currentUrl.indexOf("orderCode=") + 10);
                if (createdOrderCode.contains("&")) {
                    createdOrderCode = createdOrderCode.substring(0, createdOrderCode.indexOf("&"));
                }
            } else {
                List<Order> orders = orderRepository.findAll();
                createdOrderCode = orders.get(orders.size() - 1).getOrderCode();
            }
            assertNotNull(createdOrderCode, "Mã đơn hàng phải được tạo thành công!");

            // DB assertion after creation
            Order createdOrder = orderRepository.findByOrderCode(createdOrderCode).orElseThrow();
            assertEquals("PENDING", createdOrder.getOrderStatus(), "Đơn mới tạo ở trạng thái PENDING");
            assertEquals(0, BigDecimal.ZERO.compareTo(createdOrder.getDepositAmount()), "Đơn thanh toán 100% có depositAmount = 0");

            Product reservedProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("RESERVED", reservedProduct.getProductStatus(), "Cây chuyển sang RESERVED");

            // =========================================================================
            // 2. Moderator Claims and Approves Order with Shipping & Crane Fees
            // =========================================================================
            login(page, moderatorEntity.getEmail(), "password123");
            page.navigate(getBaseUrl() + "/moderator/orders/pool");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            page.waitForSelector("#ordersTableBody tr:has-text('" + createdOrderCode + "')",
                    new Page.WaitForSelectorOptions().setTimeout(10000));
            Locator orderRow = page.locator("#ordersTableBody tr:has-text('" + createdOrderCode + "')").first();
            orderRow.locator("button.btn-claim-action").click();
            page.waitForTimeout(2000);
            evidencePause(page, "5. Moderator Claimed Order from Pool");

            // Moderator opens Order Detail for Approval
            page.navigate(getBaseUrl() + "/moderator/orders/" + createdOrderCode);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "6. Moderator Viewing Order Detail for Full Payment Approval");

            // Enter fees: Shipping 400k, Crane 600k (Deposit field is disabled for full payment)
            page.fill("#approvalShippingFee", "400000");
            page.fill("#approvalCraneFee", "600000");
            evidencePause(page, "7. Fees Entered: Ship 400k, Crane 600k (Total: 6,000,000 VNĐ)");

            page.click("button.od-action-btn.approve");
            page.waitForSelector("#orderActionConfirmModal.show", new Page.WaitForSelectorOptions().setTimeout(5000));
            page.click("#confirmModalConfirm");
            page.waitForTimeout(3000);
            evidencePause(page, "8. Full Payment Order Approved in PENDING_PAYMENT");

            // DB assertions after approval: Total = 5M + 400k + 600k = 6,000,000 VNĐ
            Order approvedOrder = orderRepository.findByOrderCode(createdOrderCode).orElseThrow();
            assertEquals("PENDING_PAYMENT", approvedOrder.getOrderStatus(), "Trạng thái sau duyệt là PENDING_PAYMENT");
            assertEquals(0, new BigDecimal("6000000").compareTo(approvedOrder.getTotalAmount()), "Tổng tiền là 6,000,000 VNĐ");
            assertEquals(0, BigDecimal.ZERO.compareTo(approvedOrder.getDepositAmount()), "Tiền cọc vẫn là 0");

            // =========================================================================
            // 3. Process Full VNPay Payment Callback (6,000,000 VNĐ)
            // =========================================================================
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Amount", "600000000"); // 6,000,000 * 100
            params.put("vnp_BankCode", "NCB");
            params.put("vnp_CardType", "ATM");
            params.put("vnp_OrderInfo", "Thanh toan 100% don hang BSMS:" + createdOrderCode);
            params.put("vnp_PayDate", "20260814120000");
            params.put("vnp_ResponseCode", "00");
            params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode != null ? VNPayConfig.vnp_TmnCode : "TEST_TMN");
            params.put("vnp_TransactionNo", "14000003");
            params.put("vnp_TransactionStatus", "00");
            params.put("vnp_TxnRef", createdOrderCode);

            StringBuilder sb = new StringBuilder();
            Iterator<String> itr = params.keySet().iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                String val = params.get(key);
                sb.append(key).append('=').append(URLEncoder.encode(val, StandardCharsets.US_ASCII));
                if (itr.hasNext()) sb.append('&');
            }
            String secureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, sb.toString());
            String callbackUrl = getBaseUrl() + "/vnpay/payment-callback?" + sb.toString() + "&vnp_SecureHash=" + secureHash;

            page.navigate(callbackUrl);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "9. Full Payment Callback Executed Successfully");

            // DB assertions after payment: Order is PAID, NOT yet COMPLETED
            Order paidOrder = orderRepository.findByOrderCode(createdOrderCode).orElseThrow();
            assertEquals("PAID", paidOrder.getOrderStatus(), "Trạng thái đơn hàng sau thanh toán đủ 100% phải là PAID!");

            List<Payment> payments = paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(paidOrder.getOrderId());
            assertFalse(payments.isEmpty(), "Bản ghi Payment không được rỗng");
            Payment fullPayment = payments.get(0);
            assertEquals("SUCCESS", fullPayment.getPaymentStatus(), "Payment 100% phải ở trạng thái SUCCESS");
            assertEquals(0, new BigDecimal("6000000").compareTo(fullPayment.getAmount()), "Số tiền thanh toán đúng 6,000,000 VNĐ");

            Product pendingDeliveryProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("RESERVED", pendingDeliveryProduct.getProductStatus(), "Cây vẫn giữ RESERVED trong khi chờ giao");

            List<FinancialLedger> midLedgers = financialLedgerRepository
                    .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(paidOrder.getOrderId());
            assertTrue(midLedgers.isEmpty(), "Chưa ghi nhận doanh thu FinancialLedger trước khi hoàn tất giao cây!");

            // =========================================================================
            // 4. Moderator Confirms Tree Delivery via UI ("Xác nhận khách đã nhận cây")
            // =========================================================================
            page.navigate(getBaseUrl() + "/moderator/orders/" + createdOrderCode);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            evidencePause(page, "10. Moderator Viewing PAID Order Detail for Delivery Confirmation");

            Locator completeBtn = page.locator("button.od-action-btn:has-text('Xác nhận khách đã nhận cây')").first();
            assertTrue(completeBtn.isVisible(), "Nút 'Xác nhận khách đã nhận cây' phải hiển thị cho đơn PAID!");
            completeBtn.click();

            page.waitForSelector("#orderActionConfirmModal.show", new Page.WaitForSelectorOptions().setTimeout(5000));
            evidencePause(page, "11. Delivery Completion Confirmation Modal Displayed");

            page.click("#confirmModalConfirm");
            page.waitForTimeout(3000);
            evidencePause(page, "12. Delivery Confirmed & Order Completed via UI");

            // =========================================================================
            // 5. Final DB & UI Assertions
            // =========================================================================
            Order finalOrder = orderRepository.findByOrderCode(createdOrderCode).orElseThrow();
            assertEquals("COMPLETED", finalOrder.getOrderStatus(), "Đơn hàng phải chuyển sang trạng thái COMPLETED!");
            assertNotNull(finalOrder.getCompletedAt(), "Thời gian CompletedAt phải được ghi nhận!");

            Product soldProduct = productRepository.findById(testProduct.getProductId()).orElseThrow();
            assertEquals("SOLD", soldProduct.getProductStatus(), "Sản phẩm phải chuyển sang trạng thái SOLD!");

            List<FinancialLedger> finalLedgers = financialLedgerRepository
                    .findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(finalOrder.getOrderId());
            assertEquals(1, finalLedgers.size(), "FinancialLedger chỉ được ghi nhận đúng 1 lần doanh thu!");

            FinancialLedger revenueLedger = finalLedgers.get(0);
            assertEquals("INCOME", revenueLedger.getDirection().name(), "Doanh thu phải là dòng tiền INCOME!");
            assertEquals(0, new BigDecimal("6000000").compareTo(revenueLedger.getAmount()), "Số tiền ghi nhận doanh thu là 6,000,000 VNĐ!");

            evidencePause(page, "13. All TC-008 DB and UI Assertions Passed Successfully");
        }
    }
}
