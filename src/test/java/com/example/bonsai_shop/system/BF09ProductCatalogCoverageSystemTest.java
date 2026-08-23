package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import com.example.bonsai_shop.product.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BF09ProductCatalogCoverageSystemTest {

    @Autowired
    private MockMvc mockMvc;

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
    private ProductTagRepository productTagRepository;

    private Category testCategory;
    private Variety testVariety;
    private ProductSegment standardSegment;
    private Tag testTag;
    private User testArtisan;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(Category.builder()
                .categoryName("BF09 Cat " + System.nanoTime()).build());

        testVariety = varietyRepository.save(Variety.builder()
                .category(testCategory)
                .varietyName("BF09 Variety " + System.nanoTime()).build());

        standardSegment = segmentRepository.save(ProductSegment.builder()
                .segmentName("BF09 Standard " + System.nanoTime()).build());

        // Epic workaround: Dam bao no co segmentId = 3 neu can hoac ta mock de tranh loi logic.
        // Luu y trong database test segmentId cua standard segment co the la bat ky ID nao khac 3.

        testTag = tagRepository.save(Tag.builder()
                .tagName("BF09 Tag " + System.nanoTime()).build());

        testArtisan = findOrCreateArtisan();
    }

    private RequestPostProcessor customerUser() {
        User customer = findOrCreateCustomer();
        return user(new CustomUserDetails(customer,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private User findOrCreateArtisan() {
        Role role = roleRepository.findByRoleName("ROLE_ARTISAN")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_ARTISAN").description("Artisan").build()));
        String email = "artisan.bf09@test.com";
        return accountRepository.findAll().stream()
                .filter(item -> email.equalsIgnoreCase(item.getEmail()))
                .findFirst()
                .orElseGet(() -> accountRepository.save(User.builder()
                        .fullName("Artisan BF09")
                        .email(email)
                        .username("artisan.bf09")
                        .password("123456")
                        .phone("0910000091")
                        .status("ACTIVE")
                        .role(role)
                        .build()));
    }

    private User findOrCreateCustomer() {
        Role role = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_CUSTOMER").description("Customer").build()));
        String email = "customer.bf09@test.com";
        return accountRepository.findAll().stream()
                .filter(item -> email.equalsIgnoreCase(item.getEmail()))
                .findFirst()
                .orElseGet(() -> accountRepository.save(User.builder()
                        .fullName("Customer BF09")
                        .email(email)
                        .username("customer.bf09")
                        .password("123456")
                        .phone("0910000092")
                        .status("ACTIVE")
                        .role(role)
                        .build()));
    }

    private Product createProduct(String name, String code, String status, Boolean isVisible, ProductSegment segment, BigDecimal price, Integer age) {
        Product p = productRepository.save(Product.builder()
                .productName(name)
                .productCode(code)
                .productStatus(status)
                .isVisible(isVisible)
                .segment(segment)
                .variety(testVariety)
                .price(price)
                .age(age)
                .style("Informal Upright")
                .height(50.0f)
                .trunkDiameter(8.0f)
                .createdBy(testArtisan)
                .createdAt(LocalDateTime.now())
                .isPublicPrice(true)
                .viewCount(0)
                .build());

        productTagRepository.save(ProductTag.builder()
                .product(p)
                .tag(testTag)
                .build());

        return p;
    }

    // ======================== SEARCH FILTERS TESTS ========================

    @Test
    void testMarketplace_FilterKeywordAndAvailable() throws Exception {
        // Tạo sản phẩm mẫu
        createProduct("Bonsai Pine BF09", "PIN-001", "AVAILABLE", true, standardSegment, new BigDecimal("1500000"), 6);
        createProduct("Bonsai Maple BF09", "MAP-002", "SOLD", true, standardSegment, new BigDecimal("2500000"), 12);
        createProduct("Bonsai Hide BF09", "HID-003", "AVAILABLE", false, standardSegment, new BigDecimal("800000"), 4);

        // Test filter keyword
        mockMvc.perform(get("/marketplace")
                        .param("keyword", "Pine")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/marketplace"))
                .andExpect(model().attribute("products", hasProperty("content", hasSize(1))))
                .andExpect(model().attribute("products", hasProperty("content", hasItem(hasProperty("productName", containsString("Pine"))))));

        // Test filter availableOnly
        mockMvc.perform(get("/marketplace")
                        .param("availableOnly", "true")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasProperty("content", everyItem(hasProperty("productStatus", equalTo("AVAILABLE"))))));
    }

    @Test
    void testMarketplace_FilterSegmentAndCategory() throws Exception {
        createProduct("Bonsai Spec Segment", "SEG-09", "AVAILABLE", true, standardSegment, new BigDecimal("2000000"), 8);

        // Lọc theo segment ID
        mockMvc.perform(get("/marketplace")
                        .param("segment", String.valueOf(standardSegment.getSegmentId()))
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasProperty("content", hasItem(hasProperty("productCode", equalTo("SEG-09"))))));

        // Lọc theo segment Name
        mockMvc.perform(get("/marketplace")
                        .param("segment", standardSegment.getSegmentName())
                        .with(customerUser()))
                .andExpect(status().isOk());

        // Lọc theo category ID
        mockMvc.perform(get("/marketplace")
                        .param("category", String.valueOf(testCategory.getCategoryId()))
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasProperty("content", hasItem(hasProperty("productCode", equalTo("SEG-09"))))));

        // Lọc theo category Name
        mockMvc.perform(get("/marketplace")
                        .param("category", testCategory.getCategoryName())
                        .with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    void testMarketplace_FilterPricesAndTags() throws Exception {
        createProduct("Bonsai Cheap", "CHP-01", "AVAILABLE", true, standardSegment, new BigDecimal("500000"), 3);
        createProduct("Bonsai Pricey", "PRC-02", "AVAILABLE", true, standardSegment, new BigDecimal("6000000"), 15);

        // Lọc minPrice & maxPrice
        mockMvc.perform(get("/marketplace")
                        .param("minPrice", "1000000")
                        .param("maxPrice", "7000000")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasProperty("content", hasItem(hasProperty("productCode", equalTo("PRC-02"))))));

        // Lọc theo Tag ID
        mockMvc.perform(get("/marketplace")
                        .param("tagIds", String.valueOf(testTag.getTagId()))
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasProperty("content", hasItem(hasProperty("productCode", equalTo("PRC-02"))))));
    }

    @Test
    void testMarketplace_PriceRangesAndAgeRanges() throws Exception {
        createProduct("Range 1", "R1", "AVAILABLE", true, standardSegment, new BigDecimal("800000"), 3); // under 1M, age under 5
        createProduct("Range 2", "R2", "AVAILABLE", true, standardSegment, new BigDecimal("3000000"), 8); // 1M-5M, age 5-10
        createProduct("Range 3", "R3", "AVAILABLE", true, standardSegment, new BigDecimal("8000000"), 15); // 5M-10M, age 11-20
        createProduct("Range 4", "R4", "AVAILABLE", true, standardSegment, new BigDecimal("20000000"), 25); // 10M-30M, age over 20
        createProduct("Range 5", "R5", "AVAILABLE", true, standardSegment, new BigDecimal("50000000"), 10); // 30M-100M
        createProduct("Range 6", "R6", "AVAILABLE", true, standardSegment, new BigDecimal("150000000"), 10); // over 100M

        // Test các khoảng giá
        String[] priceRanges = {"under1M", "1Mto5M", "5Mto10M", "10Mto30M", "30Mto100M", "over100M"};
        for (String pr : priceRanges) {
            mockMvc.perform(get("/marketplace")
                            .param("priceRanges", pr)
                            .with(customerUser()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("products", hasProperty("content", is(not(empty())))));
        }

        // Test các khoảng tuổi
        String[] ageRanges = {"under5", "5to10", "11to20", "over20"};
        for (String ar : ageRanges) {
            mockMvc.perform(get("/marketplace")
                            .param("ages", ar)
                            .with(customerUser()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("products", hasProperty("content", is(not(empty())))));
        }

        // Lọc species
        mockMvc.perform(get("/marketplace")
                        .param("species", testVariety.getVarietyName())
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasProperty("content", is(not(empty())))));
    }

    @Test
    void testMarketplace_SortParameters() throws Exception {
        createProduct("Pine A", "PA", "AVAILABLE", true, standardSegment, new BigDecimal("1000000"), 5);
        createProduct("Pine B", "PB", "AVAILABLE", true, standardSegment, new BigDecimal("3000000"), 10);

        // Sort price asc
        mockMvc.perform(get("/marketplace").param("sort", "price_asc").with(customerUser()))
                .andExpect(status().isOk());

        // Sort price desc
        mockMvc.perform(get("/marketplace").param("sort", "price_desc").with(customerUser()))
                .andExpect(status().isOk());

        // Sort age desc
        mockMvc.perform(get("/marketplace").param("sort", "age_desc").with(customerUser()))
                .andExpect(status().isOk());

        // Sort default
        mockMvc.perform(get("/marketplace").param("sort", "default").with(customerUser()))
                .andExpect(status().isOk());
    }

    // ======================== PRODUCT DETAIL TESTS ========================

    @Test
    void testProductDetail_Success() throws Exception {
        Product p = createProduct("Detail Product", "DT-01", "AVAILABLE", true, standardSegment, new BigDecimal("2000000"), 10);

        // Xem bằng Path Variable
        mockMvc.perform(get("/product/" + p.getProductId())
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/product-detail"))
                .andExpect(model().attribute("product", hasProperty("productName", equalTo("Detail Product"))));

        // Xem bằng Request Parameter
        mockMvc.perform(get("/products/detail")
                        .param("id", String.valueOf(p.getProductId()))
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/product-detail"));
    }

    @Test
    void testProductDetail_WhenProductNotFoundOrHidden_ShouldRedirect() throws Exception {
        // Case 1: Product không tồn tại -> fallback sang sản phẩm demo trong DB, status 200
        mockMvc.perform(get("/product/999999")
                        .with(customerUser()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/detail") // parameter id null/empty -> fallback sang sản phẩm demo trong DB, status 200
                        .with(customerUser()))
                .andExpect(status().isOk());

        // Case 2: Product DRAFT (ẩn)
        Product draftProduct = createProduct("Draft Product", "DFT-01", "DRAFT", true, standardSegment, new BigDecimal("1000000"), 5);
        mockMvc.perform(get("/product/" + draftProduct.getProductId())
                        .with(customerUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/marketplace"));

        // Case 3: Product isVisible = false (ẩn)
        Product hiddenProduct = createProduct("Hidden Product", "HDN-01", "AVAILABLE", false, standardSegment, new BigDecimal("1000000"), 5);
        mockMvc.perform(get("/product/" + hiddenProduct.getProductId())
                        .with(customerUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/marketplace"));
    }
}
