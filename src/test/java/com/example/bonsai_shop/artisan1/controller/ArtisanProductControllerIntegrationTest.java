package com.example.bonsai_shop.artisan1.controller;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductJournalEvent;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.artisan.service.ArtisanMediaStorageService;
import com.example.bonsai_shop.integration.support.AbstractDatabaseSafeIntegrationTest;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductJournalEventRepository;
import com.example.bonsai_shop.product.repository.ProductMediaRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.TagRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ArtisanProductControllerIntegrationTest extends AbstractDatabaseSafeIntegrationTest {

    private static final String ARTISAN_EMAIL = "artisan-product-controller-it2@example.com";
    private static final String PRODUCT_CODE = "APC-IT2-001";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMediaRepository productMediaRepository;

    @Autowired
    private ProductJournalEventRepository productJournalEventRepository;

    @Autowired
    private TagRepository tagRepository;

    @MockitoBean
    private ArtisanMediaStorageService mediaStorageService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private ProductSegmentRepository productSegmentRepository;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TestArtisanPrincipal principal = new TestArtisanPrincipal(ARTISAN_EMAIL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(mediaStorageService.storeProductMedia(any())).thenReturn("https://cdn.test/media.jpg");
    }

    @AfterEach
    void cleanupTestData() {
        SecurityContextHolder.clearContext();
        productRepository.findByProductCode(PRODUCT_CODE).ifPresent(productRepository::delete);
        productRepository.findByCreatedByUserIdOrderByCreatedAtDesc(
                userRepository.findByEmail(ARTISAN_EMAIL).map(User::getUserId).orElse(-1)
        ).forEach(productRepository::delete);
        userRepository.findByEmail(ARTISAN_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void myProducts_WhenArtisanRequestsProducts_ShouldDisplayProductList() throws Exception {
        User artisan = createTestArtisan();
        Variety variety = createTestVariety();
        ProductSegment segment = createTestSegment();
        Product product = createTestProduct(artisan, variety, segment);

        mockMvc.perform(get("/artisan/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/products"))
                .andExpect(model().attribute("products", hasItem(hasProperty("productId", is(product.getProductId())))));
    }

    @Test
    void createForm_WhenArtisanRequestsNewProductForm_ShouldDisplayLookupData() throws Exception {
        createTestVariety();
        createTestSegment();

        mockMvc.perform(get("/artisan/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().attributeExists("productForm", "categories", "varieties", "segments", "tags"))
                .andExpect(model().attribute("product", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void create_WhenFormIsValid_ShouldCreateProductAndRedirectToMediaPage() throws Exception {
        createTestArtisan();
        Variety variety = createTestVariety();
        ProductSegment segment = createTestSegment();

        mockMvc.perform(post("/artisan/products")
                        .param("varietyId", variety.getVarietyId().toString())
                        .param("segmentId", segment.getSegmentId().toString())
                        .param("productName", "Integration Bonsai")
                        .param("description", "Created by full Spring MVC integration test")
                        .param("treeStory", "Seeded data")
                        .param("age", "12")
                        .param("height", "45.5")
                        .param("trunkDiameter", "8.5")
                        .param("style", "Formal Upright")
                        .param("price", "5000000")
                        .param("productStatus", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("success"));

        Product savedProduct = productRepository.findByCreatedByUserIdOrderByCreatedAtDesc(
                        userRepository.findByEmail(ARTISAN_EMAIL).orElseThrow().getUserId())
                .stream()
                .findFirst()
                .orElseThrow();

        assertEquals("Integration Bonsai", savedProduct.getProductName());
        assertEquals("DRAFT", savedProduct.getProductStatus());
        assertTrue(savedProduct.getProductCode() != null && !savedProduct.getProductCode().isBlank());
        productRepository.delete(savedProduct);
    }

    @Test
    void create_WhenFormIsInvalid_ShouldReturnFormViewWithBindingErrorsAndLookupData() throws Exception {
        createTestVariety();
        createTestSegment();

        mockMvc.perform(post("/artisan/products")
                        .param("productName", "")
                        .param("price", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm"));
    }

    @Test
    void create_WhenServiceRejectsValidForm_ShouldReturnFormViewWithError() throws Exception {
        createTestArtisan();
        ProductSegment segment = createTestSegment();

        mockMvc.perform(post("/artisan/products")
                        .param("varietyId", "999999")
                        .param("segmentId", segment.getSegmentId().toString())
                        .param("productName", "Integration Bonsai")
                        .param("age", "12")
                        .param("height", "45.5")
                        .param("trunkDiameter", "8.5")
                        .param("style", "Formal Upright")
                        .param("price", "5000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void editForm_WhenProductIsEditable_ShouldDisplayPopulatedForm() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().attribute("product", hasProperty("productId", is(product.getProductId()))))
                .andExpect(model().attributeExists("productForm", "categories", "varieties", "segments", "tags"));
    }

    @Test
    void editForm_WhenProductIsNotEditable_ShouldRedirectToPreviewWithError() throws Exception {
        Product product = createOwnedProduct("AVAILABLE", true);

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/preview"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void update_WhenFormIsValid_ShouldUpdateProductAndRedirectToPreview() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        Variety variety = product.getVariety();
        ProductSegment segment = product.getSegment();

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .param("varietyId", variety.getVarietyId().toString())
                        .param("segmentId", segment.getSegmentId().toString())
                        .param("productName", "Updated Integration Bonsai")
                        .param("description", "Updated")
                        .param("treeStory", "Updated")
                        .param("age", "13")
                        .param("height", "46")
                        .param("trunkDiameter", "9")
                        .param("style", "Cascade")
                        .param("price", "6000000")
                        .param("productStatus", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/preview"))
                .andExpect(flash().attributeExists("success"));

        assertEquals("Updated Integration Bonsai", productRepository.findById(product.getProductId()).orElseThrow().getProductName());
    }

    @Test
    void update_WhenFormIsInvalid_ShouldReturnFormViewWithBindingErrorsAndLookupData() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .param("productName", "")
                        .param("price", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm"));
    }

    @Test
    void update_WhenServiceRejectsValidForm_ShouldReturnFormViewWithError() throws Exception {
        Product product = createOwnedProduct("AVAILABLE", true);

        mockMvc.perform(validUpdatePost(product, "Blocked Update"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void delete_WhenServiceDeletesProductSuccessfully_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void delete_WhenServiceRejectsDeletion_ShouldRedirectWithError() throws Exception {
        Product product = createOwnedProduct("AVAILABLE", true);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void mediaForm_WhenProductHasMedia_ShouldDisplayMediaManagementPage() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        createProductMedia(product, "IMAGE", true);

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/media"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-media"))
                .andExpect(model().attributeExists("product", "mediaList", "isSold", "isEditable"));
    }

    @Test
    void addMedia_WhenUploadBatchIsValid_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/media")
                        .file(imageFile())
                        .param("mediaTypes", "IMAGE")
                        .param("slotTypes", "FRONT")
                        .param("captions", "Front")
                        .param("thumbnailIndex", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void addMedia_WhenUploadBatchInvalid_ShouldRedirectWithError() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/media")
                        .file(new MockMultipartFile("files", "bad.txt", "text/plain", "bad".getBytes()))
                        .param("mediaTypes", "IMAGE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void setThumbnail_WhenMediaIsEligible_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        ProductMedia media = createProductMedia(product, "IMAGE", false);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/media/" + media.getMediaId() + "/thumbnail"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void setThumbnail_WhenMediaIsIneligible_ShouldRedirectWithError() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        ProductMedia media = createProductMedia(product, "VIDEO", false);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/media/" + media.getMediaId() + "/thumbnail"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void updateMediaOrder_WhenDataIsValid_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        ProductMedia media = createProductMedia(product, "IMAGE", true);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/media/order")
                        .param("mediaIds", media.getMediaId().toString())
                        .param("displayOrders", "2")
                        .param("slotTypes", "DETAIL")
                        .param("captions", "Detail"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void updateMediaOrder_WhenDataIsInvalid_ShouldRedirectWithError() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/media/order")
                        .param("mediaIds", "999999")
                        .param("displayOrders", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void deleteMedia_WhenServiceDeletesSuccessfully_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        ProductMedia media = createProductMedia(product, "IMAGE", true);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/media/" + media.getMediaId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void journal_WhenProductIsNotSold_ShouldDisplayJournalPage() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        createJournalEvent(product);

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-journal"))
                .andExpect(model().attributeExists("product", "journalEvents", "today"));
    }

    @Test
    void journal_WhenProductIsSold_ShouldRedirectToPreviewWithError() throws Exception {
        Product product = createOwnedProduct("SOLD", false);

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/preview"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void addJournalEvent_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal")
                        .file(imageFile())
                        .file(imageFile())
                        .file(imageFile())
                        .param("eventType", "PHOTO_UPDATE")
                        .param("title", "Repotting")
                        .param("description", "Updated roots")
                        .param("isPublic", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/journal"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void addJournalEvent_WhenServiceRejectsNoMedia_ShouldRedirectWithError() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal")
                        .param("eventType", "PHOTO_UPDATE")
                        .param("title", "No media"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/journal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void deleteJournalEvent_WhenServiceDeletesSuccessfully_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/journal"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void updateJournalEventText_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId())
                        .param("title", "Updated journal")
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/journal"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void updateJournalEventVisibility_WhenIsPublicChanges_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId() + "/visibility")
                        .param("isPublic", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/journal"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void addJournalEventMedia_WhenFilesAreValid_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId() + "/media")
                        .file(imageFile()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/journal"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void preview_WhenProductHasMediaAndTags_ShouldDisplayPreviewPage() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        createProductMedia(product, "IMAGE", true);
        Tag tag = tagRepository.save(Tag.builder().tagName("IT2 Tag").build());

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/preview"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-preview"))
                .andExpect(model().attributeExists("product", "mediaList", "thumbnail", "tags", "imageCount", "videoCount"));
    }

    @Test
    void publish_WhenServicePublishesSuccessfully_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);
        createProductMedia(product, "IMAGE", true);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void publish_WhenServiceRejectsPublication_ShouldRedirectWithError() throws Exception {
        Product product = createOwnedProduct("DRAFT", false);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void hide_WhenServiceHidesSuccessfully_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("AVAILABLE", true);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/hide"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void show_WhenServiceShowsSuccessfully_ShouldRedirectWithSuccess() throws Exception {
        Product product = createOwnedProduct("AVAILABLE", false);
        createProductMedia(product, "IMAGE", true);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/show"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void preview_WhenProductAccessIsRejectedByService_ShouldPropagateAccessFailure() throws Exception {
        createTestArtisan();

        assertThrows(Exception.class, () -> mockMvc.perform(get("/artisan/products/999999/preview")));
    }

    @Test
    void myProducts_WhenNoAuthenticatedPrincipalExists_ShouldNotCallControllerService() throws Exception {
        SecurityContextHolder.clearContext();

        assertThrows(Exception.class, () -> mockMvc.perform(get("/artisan/products")));
    }

    private User createTestArtisan() {
        Role role = roleRepository.findByRoleName("ARTISAN")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ARTISAN").description("ARTISAN").build()));
        User user = new User();
        user.setEmail(ARTISAN_EMAIL);
        user.setUsername(ARTISAN_EMAIL);
        user.setFullName("Artisan Product Controller IT2");
        user.setPhone("0987654321");
        user.setPassword("password123");
        user.setStatus("ACTIVE");
        user.setRole(role);
        return userRepository.save(user);
    }

    private Category createTestCategory() {
        return categoryRepository.save(Category.builder()
                .categoryName("IT2 Category")
                .description("Integration test category")
                .build());
    }

    private Variety createTestVariety() {
        Category category = createTestCategory();
        return varietyRepository.save(Variety.builder()
                .category(category)
                .varietyName("IT2 Variety")
                .description("Integration test variety")
                .build());
    }

    private ProductSegment createTestSegment() {
        return productSegmentRepository.save(ProductSegment.builder()
                .segmentName("Standard")
                .build());
    }

    private Product createTestProduct(User artisan, Variety variety, ProductSegment segment) {
        Product product = new Product();
        product.setCreatedBy(artisan);
        product.setVariety(variety);
        product.setSegment(segment);
        product.setProductCode(PRODUCT_CODE);
        product.setProductName("Existing Integration Bonsai");
        product.setDescription("Existing product for controller integration test");
        product.setTreeStory("Existing story");
        product.setAge(10);
        product.setHeight(40.0f);
        product.setTrunkDiameter(7.0f);
        product.setStyle("Informal Upright");
        product.setPrice(new BigDecimal("3000000"));
        product.setProductStatus("DRAFT");
        product.setIsVisible(false);
        product.setIsPublicPrice(true);
        product.setViewCount(0);
        product.setCreatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    private Product createOwnedProduct(String status, boolean visible) {
        User artisan = userRepository.findByEmail(ARTISAN_EMAIL).orElseGet(this::createTestArtisan);
        return createTestProduct(artisan, createTestVariety(), createTestSegment(), "APC-IT2-" + System.nanoTime(), status, visible);
    }

    private Product createTestProduct(User artisan, Variety variety, ProductSegment segment, String code, String status, boolean visible) {
        Product product = new Product();
        product.setCreatedBy(artisan);
        product.setVariety(variety);
        product.setSegment(segment);
        product.setProductCode(code);
        product.setProductName("Existing Integration Bonsai");
        product.setDescription("Existing product for controller integration test");
        product.setTreeStory("Existing story");
        product.setAge(10);
        product.setHeight(40.0f);
        product.setTrunkDiameter(7.0f);
        product.setStyle("Informal Upright");
        product.setPrice(new BigDecimal("3000000"));
        product.setProductStatus(status);
        product.setIsVisible(visible);
        product.setIsPublicPrice(true);
        product.setViewCount(0);
        product.setCreatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validUpdatePost(Product product, String productName) {
        return post("/artisan/products/" + product.getProductId())
                .param("varietyId", product.getVariety().getVarietyId().toString())
                .param("segmentId", product.getSegment().getSegmentId().toString())
                .param("productName", productName)
                .param("description", "Updated")
                .param("treeStory", "Updated")
                .param("age", "13")
                .param("height", "46")
                .param("trunkDiameter", "9")
                .param("style", "Cascade")
                .param("price", "6000000")
                .param("productStatus", product.getProductStatus());
    }

    private ProductMedia createProductMedia(Product product, String mediaType, boolean thumbnail) {
        return productMediaRepository.save(ProductMedia.builder()
                .product(product)
                .mediaUrl("https://cdn.test/" + System.nanoTime() + "." + ("VIDEO".equals(mediaType) ? "mp4" : "jpg"))
                .mediaType(mediaType)
                .slotType("FRONT")
                .caption("Media")
                .isThumbnail(thumbnail)
                .displayOrder(1)
                .build());
    }

    private ProductJournalEvent createJournalEvent(Product product) {
        User artisan = userRepository.findByEmail(ARTISAN_EMAIL).orElseGet(this::createTestArtisan);
        return productJournalEventRepository.save(ProductJournalEvent.builder()
                .product(product)
                .createdBy(artisan)
                .eventDate(java.time.LocalDate.now())
                .eventType("PHOTO_UPDATE")
                .title("Journal Event")
                .description("Journal Description")
                .isPublic(true)
                .build());
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile("files", "bonsai.jpg", "image/jpeg", "image".getBytes());
    }

    private record TestArtisanPrincipal(String username) implements UserDetails {

        public String getFullName() {
            return "Artisan Product Controller IT2";
        }

        public String getAvatar() {
            return "";
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority("ROLE_ARTISAN"));
        }

        @Override
        public String getPassword() {
            return "password123";
        }
    }
}
