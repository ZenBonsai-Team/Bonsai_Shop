package com.example.bonsai_shop.system;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application.properties")
class BF05StaffAccountE2ETest {

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;

    @LocalServerPort
    private int port;

    private String baseUrl;

    private static final String OWNER_EMAIL = "owner@bonsai.com";
    private static final String OWNER_PASSWORD = "123";

    private static final String STAFF_PASSWORD = "123456";

    @BeforeAll
    static void beforeAll() {
        playwright = Playwright.create();

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(Boolean.parseBoolean(System.getProperty("playwright.headless", "false")))
        );
    }

    @BeforeEach
    void beforeEach() {
        baseUrl = System.getProperty("e2e.baseUrl", "http://localhost:" + port);
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void afterEach() {
        context.close();
    }

    @AfterAll
    static void afterAll() {
        browser.close();
        playwright.close();
    }

    private void login(Page targetPage, String email, String password) {
        targetPage.navigate(baseUrl + "/login");

        targetPage.locator("#email")
                .fill(email);

        targetPage.locator("#password")
                .fill(password);

        targetPage.locator("button.btn-signin")
                .click();
    }

    private void loginAsOwner() {
        login(page, OWNER_EMAIL, OWNER_PASSWORD);
        page.waitForURL(url -> url.endsWith("/owner"));
    }

    private String selectArtisanRole() {
        Object roleName = page.locator("#roleId").evaluate("""
                select => {
                    const option = Array.from(select.options)
                        .find(item => {
                            const roleName = item.textContent.trim().toUpperCase();
                            return item.value && (roleName === 'ARTISAN' || roleName === 'ROLE_ARTISAN');
                        });

                    if (!option) {
                        return null;
                    }

                    select.value = option.value;
                    select.dispatchEvent(new Event('change', { bubbles: true }));
                    return option.textContent.trim();
                }
                """);

        assertNotNull(roleName, "Form tao tai khoan phai co role ARTISAN de gan cho nhan vien moi");
        return String.valueOf(roleName);
    }

    private void logout() {
        page.locator("form[action='/logout'] button[type='submit']").first().click();
        page.waitForURL(url -> url.endsWith("/login?logout"));
    }

    private void openOwnerPageAndAssertVisible(String path, String contentSelector, String tableSelector) {
        loginAsOwner();

        page.navigate(baseUrl + path);
        page.waitForLoadState();

        assertTrue(page.url().endsWith(path));
        assertTrue(page.locator(contentSelector).isVisible());
        assertTrue(page.locator(tableSelector).isVisible());
    }

    private void assertOwnerReportSummaryIsVisible() {
        Locator summary = page.locator(".owner-report-summary strong").first();

        assertTrue(summary.isVisible());
        assertFalse(summary.innerText().isBlank());
    }

    @Test
    void tcE2EBF05001_ownerCreatesArtisanStaffAccountAndArtisanCanOpenAuthorizedPage() {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        String staffEmail = "artisan.e2e." + uniqueSuffix + "@bonsai.com";
        String staffFullName = "Artisan E2E " + uniqueSuffix;
        String staffPhone = "09" + uniqueSuffix.substring(uniqueSuffix.length() - 8);

        loginAsOwner();

        page.navigate(baseUrl + "/owner/users");
        page.waitForLoadState();

        assertTrue(page.url().endsWith("/owner/users"));
        assertTrue(page.locator("table").isVisible());
        assertTrue(page.locator("a[href='/owner/users/create']").isVisible());

        page.locator("a[href='/owner/users/create']").click();
        page.waitForURL(url -> url.endsWith("/owner/users/create"));

        assertTrue(page.locator("#createUserForm").isVisible());

        page.locator("#fullName").fill(staffFullName);
        page.locator("#email").fill(staffEmail);
        page.locator("#password").fill(STAFF_PASSWORD);
        page.locator("#phone").fill(staffPhone);

        String selectedRoleName = selectArtisanRole();

        assertFalse(page.locator("#createUserSubmit").isDisabled());

        page.locator("#createUserSubmit").click();
        page.waitForURL(url -> url.endsWith("/owner/users"));

        page.locator("#searchEmail").fill(staffEmail);

        Locator createdUserRow = page.locator("tbody#userTableBody tr")
                .filter(new Locator.FilterOptions().setHasText(staffEmail));

        createdUserRow.first().waitFor();
        assertTrue(createdUserRow.first().isVisible());
        assertEquals("ACTIVE", createdUserRow.first().getAttribute("data-status"));
        assertEquals(selectedRoleName, createdUserRow.first().getAttribute("data-role"));

        logout();

        login(page, staffEmail, STAFF_PASSWORD);
        page.waitForURL(url -> url.endsWith("/artisan/products"));

        page.navigate(baseUrl + "/artisan/products");
        page.waitForLoadState();

        assertTrue(page.url().endsWith("/artisan/products"));
        assertTrue(page.locator("body").isVisible());
    }

    @Test
    void tcE2EBF05002_ownerCannotCreateStaffAccountWithExistingEmail() {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        String staffFullName = "Duplicate Email E2E " + uniqueSuffix;
        String staffPhone = "08" + uniqueSuffix.substring(uniqueSuffix.length() - 8);

        loginAsOwner();

        page.navigate(baseUrl + "/owner/users/create");
        page.waitForLoadState();

        assertTrue(page.url().endsWith("/owner/users/create"));
        assertTrue(page.locator("#createUserForm").isVisible());

        page.locator("#fullName").fill(staffFullName);
        page.locator("#email").fill(OWNER_EMAIL);
        page.locator("#password").fill(STAFF_PASSWORD);
        page.locator("#phone").fill(staffPhone);
        selectArtisanRole();

        assertFalse(page.locator("#createUserSubmit").isDisabled());

        page.locator("#createUserSubmit").click();
        page.waitForURL(url -> url.endsWith("/owner/users/create"));

        assertTrue(page.locator(".alert-error").isVisible());
        assertTrue(page.locator(".alert-error").innerText().contains("Email"));
        assertEquals(OWNER_EMAIL, page.locator("#email").inputValue());
        assertEquals(staffFullName, page.locator("#fullName").inputValue());
        assertEquals(staffPhone, page.locator("#phone").inputValue());
    }

    @Test
    void tcE2EBF05003_ownerCanViewOrderHistoryList() {
        openOwnerPageAndAssertVisible(
                "/owner/order-history",
                "#order-history-content",
                "table.owner-order-history-table"
        );

        assertTrue(page.locator("#ownerOrderHistorySearchForm").isVisible());
        assertTrue(page.locator("#ownerOrderTypeSelect").isVisible());
        assertTrue(page.locator(".owner-order-status-tabs").isVisible());
    }

    @Test
    void tcE2EBF05004_ownerCanViewArtisanRevenueReport() {
        openOwnerPageAndAssertVisible(
                "/owner/dashboard/artisan-revenue",
                "#owner-artisan-revenue-content",
                "table.owner-artisan-revenue-table"
        );

        assertOwnerReportSummaryIsVisible();
        assertTrue(page.locator("#ownerArtisanRevenueMonth").isVisible());
    }

    @Test
    void tcE2EBF05005_ownerCanViewGardenTreesReport() {
        openOwnerPageAndAssertVisible(
                "/owner/dashboard/garden-trees",
                "#owner-garden-trees-content",
                "table.owner-report-table"
        );

        assertOwnerReportSummaryIsVisible();
    }

    @Test
    void tcE2EBF05006_ownerCanViewSoldTreesReport() {
        openOwnerPageAndAssertVisible(
                "/owner/dashboard/sold-trees",
                "#owner-sold-trees-content",
                "table.owner-report-table"
        );

        assertOwnerReportSummaryIsVisible();
    }
}

