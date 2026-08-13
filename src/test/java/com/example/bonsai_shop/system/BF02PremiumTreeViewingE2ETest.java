package com.example.bonsai_shop.system;

import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.artisan.scheduler.AppointmentAutoStatusScheduler;
import com.example.bonsai_shop.artisan.service.ArtisanAppointmentService;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductMediaRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BF02PremiumTreeViewingE2ETest {

    private static final String PASSWORD = "123456";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository segmentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMediaRepository mediaRepository;

    @Autowired
    private ViewingAppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentSettingRepository appointmentSettingRepository;

    @Autowired
    private ArtisanAppointmentService artisanAppointmentService;

    @Autowired
    private AppointmentAutoStatusScheduler appointmentAutoStatusScheduler;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private String baseUrl;
    private String customerEmail;
    private User customer;
    private Product premiumProduct;

    @BeforeAll
    void setUpAll() {
        baseUrl = "http://localhost:" + port;
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Boolean.parseBoolean(System.getProperty("playwright.headless", "false")))
                .setSlowMo(Double.parseDouble(System.getProperty("playwright.slowMo", "1000"))));
    }

    @BeforeEach
    void setUp() {
        clearPauseSetting();
        customerEmail = "bf02.e2e.customer." + System.nanoTime() + "@test.com";
        customer = createUser(customerEmail, "BF02 E2E Customer", findRole("CUSTOMER", "ROLE_CUSTOMER"));
        premiumProduct = createPremiumProduct();

        context = browser.newContext(new Browser.NewContextOptions()
                .setLocale("vi-VN")
                .setTimezoneId("Asia/Bangkok"));
        page = context.newPage();
        blockExternalAssets();
    }

    @AfterEach
    void tearDown() {
        clearPauseSetting();
        if (context != null) {
            context.close();
        }
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

    private Product createPremiumProduct() {
        User artisan = createUser(
                "bf02.e2e.artisan@test.com",
                "BF02 E2E Artisan",
                findRole("ARTISAN", "ROLE_ARTISAN"));

        Category category = categoryRepository.save(Category.builder()
                .categoryName("BF02 E2E Category " + System.nanoTime())
                .description("BF02 E2E category")
                .build());
        Variety variety = varietyRepository.save(Variety.builder()
                .category(category)
                .varietyName("BF02 E2E Variety " + System.nanoTime())
                .description("BF02 E2E variety")
                .build());
        ProductSegment eliteSegment = segmentRepository.findById(3)
                .orElseThrow(() -> new IllegalStateException("Premium segment with id 3 is required"));

        Product product = productRepository.save(Product.builder()
                .createdBy(artisan)
                .variety(variety)
                .segment(eliteSegment)
                .productCode("BF02-E2E-" + System.nanoTime())
                .productName("BF02 E2E Premium Bonsai")
                .description("BF02 E2E premium tree")
                .treeStory("BF02 E2E tree story")
                .age(25)
                .height(80.0F)
                .trunkDiameter(12.0F)
                .style("Formal")
                .price(new BigDecimal("25000000"))
                .isPublicPrice(false)
                .productStatus("AVAILABLE")
                .isVisible(true)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build());

        mediaRepository.save(ProductMedia.builder()
                .product(product)
                .mediaUrl("https://example.com/bf02-e2e-bonsai.jpg")
                .mediaType("IMAGE")
                .slotType("OVERVIEW")
                .caption("BF02 E2E thumbnail")
                .isThumbnail(true)
                .displayOrder(0)
                .build());

        return product;
    }

    private User createUser(String email, String fullName, Role role) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setUsername(email);
        user.setFullName(fullName);
        user.setPhone("090" + Math.abs(email.hashCode() % 10000000));
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private Role findRole(String... roleNames) {
        return roleRepository.findAll()
                .stream()
                .filter(role -> {
                    for (String roleName : roleNames) {
                        if (roleName.equalsIgnoreCase(role.getRoleName())) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Required role not found"));
    }

    private void blockExternalAssets() {
        page.route("**/*", route -> {
            String url = route.request().url();
            if (url.contains("fonts.googleapis.com")
                    || url.contains("fonts.gstatic.com")
                    || url.contains("cdnjs.cloudflare.com")
                    || url.contains("cdn.jsdelivr.net")
                    || url.contains("example.com")) {
                route.abort();
                return;
            }
            route.resume();
        });
    }

    private void loginAs(String email) {
        page.context().clearCookies();
        page.navigate(baseUrl + "/login");
        page.locator("input[name='username']").fill(email);
        page.locator("input[name='password']").fill(PASSWORD);
        page.locator("button[type='submit']").click(new Locator.ClickOptions().setForce(true));
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    private void configurePauseSetting(LocalDate appointmentDate) {
        AppointmentSetting setting = appointmentSettingRepository.findFirstByOrderBySettingIdAsc()
                .orElseGet(AppointmentSetting::new);

        setting.setAutoApprove(setting.getAutoApprove() != null ? setting.getAutoApprove() : true);
        setting.setAutoApproveAfter(setting.getAutoApproveAfter() != null ? setting.getAutoApproveAfter() : 5);
        setting.setAutoComplete(setting.getAutoComplete() != null ? setting.getAutoComplete() : true);
        setting.setAutoCompleteAfter(setting.getAutoCompleteAfter() != null ? setting.getAutoCompleteAfter() : 60);
        setting.setPauseFrom(LocalDateTime.parse(appointmentDate + "T" + "09:00"));
        setting.setPauseTo(LocalDateTime.parse(appointmentDate + "T" + "11:00"));
        setting.setPauseReason("BF02 E2E pause schedule");
        setting.setUpdatedAt(LocalDateTime.now());

        appointmentSettingRepository.saveAndFlush(setting);
    }

    private void configureAutoApprovalSetting() {
        AppointmentSetting setting = appointmentSettingRepository.findFirstByOrderBySettingIdAsc()
                .orElseGet(AppointmentSetting::new);

        setting.setAutoApprove(true);
        setting.setAutoApproveAfter(1);
        setting.setAutoComplete(setting.getAutoComplete() != null ? setting.getAutoComplete() : true);
        setting.setAutoCompleteAfter(setting.getAutoCompleteAfter() != null ? setting.getAutoCompleteAfter() : 60);
        setting.setPauseFrom(null);
        setting.setPauseTo(null);
        setting.setPauseReason(null);
        setting.setUpdatedAt(LocalDateTime.now());

        appointmentSettingRepository.saveAndFlush(setting);
    }

    private void configureAutoCompleteSetting() {
        AppointmentSetting setting = appointmentSettingRepository.findFirstByOrderBySettingIdAsc()
                .orElseGet(AppointmentSetting::new);

        setting.setAutoApprove(setting.getAutoApprove() != null ? setting.getAutoApprove() : true);
        setting.setAutoApproveAfter(setting.getAutoApproveAfter() != null ? setting.getAutoApproveAfter() : 5);
        setting.setAutoComplete(true);
        setting.setAutoCompleteAfter(1);
        setting.setPauseFrom(null);
        setting.setPauseTo(null);
        setting.setPauseReason(null);
        setting.setUpdatedAt(LocalDateTime.now());

        appointmentSettingRepository.saveAndFlush(setting);
    }

    private ViewingAppointment createCompletedCandidateAppointment(
            LocalDateTime appointmentDateTime,
            String note
    ) {
        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setCustomer(customer);
        appointment.setAppointmentDate(appointmentDateTime);
        appointment.setNote(note);
        appointment.setStatus("APPROVED");
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointmentRepository.saveAndFlush(appointment);
    }

    private void clearPauseSetting() {
        appointmentSettingRepository.findFirstByOrderBySettingIdAsc().ifPresent(setting -> {
            setting.setAutoApprove(true);
            setting.setAutoApproveAfter(5);
            setting.setAutoComplete(true);
            setting.setAutoCompleteAfter(60);
            setting.setPauseFrom(null);
            setting.setPauseTo(null);
            setting.setPauseReason(null);
            setting.setUpdatedAt(LocalDateTime.now());
            appointmentSettingRepository.saveAndFlush(setting);
        });
    }

    private ViewingAppointment createApprovedAppointment(
            LocalDate appointmentDate,
            String appointmentTime,
            String note
    ) {
        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setCustomer(customer);
        appointment.setAppointmentDate(LocalDateTime.parse(appointmentDate + "T" + appointmentTime));
        appointment.setNote(note);
        appointment.setStatus("APPROVED");
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointmentRepository.saveAndFlush(appointment);
    }

    @Test
    @DisplayName("TC-E2E-BF02-001 - Customer submits a garden visit booking")
    void tcE2E_BF02_001_customerSubmitsGardenVisitBooking() {
        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        String appointmentTime = "10:00";
        String note = "BF02 E2E garden visit note " + System.nanoTime();

        loginAs(customerEmail);

        page.navigate(baseUrl + "/bonsai-luxury-detail/" + premiumProduct.getProductId());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator("#productTitle").waitFor();
        assertThat(page.locator("body")).containsText(premiumProduct.getProductName());

        page.locator(".schedule-btn").first().click(new Locator.ClickOptions().setForce(true));
        assertThat(page.locator("#bookingModal")).hasClass(java.util.regex.Pattern.compile(".*is-open.*"));

        page.locator("#appointmentDate").fill(appointmentDate.toString());
        page.locator("#appointmentTime").selectOption(appointmentTime);
        page.locator("#note").fill(note);

        page.locator("#actualBookingForm button[type='submit']").click(new Locator.ClickOptions().setForce(true));
        page.waitForURL("**/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator(".appointment-card").first().waitFor();

        assertTrue(page.url().endsWith("/appointments"), "Customer should be redirected to appointment page");
        assertThat(page.locator(".appointment-status-tag[data-status='PENDING']").first()).containsText("Chờ duyệt");
        assertThat(page.locator("body")).containsText(note);

        List<ViewingAppointment> appointments = appointmentRepository.findByCustomerOrderByCreatedAtDesc(customer);
        ViewingAppointment createdAppointment = appointments.stream()
                .filter(appointment -> note.equals(appointment.getNote()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Newly created appointment was not persisted"));

        assertEquals("PENDING", createdAppointment.getStatus());
        assertEquals(appointmentDate, createdAppointment.getAppointmentDate().toLocalDate());
        assertEquals(appointmentTime, createdAppointment.getAppointmentDate().toLocalTime().toString());
    }

    @Test
    @DisplayName("TC-E2E-BF02-002 - Customer cannot book during Artisan pause schedule")
    void tcE2E_BF02_002_customerCannotBookDuringArtisanPause() {
        LocalDate appointmentDate = LocalDate.now().plusDays(2);
        String appointmentTime = "10:00";
        String note = "Test Canot Booking " + System.nanoTime();

        configurePauseSetting(appointmentDate);

        loginAs(customerEmail);

        page.navigate(baseUrl + "/bonsai-luxury-detail/" + premiumProduct.getProductId());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator("#productTitle").waitFor();
        assertThat(page.locator("body")).containsText(premiumProduct.getProductName());

        page.locator(".schedule-btn").first().click(new Locator.ClickOptions().setForce(true));
        assertThat(page.locator("#bookingModal")).hasClass(java.util.regex.Pattern.compile(".*is-open.*"));

        page.locator("#appointmentDate").fill(appointmentDate.toString());
        page.locator("#appointmentTime").selectOption(appointmentTime);
        page.locator("#note").fill(note);

        int appointmentsBefore = appointmentRepository
                .findByCustomerOrderByCreatedAtDesc(customer)
                .size();

        page.locator("#actualBookingForm button[type='submit']").click(new Locator.ClickOptions().setForce(true));
        page.waitForURL("**/bonsai-luxury-detail/" + premiumProduct.getProductId());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertThat(page.locator(".flash-message.flash-error").first())
                .containsText("BF02 E2E pause schedule");

        List<ViewingAppointment> appointmentsAfter =
                appointmentRepository.findByCustomerOrderByCreatedAtDesc(customer);

        assertEquals(
                appointmentsBefore,
                appointmentsAfter.size(),
                "No new appointment should be created during Artisan pause schedule"
        );

        boolean createdConflictAppointment = appointmentsAfter.stream()
                .anyMatch(appointment -> note.equals(appointment.getNote()));

        assertFalse(createdConflictAppointment, "Conflicting appointment must not be persisted");
    }

    @Test
    @DisplayName("TC-E2E-BF02-003 - Customer booking is automatically approved")
    void tcE2E_BF02_003_customerBookingIsAutomaticallyApproved() {
        LocalDate appointmentDate = LocalDate.now().plusDays(3);
        String appointmentTime = "10:00";
        String note = "Test Auto Approve " + System.nanoTime();

        configureAutoApprovalSetting();

        loginAs(customerEmail);

        page.navigate(baseUrl + "/bonsai-luxury-detail/" + premiumProduct.getProductId());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator("#productTitle").waitFor();
        assertThat(page.locator("body")).containsText(premiumProduct.getProductName());

        page.locator(".schedule-btn").first().click(new Locator.ClickOptions().setForce(true));
        assertThat(page.locator("#bookingModal")).hasClass(java.util.regex.Pattern.compile(".*is-open.*"));

        page.locator("#appointmentDate").fill(appointmentDate.toString());
        page.locator("#appointmentTime").selectOption(appointmentTime);
        page.locator("#note").fill(note);

        page.locator("#actualBookingForm button[type='submit']").click(new Locator.ClickOptions().setForce(true));
        page.waitForURL("**/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator(".appointment-card").first().waitFor();

        ViewingAppointment createdAppointment = appointmentRepository
                .findByCustomerOrderByCreatedAtDesc(customer)
                .stream()
                .filter(appointment -> note.equals(appointment.getNote()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Auto-approval candidate appointment was not persisted"));

        createdAppointment.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        appointmentRepository.saveAndFlush(createdAppointment);

        int approvedCount = artisanAppointmentService.processAutoApprove();
        assertTrue(approvedCount > 0, "Auto-approve job should approve at least one appointment");

        page.navigate(baseUrl + "/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator(".appointment-card").first().waitFor();

        assertTrue(page.url().endsWith("/appointments"), "Customer should stay on appointment page");
        assertThat(page.locator(".appointment-status-tag[data-status='APPROVED']").first()).containsText("Đã duyệt");
        assertThat(page.locator("body")).containsText(note);
        assertThat(page.locator("body")).containsText(appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        showApprovedEvidenceOnScreen(note);

        ViewingAppointment approvedAppointment = appointmentRepository.findById(createdAppointment.getAppointmentId())
                .orElseThrow(() -> new AssertionError("Auto-approved appointment was not persisted"));

        assertEquals("APPROVED", approvedAppointment.getStatus(), "Appointment should be automatically approved");
        assertEquals(appointmentDate, approvedAppointment.getAppointmentDate().toLocalDate());
        assertEquals(appointmentTime, approvedAppointment.getAppointmentDate().toLocalTime().toString());
    }

    @Test
    @DisplayName("TC-E2E-BF02-004 - Customer can view approved appointment detail")
    void tcE2E_BF02_004_customerCanViewApprovedAppointmentDetail() {
        LocalDate appointmentDate = LocalDate.now().plusDays(4);
        String appointmentTime = "14:00";
        String note = "BF02 E2E approved detail " + System.nanoTime();

        ViewingAppointment appointment = createApprovedAppointment(
                appointmentDate,
                appointmentTime,
                note
        );

        loginAs(customerEmail);

        page.navigate(baseUrl + "/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Locator appointmentCard = page.locator(".appointment-card")
                .filter(new Locator.FilterOptions().setHasText(note))
                .first();

        appointmentCard.waitFor();

        assertThat(appointmentCard).containsText(note);
        assertThat(appointmentCard.locator(".appointment-status-tag"))
                .hasAttribute("data-status", "APPROVED");

        appointmentCard.locator(".view-detail-btn").click(new Locator.ClickOptions().setForce(true));

        Locator detailModal = page.locator("#appointmentModal");
        assertThat(detailModal).hasClass(java.util.regex.Pattern.compile(".*is-open.*"));

        assertThat(detailModal.locator("#detailCode")).containsText("APT-" + appointment.getAppointmentId());
        assertThat(detailModal.locator("#detailDate"))
                .containsText(appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        assertThat(detailModal.locator("#detailTime")).containsText(appointmentTime);
        assertThat(detailModal.locator("#detailStatus")).not().containsText("Chá»");
        assertThat(detailModal.locator("#detailName")).containsText("Bonsai");
        assertThat(detailModal.locator("#detailNote")).containsText(note);

        showAppointmentDetailEvidenceOnScreen(note);
    }

    @Test
    @DisplayName("TC-E2E-BF02-005 - Customer cancels a PENDING appointment")
    void tcE2E_BF02_005_customerCancelsPendingAppointment() {
        LocalDate appointmentDate = LocalDate.now().plusDays(3);
        String appointmentTime = "14:00";
        String note = "BF02 E2E cancel pending " + System.nanoTime();

        loginAs(customerEmail);

        page.navigate(baseUrl + "/bonsai-luxury-detail/" + premiumProduct.getProductId());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator("#productTitle").waitFor();
        assertThat(page.locator("body")).containsText(premiumProduct.getProductName());

        page.locator(".schedule-btn").first().click(new Locator.ClickOptions().setForce(true));
        assertThat(page.locator("#bookingModal")).hasClass(java.util.regex.Pattern.compile(".*is-open.*"));

        page.locator("#appointmentDate").fill(appointmentDate.toString());
        page.locator("#appointmentTime").selectOption(appointmentTime);
        page.locator("#note").fill(note);

        page.locator("#actualBookingForm button[type='submit']").click(new Locator.ClickOptions().setForce(true));
        page.waitForURL("**/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        ViewingAppointment appointment = appointmentRepository.findByCustomerOrderByCreatedAtDesc(customer)
                .stream()
                .filter(item -> note.equals(item.getNote()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("PENDING appointment was not created"));

        assertEquals("PENDING", appointment.getStatus(), "Appointment must initially be PENDING");

        Locator appointmentCard = page.locator(".appointment-card")
                .filter(new Locator.FilterOptions().setHasText(note))
                .first();

        appointmentCard.waitFor();
        assertThat(appointmentCard.locator(".appointment-status-tag"))
                .hasAttribute("data-status", "PENDING");

        appointmentCard.locator(".trigger-cancel-modal").click(new Locator.ClickOptions().setForce(true));

        Locator cancelModal = page.locator("#cancelAppointmentModal");
        assertThat(cancelModal).hasClass(java.util.regex.Pattern.compile(".*is-open.*"));

        cancelModal.locator("button[type='submit']").click(new Locator.ClickOptions().setForce(true));
        page.waitForURL("**/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Locator cancelledCard = page.locator(".appointment-card")
                .filter(new Locator.FilterOptions().setHasText(note))
                .first();

        cancelledCard.waitFor();
        assertThat(cancelledCard.locator(".appointment-status-tag"))
                .hasAttribute("data-status", "CANCELLED");
        assertThat(cancelledCard.locator(".trigger-cancel-modal")).hasCount(0);
        assertThat(page.locator(".flash-message.flash-success").first()).not().hasCount(0);

        ViewingAppointment cancelledAppointment = appointmentRepository.findById(appointment.getAppointmentId())
                .orElseThrow(() -> new AssertionError("Appointment disappeared from database"));

        assertEquals(
                "CANCELLED",
                cancelledAppointment.getStatus(),
                "Appointment status must change from PENDING to CANCELLED"
        );

        showCancelledEvidenceOnScreen(note);
    }

    @Test
    @DisplayName("TC-E2E-BF02-006 - Customer cannot cancel an APPROVED appointment")
    void tcE2E_BF02_006_customerCannotCancelApprovedAppointment() {
        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        String appointmentTime = "10:00";
        String note = "BF02 E2E approved cancel attempt " + System.nanoTime();

        ViewingAppointment appointment = createApprovedAppointment(
                appointmentDate,
                appointmentTime,
                note
        );

        loginAs(customerEmail);

        page.navigate(baseUrl + "/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Locator appointmentCard = page.locator(".appointment-card")
                .filter(new Locator.FilterOptions().setHasText(note))
                .first();

        appointmentCard.waitFor();
        assertThat(appointmentCard).containsText(note);
        assertThat(appointmentCard.locator(".appointment-status-tag"))
                .hasAttribute("data-status", "APPROVED");

        assertThat(appointmentCard.locator(".trigger-cancel-modal")).hasCount(0);
        assertThat(appointmentCard.locator(".type-locked")).isVisible();

        ViewingAppointment savedAppointment = appointmentRepository.findById(appointment.getAppointmentId())
                .orElseThrow(() -> new AssertionError("Appointment was not found"));

        assertEquals(
                "APPROVED",
                savedAppointment.getStatus(),
                "APPROVED appointment must remain APPROVED"
        );

        showApprovedEvidenceOnScreen(note);
    }

    @Test
    @DisplayName("TC-E2E-BF02-007 - Appointment is automatically completed by scheduler")
    void tcE2E_BF02_007_appointmentAutomaticallyCompleted() {
        configureAutoCompleteSetting();

        LocalDateTime appointmentDateTime = LocalDateTime.now().minusMinutes(2);
        String note = "BF02 E2E auto complete " + System.nanoTime();

        ViewingAppointment appointment = createCompletedCandidateAppointment(
                appointmentDateTime,
                note
        );

        ViewingAppointment before = appointmentRepository.findById(appointment.getAppointmentId())
                .orElseThrow(() -> new AssertionError("Appointment was not found before scheduler run"));

        assertEquals("APPROVED", before.getStatus(), "Appointment should initially be APPROVED");

        appointmentAutoStatusScheduler.scheduledAutoComplete();

        ViewingAppointment completedAppointment = appointmentRepository.findById(appointment.getAppointmentId())
                .orElseThrow(() -> new AssertionError("Appointment was not found after scheduler run"));

        assertEquals(
                "COMPLETED",
                completedAppointment.getStatus(),
                "Scheduler should automatically change appointment status to COMPLETED"
        );

        loginAs(customerEmail);

        page.navigate(baseUrl + "/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Locator appointmentCard = page.locator(".appointment-card")
                .filter(new Locator.FilterOptions().setHasText(note))
                .first();

        appointmentCard.waitFor();
        assertThat(appointmentCard).containsText(note);
        assertThat(appointmentCard.locator(".appointment-status-tag"))
                .hasAttribute("data-status", "COMPLETED");
        assertThat(appointmentCard.locator(".type-locked")).isVisible();

        showCompletedEvidenceOnScreen(note);
    }

    @Test
    @DisplayName("TC-E2E-BF02-008 - Customer can navigate between showroom and appointment history")
    void tcE2E_BF02_008_customerCanNavigateBetweenShowroomAndAppointmentHistory() {
        LocalDate appointmentDate = LocalDate.now().plusDays(3);
        String appointmentTime = "14:00";
        String note = "BF02 E2E navigation test " + System.nanoTime();

        loginAs(customerEmail);

        page.navigate(baseUrl + "/bonsai-luxury-detail/" + premiumProduct.getProductId());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator("#productTitle").waitFor();
        assertThat(page.locator("body")).containsText(premiumProduct.getProductName());

        page.locator(".schedule-btn").first().click(new Locator.ClickOptions().setForce(true));
        assertThat(page.locator("#bookingModal")).hasClass(java.util.regex.Pattern.compile(".*is-open.*"));

        page.locator("#appointmentDate").fill(appointmentDate.toString());
        page.locator("#appointmentTime").selectOption(appointmentTime);
        page.locator("#note").fill(note);

        page.locator("#actualBookingForm button[type='submit']").click(new Locator.ClickOptions().setForce(true));
        page.waitForURL("**/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Locator createdAppointmentCard = page.locator(".appointment-card")
                .filter(new Locator.FilterOptions().setHasText(note))
                .first();

        createdAppointmentCard.waitFor();

        assertTrue(page.url().endsWith("/appointments"), "Customer should be redirected to appointment history");
        assertThat(createdAppointmentCard).containsText(note);
        assertThat(createdAppointmentCard)
                .containsText(appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        assertThat(createdAppointmentCard).containsText(appointmentTime);

        page.navigate(baseUrl + "/bonsai-luxury-detail/" + premiumProduct.getProductId());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator("#productTitle").waitFor();
        assertThat(page.locator("body")).containsText(premiumProduct.getProductName());

        page.navigate(baseUrl + "/appointments");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Locator persistedAppointmentCard = page.locator(".appointment-card")
                .filter(new Locator.FilterOptions().setHasText(note))
                .first();

        persistedAppointmentCard.waitFor();
        assertThat(persistedAppointmentCard).containsText(note);
        assertThat(persistedAppointmentCard)
                .containsText(appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        assertThat(persistedAppointmentCard).containsText(appointmentTime);

        ViewingAppointment createdAppointment = appointmentRepository.findByCustomerOrderByCreatedAtDesc(customer)
                .stream()
                .filter(appointment -> note.equals(appointment.getNote()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Appointment was lost after navigating between pages"));

        assertEquals(appointmentDate, createdAppointment.getAppointmentDate().toLocalDate());
        assertEquals(appointmentTime, createdAppointment.getAppointmentDate().toLocalTime().toString());
        assertEquals(note, createdAppointment.getNote());

        showNavigationEvidenceOnScreen(note);
    }

    private void showApprovedEvidenceOnScreen(String note) {
        page.evaluate("""
                note => {
                    const cards = Array.from(document.querySelectorAll(".appointment-card"));
                    const card = cards.find(item => item.textContent.includes(note));
                    if (!card) return;

                    const badge = card.querySelector(".appointment-status-tag[data-status='APPROVED']");
                    card.scrollIntoView({ behavior: "instant", block: "center" });
                    card.style.outline = "5px solid #16a34a";
                    card.style.boxShadow = "0 0 0 10px rgba(22, 163, 74, 0.25)";

                    if (badge) {
                        badge.style.outline = "4px solid #22c55e";
                        badge.style.background = "#16a34a";
                        badge.style.color = "#ffffff";
                    }
                }
                """, note);
        page.waitForTimeout(Double.parseDouble(System.getProperty("playwright.evidencePause", "3000")));
    }

    private void showAppointmentDetailEvidenceOnScreen(String note) {
        page.evaluate("""
                note => {
                    const modal = document.querySelector("#appointmentModal");
                    const noteElement = document.querySelector("#detailNote");
                    const statusElement = document.querySelector("#detailStatus");
                    if (!modal || !noteElement || !noteElement.textContent.includes(note)) return;

                    modal.scrollIntoView({ behavior: "instant", block: "center" });
                    modal.style.outline = "5px solid #2563eb";
                    modal.style.boxShadow = "0 0 0 10px rgba(37, 99, 235, 0.25)";

                    if (statusElement) {
                        statusElement.style.outline = "4px solid #22c55e";
                        statusElement.style.background = "#16a34a";
                        statusElement.style.color = "#ffffff";
                        statusElement.style.padding = "6px 10px";
                        statusElement.style.borderRadius = "999px";
                    }
                }
                """, note);
        page.waitForTimeout(Double.parseDouble(System.getProperty("playwright.evidencePause", "3000")));
    }

    private void showCancelledEvidenceOnScreen(String note) {
        page.evaluate("""
                note => {
                    const cards = Array.from(document.querySelectorAll(".appointment-card"));
                    const card = cards.find(item => item.textContent.includes(note));
                    if (!card) return;

                    const badge = card.querySelector(".appointment-status-tag[data-status='CANCELLED']");
                    card.scrollIntoView({ behavior: "instant", block: "center" });
                    card.style.outline = "5px solid #dc2626";
                    card.style.boxShadow = "0 0 0 10px rgba(220, 38, 38, 0.25)";

                    if (badge) {
                        badge.style.outline = "4px solid #ef4444";
                        badge.style.background = "#dc2626";
                        badge.style.color = "#ffffff";
                    }
                }
                """, note);
        page.waitForTimeout(Double.parseDouble(System.getProperty("playwright.evidencePause", "3000")));
    }

    private void showCompletedEvidenceOnScreen(String note) {
        page.evaluate("""
                note => {
                    const cards = Array.from(document.querySelectorAll(".appointment-card"));
                    const card = cards.find(item => item.textContent.includes(note));
                    if (!card) return;

                    const badge = card.querySelector(".appointment-status-tag[data-status='COMPLETED']");
                    card.scrollIntoView({ behavior: "instant", block: "center" });
                    card.style.outline = "5px solid #7c3aed";
                    card.style.boxShadow = "0 0 0 10px rgba(124, 58, 237, 0.25)";

                    if (badge) {
                        badge.style.outline = "4px solid #8b5cf6";
                        badge.style.background = "#7c3aed";
                        badge.style.color = "#ffffff";
                    }
                }
                """, note);
        page.waitForTimeout(Double.parseDouble(System.getProperty("playwright.evidencePause", "3000")));
    }

    private void showNavigationEvidenceOnScreen(String note) {
        page.evaluate("""
                note => {
                    const cards = Array.from(document.querySelectorAll(".appointment-card"));
                    const card = cards.find(item => item.textContent.includes(note));
                    if (!card) return;

                    card.scrollIntoView({ behavior: "instant", block: "center" });
                    card.style.outline = "5px solid #f59e0b";
                    card.style.boxShadow = "0 0 0 10px rgba(245, 158, 11, 0.25)";
                }
                """, note);
        page.waitForTimeout(Double.parseDouble(System.getProperty("playwright.evidencePause", "3000")));
    }




}
