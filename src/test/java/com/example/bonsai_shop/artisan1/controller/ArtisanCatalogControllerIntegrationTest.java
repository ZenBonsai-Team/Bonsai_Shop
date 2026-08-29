package com.example.bonsai_shop.artisan1.controller;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.ProductTag;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.integration.support.AbstractDatabaseSafeIntegrationTest;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.ProductTagRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@Transactional
class ArtisanCatalogControllerIntegrationTest extends AbstractDatabaseSafeIntegrationTest {

    private static final String ARTISAN_EMAIL = "artisan-catalog-controller-it@example.com";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSegmentRepository productSegmentRepository;

    @Autowired
    private ProductTagRepository productTagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TestArtisanPrincipal principal = new TestArtisanPrincipal(ARTISAN_EMAIL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void catalog_WhenRequested_ShouldDisplayCatalogManagementPage() throws Exception {
        Category category = createCategory("Catalog IT Category");
        Variety variety = createVariety(category, "Catalog IT Variety");
        Tag tag = createTag("Catalog IT Tag");

        mockMvc.perform(get("/artisan/catalog"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/catalog"))
                .andExpect(model().attribute("categories", hasItem(hasProperty("categoryId", is(category.getCategoryId())))))
                .andExpect(model().attribute("varieties", hasItem(hasProperty("varietyId", is(variety.getVarietyId())))))
                .andExpect(model().attribute("tags", hasItem(hasProperty("tagId", is(tag.getTagId())))))
                .andExpect(model().attributeExists(
                        "categoryIdsInUse",
                        "varietyIdsInUse",
                        "tagIdsInUse",
                        "varietyCountByCategoryId",
                        "productCountByCategoryId",
                        "productCountByVarietyId",
                        "productCountByTagId"));
    }

    @Test
    void createCategory_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        String categoryName = uniqueName("Outdoor");

        mockMvc.perform(post("/artisan/catalog/categories")
                        .param("categoryName", categoryName)
                        .param("description", "Outdoor bonsai"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        assertTrue(categoryRepository.existsByCategoryNameIgnoreCase(categoryName));
    }

    @Test
    void createCategory_WhenServiceRejectsCreation_ShouldRedirectWithError() throws Exception {
        String categoryName = uniqueName("Duplicate Category");
        createCategory(categoryName);

        mockMvc.perform(post("/artisan/catalog/categories")
                        .param("categoryName", categoryName)
                        .param("description", "Outdoor bonsai"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void updateCategory_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        Category category = createCategory(uniqueName("Old Category"));
        String updatedName = uniqueName("Updated Category");

        mockMvc.perform(post("/artisan/catalog/categories/" + category.getCategoryId())
                        .param("categoryName", updatedName)
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        assertEquals(updatedName, categoryRepository.findById(category.getCategoryId()).orElseThrow().getCategoryName());
    }

    @Test
    void deleteCategory_WhenServiceAllowsOrRejectsDeletion_ShouldRedirectWithExpectedFlash() throws Exception {
        Category unusedCategory = createCategory(uniqueName("Unused Category"));

        mockMvc.perform(post("/artisan/catalog/categories/" + unusedCategory.getCategoryId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        assertFalse(categoryRepository.existsById(unusedCategory.getCategoryId()));

        Category categoryInUse = createCategory(uniqueName("Category In Use"));
        createVariety(categoryInUse, uniqueName("Variety Blocks Category Delete"));

        mockMvc.perform(post("/artisan/catalog/categories/" + categoryInUse.getCategoryId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void createVariety_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        Category category = createCategory(uniqueName("Variety Parent"));
        String varietyName = uniqueName("Kim Gion");

        mockMvc.perform(post("/artisan/catalog/varieties")
                        .param("categoryId", category.getCategoryId().toString())
                        .param("varietyName", varietyName)
                        .param("description", "Small leaves"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        assertTrue(varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCase(
                category.getCategoryId(), varietyName));
    }

    @Test
    void createVariety_WhenCategoryInvalid_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/artisan/catalog/varieties")
                        .param("categoryId", "999999")
                        .param("varietyName", uniqueName("Kim Gion"))
                        .param("description", "Small leaves"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void updateVariety_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        Category oldCategory = createCategory(uniqueName("Old Parent"));
        Category newCategory = createCategory(uniqueName("New Parent"));
        Variety variety = createVariety(oldCategory, uniqueName("Old Variety"));
        String updatedName = uniqueName("Updated Variety");

        mockMvc.perform(post("/artisan/catalog/varieties/" + variety.getVarietyId())
                        .param("categoryId", newCategory.getCategoryId().toString())
                        .param("varietyName", updatedName)
                        .param("description", "Updated leaves"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        Variety updatedVariety = varietyRepository.findById(variety.getVarietyId()).orElseThrow();
        assertEquals(updatedName, updatedVariety.getVarietyName());
        assertEquals(newCategory.getCategoryId(), updatedVariety.getCategory().getCategoryId());
    }

    @Test
    void deleteVariety_WhenServiceAllowsOrRejectsDeletion_ShouldRedirectWithExpectedFlash() throws Exception {
        Category category = createCategory(uniqueName("Delete Variety Parent"));
        Variety unusedVariety = createVariety(category, uniqueName("Unused Variety"));

        mockMvc.perform(post("/artisan/catalog/varieties/" + unusedVariety.getVarietyId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        assertFalse(varietyRepository.existsById(unusedVariety.getVarietyId()));

        Variety varietyInUse = createVariety(category, uniqueName("Variety In Use"));
        createProduct(varietyInUse);

        mockMvc.perform(post("/artisan/catalog/varieties/" + varietyInUse.getVarietyId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void createTag_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        String tagName = uniqueName("Mini");

        mockMvc.perform(post("/artisan/catalog/tags")
                        .param("tagName", tagName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        assertTrue(tagRepository.existsByTagNameIgnoreCase(tagName));
    }

    @Test
    void createTag_WhenServiceRejectsCreation_ShouldRedirectWithError() throws Exception {
        String tagName = uniqueName("Duplicate Tag");
        createTag(tagName);

        mockMvc.perform(post("/artisan/catalog/tags")
                        .param("tagName", tagName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void updateTag_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        Tag tag = createTag(uniqueName("Old Tag"));
        String updatedName = uniqueName("Updated Tag");

        mockMvc.perform(post("/artisan/catalog/tags/" + tag.getTagId())
                        .param("tagName", updatedName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        assertEquals(updatedName, tagRepository.findById(tag.getTagId()).orElseThrow().getTagName());
    }

    @Test
    void deleteTag_WhenServiceAllowsOrRejectsDeletion_ShouldRedirectWithExpectedFlash() throws Exception {
        Tag unusedTag = createTag(uniqueName("Unused Tag"));

        mockMvc.perform(post("/artisan/catalog/tags/" + unusedTag.getTagId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        assertFalse(tagRepository.existsById(unusedTag.getTagId()));

        Category category = createCategory(uniqueName("Tagged Product Category"));
        Variety variety = createVariety(category, uniqueName("Tagged Product Variety"));
        Product product = createProduct(variety);
        Tag tagInUse = createTag(uniqueName("Tag In Use"));
        productTagRepository.save(ProductTag.builder()
                .product(product)
                .tag(tagInUse)
                .build());

        mockMvc.perform(post("/artisan/catalog/tags/" + tagInUse.getTagId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("error"));
    }

    private Category createCategory(String categoryName) {
        return categoryRepository.save(Category.builder()
                .categoryName(categoryName)
                .description("Integration test category")
                .build());
    }

    private Variety createVariety(Category category, String varietyName) {
        return varietyRepository.save(Variety.builder()
                .category(category)
                .varietyName(varietyName)
                .description("Integration test variety")
                .build());
    }

    private Tag createTag(String tagName) {
        return tagRepository.save(Tag.builder()
                .tagName(tagName)
                .build());
    }

    private Product createProduct(Variety variety) {
        ProductSegment segment = productSegmentRepository.save(ProductSegment.builder()
                .segmentName(uniqueName("Standard Segment"))
                .build());
        return productRepository.save(Product.builder()
                .createdBy(userRepository.findByEmail(ARTISAN_EMAIL).orElseGet(this::createTestArtisan))
                .variety(variety)
                .segment(segment)
                .productCode("ACC-IT-" + System.nanoTime())
                .productName("Catalog Integration Bonsai")
                .description("Product used by catalog integration test")
                .treeStory("Catalog integration")
                .age(10)
                .height(40.0f)
                .trunkDiameter(7.0f)
                .style("Formal Upright")
                .price(new BigDecimal("3000000"))
                .productStatus("AVAILABLE")
                .isVisible(true)
                .isPublicPrice(true)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private User createTestArtisan() {
        Role role = roleRepository.findByRoleName("ARTISAN")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ARTISAN").description("ARTISAN").build()));
        return userRepository.save(User.builder()
                .email(ARTISAN_EMAIL)
                .username(ARTISAN_EMAIL)
                .fullName("Artisan Catalog Controller IT")
                .phone("0987654321")
                .password("password123")
                .status("ACTIVE")
                .role(role)
                .build());
    }

    private String uniqueName(String prefix) {
        return prefix + " " + System.nanoTime();
    }

    private record TestArtisanPrincipal(String username) implements UserDetails {

        public String getFullName() {
            return "Artisan Catalog Controller IT";
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
