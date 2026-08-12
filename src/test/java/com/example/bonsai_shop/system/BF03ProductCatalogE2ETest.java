package com.example.bonsai_shop.system;

import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.TagRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BF03ProductCatalogE2ETest {

    private static final String ARTISAN_EMAIL = "artisan.bf03.e2e@test.com";
    private static final String CUSTOMER_EMAIL = "customer.bf03.e2e@test.com";
    private static final String PASSWORD = "123456";

    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository segmentRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CloudinaryStorageService cloudinaryStorageService;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private String baseUrl;

    @BeforeAll
    void setUpAll() {
        baseUrl = "http://localhost:" + port;
        cleanExistingBf03Products();
        createUser(ARTISAN_EMAIL, "BF03 E2E Artisan", findRole("ARTISAN", "ROLE_ARTISAN"));
        createUser(CUSTOMER_EMAIL, "BF03 E2E Customer", findRole("CUSTOMER", "ROLE_CUSTOMER"));
        mockImageUpload();

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(1000));
    }

    @BeforeEach
    void setUp() {
        context = browser.newContext();
        page = context.newPage();
        page.onConsoleMessage(message -> System.out.println("[BF03 BROWSER CONSOLE] "
                + message.type() + ": " + message.text()));
        page.onPageError(error -> System.out.println("[BF03 BROWSER ERROR] " + error));
        page.onResponse(response -> {
            if (response.status() >= 400) {
                System.out.println("[BF03 HTTP " + response.status() + "] " + response.url());
            }
        });
    }

    @AfterEach
    void tearDown() {
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

    @Test
    void tcE2eBF03001_artisanCanCreatePublishAndCustomerCanSeeProduct() {
        CatalogData catalogData = catalogData("BF03 E2E Main");
        String productName = "BF03 E2E Bonsai " + System.nanoTime();

        loginAs(ARTISAN_EMAIL);

        page.navigate(baseUrl + "/artisan/products");
        assertTrue(page.url().endsWith("/artisan/products"));

        String createHref = page.locator("a[href='/artisan/products/new']").first().getAttribute("href");
        page.navigate(baseUrl + createHref);
        assertTrue(page.url().endsWith("/artisan/products/new"));

        fillProductForm(
                productName,
                catalogData,
                catalogData.standardSegment(),
                "E2E description",
                "E2E tree story",
                "10",
                "45.5",
                "7.5",
                "Formal");
        page.locator("form.form-card button[type='submit']")
                .click();
        page.waitForURL("**/artisan/products/*/media");
        page.waitForLoadState(LoadState.LOAD);
        assertTrue(page.url().contains("/media"));
        assertThat(page.locator("body")).containsText("Bước 2");
        assertThat(page.locator(".dashboard-layout")).isVisible();
        assertThat(page.locator(".page-header h1")).isVisible();
        Integer productId = extractProductIdFromCurrentUrl();

        uploadMedia();
        assertThat(page.locator(".media-card")).containsText("OVERVIEW");
        assertThat(page.locator(".media-card input[name='captions']")).hasValue("E2E overview image");
        assertTrue(page.locator(".media-current-thumbnail, .media-badge").count() > 0);

        String previewHref = page.locator("a[href*='/preview']").first().getAttribute("href");
        page.navigate(baseUrl + previewHref);
        assertThat(page.locator("body")).containsText(productName);

        page.navigate(baseUrl + "/artisan/products/" + productId + "/edit");
        fillProductForm(
                productName,
                catalogData,
                catalogData.eliteSegment(),
                "Updated E2E description",
                "Updated E2E tree story",
                "15",
                "70.5",
                "12.5",
                "Cascade");
        assertThat(page.locator("select[name='segmentId']"))
                .hasValue(String.valueOf(catalogData.eliteSegment().getSegmentId()));
        page.locator("form.form-card button[type='submit']")
                .click();
        page.waitForURL("**/artisan/products/*/preview");
        page.waitForLoadState(LoadState.LOAD);

        page.locator("[data-open-publish-modal]").click();
        page.locator("#artisanPublishModal .btn-modal-confirm").click();
        page.waitForURL("**/artisan/products");
        page.waitForLoadState(LoadState.LOAD);
        assertThat(page.locator("body")).containsText(productName);

        Product publishedProduct = productRepository.findById(productId).orElseThrow();

        assertEquals("AVAILABLE", publishedProduct.getProductStatus());
        assertEquals(Boolean.TRUE, publishedProduct.getIsVisible());
        assertEquals(Boolean.FALSE, publishedProduct.getIsPublicPrice());

        page.context().clearCookies();
        loginAs(CUSTOMER_EMAIL);
        page.navigate(baseUrl + "/marketplace?keyword=" + URLEncoder.encode(productName, StandardCharsets.UTF_8));
        assertThat(page.locator("body")).containsText(productName);
    }

    @Test
    void tcE2eBF03002_nonArtisanCannotOpenArtisanProductUi() {
        loginAs(CUSTOMER_EMAIL);

        page.navigate(baseUrl + "/artisan/products/new");

        assertTrue(
                page.url().contains("/login")
                        || page.locator("body").textContent().contains("403")
                        || page.locator("body").textContent().toLowerCase().contains("forbidden"));
    }

    @Test
    void tcE2eBF03003_logoutInvalidatesArtisanSession() {
        loginAs(ARTISAN_EMAIL);
        page.navigate(baseUrl + "/artisan/products");
        assertTrue(page.url().endsWith("/artisan/products"));

        page.waitForNavigation(() -> page.locator(".sidebar-logout-btn").first()
                .click());
        assertTrue(page.url().contains("/login"));

        page.navigate(baseUrl + "/artisan/products");

        assertTrue(page.url().contains("/login"));
    }

    private void loginAs(String email) {
        page.navigate(baseUrl + "/login");
        page.locator("#email").fill(email);
        page.locator("#password").fill(PASSWORD);
        page.locator("button.btn-signin").click();
        page.waitForURL(url -> !url.contains("/login"));
    }

    private void fillProductForm(String productName,
            CatalogData catalogData,
            ProductSegment segment,
            String description,
            String treeStory,
            String age,
            String height,
            String trunkDiameter,
            String style) {
        page.locator("input[name='productName']").fill(productName);
        page.locator("#categorySelect").selectOption(String.valueOf(catalogData.category().getCategoryId()));
        page.locator("select[name='varietyId']").selectOption(String.valueOf(catalogData.variety().getVarietyId()));
        page.locator("select[name='segmentId']").selectOption(String.valueOf(segment.getSegmentId()));
        page.locator("textarea[name='description']").fill(description);
        page.locator("textarea[name='treeStory']").fill(treeStory);
        page.locator("input[name='age']").fill(age);
        page.locator("input[name='height']").fill(height);
        page.locator("input[name='trunkDiameter']").fill(trunkDiameter);
        page.locator("input[name='style']").fill(style);
        page.locator("#priceDisplay").fill("1500000");
        page.locator("input[name='price']").evaluate("element => element.value = '1500000'");
    }

    private void uploadMedia() {
        page.waitForSelector(".media-upload-item input[type='file'][name='files']");
        page.setInputFiles(".media-upload-item input[type='file'][name='files']", Path.of("src/test/resources/e2e/bf03-bonsai.png"));
        page.locator(".media-upload-item select[name='slotTypes']").selectOption("OVERVIEW");
        page.locator(".media-upload-item input[name='captions']").fill("E2E overview image");
        page.locator(".media-upload-item input[name='thumbnailIndex']").check();
        page.locator("#uploadAllBtn").click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForSelector(".media-card");
    }

    private Integer extractProductIdFromCurrentUrl() {
        String marker = "/artisan/products/";
        String url = page.url();
        int start = url.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Product id not found in URL: " + url);
        }

        int idStart = start + marker.length();
        int idEnd = url.indexOf('/', idStart);
        String productId = idEnd < 0 ? url.substring(idStart) : url.substring(idStart, idEnd);
        return Integer.valueOf(productId);
    }

    private User createUser(String email, String fullName, Role role) {
        User user = accountRepository.findAll()
                .stream()
                .filter(candidate -> email.equalsIgnoreCase(candidate.getEmail()))
                .findFirst()
                .orElseGet(User::new);

        user.setEmail(email);
        user.setUsername(email);
        user.setFullName(fullName);
        user.setPhone("090" + Math.abs(email.hashCode() % 10000000));
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(user);
    }

    private void cleanExistingBf03Products() {
        productRepository.findAll().stream()
                .filter(product -> product.getProductName() != null
                        && product.getProductName().startsWith("BF03 E2E Bonsai "))
                .forEach(productRepository::delete);
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

    private CatalogData catalogData(String prefix) {
        Category category = categoryRepository.save(Category.builder()
                .categoryName(prefix + " Category")
                .description(prefix + " category")
                .build());

        Variety variety = varietyRepository.save(Variety.builder()
                .category(category)
                .varietyName(prefix + " Variety")
                .description(prefix + " variety")
                .build());

        ProductSegment standardSegment = segmentRepository.save(ProductSegment.builder()
                .segmentName(prefix + " Budget")
                .build());

        ProductSegment eliteSegment = segmentRepository.save(ProductSegment.builder()
                .segmentName("Elite")
                .build());

        Tag tag = tagRepository.save(Tag.builder()
                .tagName(prefix + " Tag")
                .build());

        return new CatalogData(category, variety, standardSegment, eliteSegment, tag);
    }

    private void mockImageUpload() {
        when(cloudinaryStorageService.uploadImage(any(), any()))
                .thenReturn(new CloudinaryUploadResponse(
                        "/images/bonsai-1.png",
                        "bf03-e2e-bonsai",
                        "image"));
    }

    private record CatalogData(
            Category category,
            Variety variety,
            ProductSegment standardSegment,
            ProductSegment eliteSegment,
            Tag tag) {
    }
}
