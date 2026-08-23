package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import com.example.bonsai_shop.owner.service.AccountService;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductMediaRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.ProductTagRepository;
import com.example.bonsai_shop.product.repository.TagRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BF03ProductCatalogSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

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
    private ProductMediaRepository mediaRepository;

    @Autowired
    private ProductTagRepository productTagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private CloudinaryStorageService cloudinaryStorageService;

    private RequestPostProcessor artisanUser() {
        User artisan = findOrCreateArtisan();
        return user(new CustomUserDetails(
                artisan,
                List.of(new SimpleGrantedAuthority("ROLE_ARTISAN"))
        ));
    }

    private User findOrCreateArtisan() {
        Role artisanRole = findRole("ARTISAN", "ROLE_ARTISAN");
        String email = "artisan.bf03@test.com";

        User artisan = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    accountService.createAccount(
                            "BF03 Artisan Test",
                            email,
                            "123456",
                            "0910000003",
                            artisanRole.getRoleId()
                    );

                    return accountRepository.findAll()
                            .stream()
                            .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                            .findFirst()
                            .orElseThrow();
                });

        artisan.setStatus("ACTIVE");
        artisan.setRole(artisanRole);
        return accountRepository.save(artisan);
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

    private CatalogData catalogData() {
        Category category = categoryRepository.save(Category.builder()
                .categoryName("BF03 Category")
                .description("BF03 category")
                .build());

        Variety variety = varietyRepository.save(Variety.builder()
                .category(category)
                .varietyName("BF03 Variety")
                .description("BF03 variety")
                .build());

        ProductSegment standardSegment = segmentRepository.save(ProductSegment.builder()
                .segmentName("Budget")
                .build());

        Tag tag = tagRepository.save(Tag.builder()
                .tagName("BF03 Tag")
                .build());

        return new CatalogData(category, variety, standardSegment, tag);
    }

    private Product createDraftProduct() throws Exception {
        CatalogData data = catalogData();
        String productName = "BF03 Draft Bonsai";

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productName", productName)
                        .param("varietyId", String.valueOf(data.variety().getVarietyId()))
                        .param("segmentId", String.valueOf(data.standardSegment().getSegmentId()))
                        .param("description", "Initial BF03 description")
                        .param("treeStory", "Initial BF03 tree story")
                        .param("age", "12")
                        .param("height", "55.5")
                        .param("trunkDiameter", "8.5")
                        .param("style", "Formal Upright")
                        .param("price", "1500000")
                        .param("tagIds", String.valueOf(data.tag().getTagId())))
                .andExpect(status().is3xxRedirection());

        return productRepository.findAll()
                .stream()
                .filter(product -> productName.equals(product.getProductName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Draft product was not created"));
    }

    private MockMultipartFile validImage() {
        return new MockMultipartFile(
                "files",
                "bf03-bonsai.jpg",
                "image/jpeg",
                "valid image content".getBytes()
        );
    }

    private void mockImageUpload() {
        when(cloudinaryStorageService.uploadImage(any(), any()))
                .thenReturn(new CloudinaryUploadResponse(
                        "https://res.cloudinary.com/test/image/upload/bf03-bonsai.jpg",
                        "bf03-bonsai",
                        "image"
                ));
    }

    @Test
    void tcSysBF03001_artisanCanPublishProductAndCustomerCanOpenDetails() throws Exception {

        CatalogData data = catalogData();
        String productName = "BF03 Created Bonsai";

        mockMvc.perform(get("/artisan/products/new")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().attributeExists(
                        "productForm",
                        "categories",
                        "varieties",
                        "segments",
                        "tags"
                ));

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productName", productName)
                        .param("varietyId", String.valueOf(data.variety().getVarietyId()))
                        .param("segmentId", String.valueOf(data.standardSegment().getSegmentId()))
                        .param("description", "BF03 description")
                        .param("treeStory", "BF03 tree story")
                        .param("age", "10")
                        .param("height", "45.5")
                        .param("trunkDiameter", "7.5")
                        .param("style", "Informal Upright")
                        .param("price", "1200000")
                        .param("tagIds", String.valueOf(data.tag().getTagId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"))
                .andExpect(flash().attributeExists("success"));

        Product product = productRepository.findAll()
                .stream()
                .filter(savedProduct -> productName.equals(savedProduct.getProductName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Created product was not found"));

        assertEquals("DRAFT", product.getProductStatus());
        assertEquals(Boolean.FALSE, product.getIsVisible());
        assertEquals(findOrCreateArtisan().getUserId(), product.getCreatedBy().getUserId());
        assertEquals(data.variety().getVarietyId(), product.getVariety().getVarietyId());
        assertEquals(data.standardSegment().getSegmentId(), product.getSegment().getSegmentId());
        assertEquals("BF03 tree story", product.getTreeStory());
        assertEquals(1, productTagRepository.findByProduct(product).size());

        mockImageUpload();

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/media")
                        .file(validImage())
                        .with(artisanUser())
                        .with(csrf())
                        .param("mediaTypes", "IMAGE")
                        .param("slotTypes", "OVERVIEW")
                        .param("captions", "Overview image")
                        .param("thumbnailIndex", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/media"))
                .andExpect(flash().attributeExists("success"));

        List<ProductMedia> mediaList = mediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product);

        assertEquals(1, mediaList.size());
        assertEquals("IMAGE", mediaList.get(0).getMediaType());
        assertEquals("OVERVIEW", mediaList.get(0).getSlotType());
        assertEquals("Overview image", mediaList.get(0).getCaption());
        assertEquals(Boolean.TRUE, mediaList.get(0).getIsThumbnail());

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/media")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-media"))
                .andExpect(model().attribute("mediaList", hasItem(
                        hasProperty("slotType", equalTo("OVERVIEW"))
                )));

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/preview")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-preview"))
                .andExpect(model().attribute("product", hasProperty(
                        "productName",
                        equalTo(product.getProductName())
                )))
                .andExpect(model().attribute("mediaList", hasItem(
                        hasProperty("slotType", equalTo("OVERVIEW"))
                )));

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .with(artisanUser())
                        .with(csrf())
                        .param("productName", "BF03 Updated Bonsai")
                        .param("varietyId", String.valueOf(data.variety().getVarietyId()))
                        .param("segmentId", String.valueOf(data.standardSegment().getSegmentId()))
                        .param("description", "Updated BF03 description")
                        .param("treeStory", "Updated BF03 tree story")
                        .param("age", "15")
                        .param("height", "70.5")
                        .param("trunkDiameter", "12.5")
                        .param("style", "Cascade")
                        .param("price", "1800000")
                        .param("productStatus", "DRAFT")
                        .param("tagIds", String.valueOf(data.tag().getTagId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/preview"))
                .andExpect(flash().attributeExists("success"));

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();

        assertEquals("BF03 Updated Bonsai", updatedProduct.getProductName());
        assertEquals("Updated BF03 tree story", updatedProduct.getTreeStory());
        assertEquals(data.standardSegment().getSegmentId(), updatedProduct.getSegment().getSegmentId());
        assertEquals("DRAFT", updatedProduct.getProductStatus());
        assertEquals(Boolean.FALSE, updatedProduct.getIsVisible());

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/preview")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-preview"))
                .andExpect(model().attribute("product", hasProperty(
                        "productName",
                        equalTo("BF03 Updated Bonsai")
                )))
                .andExpect(model().attribute("mediaList", hasItem(
                        hasProperty("slotType", equalTo("OVERVIEW"))
                )));

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("success"));

        Product publishedProduct = productRepository.findById(product.getProductId()).orElseThrow();

        assertEquals("AVAILABLE", publishedProduct.getProductStatus());
        assertEquals(Boolean.TRUE, publishedProduct.getIsVisible());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/marketplace")
                        .param("keyword", updatedProduct.getProductName()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/marketplace"))
                .andExpect(model().attributeExists("products"))
                .andExpect(content().string(containsString(updatedProduct.getProductName())));

        mockMvc.perform(get("/product/" + product.getProductId()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/product-detail"))
                .andExpect(model().attribute("product", hasProperty(
                        "productName",
                        equalTo(updatedProduct.getProductName())
                )))
                .andExpect(model().attribute("productImages", hasItem(
                        hasProperty("slotType", equalTo("OVERVIEW"))
                )));
    }

    @Test
    void tcSysBF03002_invalidMediaUploadIsRejected() throws Exception {

        Product product = createDraftProduct();
        MockMultipartFile image = new MockMultipartFile(
                "files",
                "bf03-invalid.jpg",
                "image/jpeg",
                "invalid image content".getBytes()
        );

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/media")
                        .file(image)
                        .with(artisanUser())
                        .with(csrf())
                        .param("mediaTypes", "IMAGE")
                        .param("slotTypes", "INVALID")
                        .param("captions", "Invalid shot type")
                        .param("thumbnailIndex", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/media"))
                .andExpect(flash().attributeExists("error"));

        assertTrue(mediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product).isEmpty());
    }

    @Test
    void tcSysBF03003_publishWithoutMediaIsBlocked() throws Exception {

        Product product = createDraftProduct();

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("error"));

        Product unchangedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("DRAFT", unchangedProduct.getProductStatus());
        assertEquals(Boolean.FALSE, unchangedProduct.getIsVisible());
    }

    @Test
    void tcSysBF03004_catalogManageCategorySuccessAndFailures() throws Exception {
        // 1. Get catalog page
        mockMvc.perform(get("/artisan/catalog")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("varieties"))
                .andExpect(model().attributeExists("tags"));

        // 2. Create Category success
        mockMvc.perform(post("/artisan/catalog/categories")
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryName", "Valid Category New")
                        .param("description", "A valid category description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã tạo category."));

        Category created = categoryRepository.findAll().stream()
                .filter(c -> "Valid Category New".equals(c.getCategoryName()))
                .findFirst().orElseThrow();

        // 3. Create Duplicate Category fails
        mockMvc.perform(post("/artisan/catalog/categories")
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryName", "Valid Category New")
                        .param("description", "duplicate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Tên category đã tồn tại."));

        // 4. Update Category success
        mockMvc.perform(post("/artisan/catalog/categories/" + created.getCategoryId())
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryName", "Updated Category Name")
                        .param("description", "Updated desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã cập nhật category."));

        // 5. Delete Category success
        mockMvc.perform(post("/artisan/catalog/categories/" + created.getCategoryId() + "/delete")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã xóa category."));
    }

    @Test
    void tcSysBF03005_catalogManageVarietySuccessAndFailures() throws Exception {
        Category category = categoryRepository.save(Category.builder().categoryName("Variety Parent Category").build());

        // 1. Create Variety success
        mockMvc.perform(post("/artisan/catalog/varieties")
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("varietyName", "New Variety Name")
                        .param("description", "Variety description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã tạo variety."));

        Variety variety = varietyRepository.findAll().stream()
                .filter(v -> "New Variety Name".equals(v.getVarietyName()))
                .findFirst().orElseThrow();

        // 2. Create Duplicate Variety fails
        mockMvc.perform(post("/artisan/catalog/varieties")
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("varietyName", "New Variety Name")
                        .param("description", "another"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Tên variety đã tồn tại trong category này."));

        // 3. Update Variety success
        mockMvc.perform(post("/artisan/catalog/varieties/" + variety.getVarietyId())
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryId", String.valueOf(category.getCategoryId()))
                        .param("varietyName", "Updated Variety Name")
                        .param("description", "Updated desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã cập nhật variety."));

        // 4. Delete Variety success
        mockMvc.perform(post("/artisan/catalog/varieties/" + variety.getVarietyId() + "/delete")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã xóa variety."));
    }

    @Test
    void tcSysBF03006_catalogManageTagSuccessAndFailures() throws Exception {
        // 1. Create Tag success
        mockMvc.perform(post("/artisan/catalog/tags")
                        .with(artisanUser())
                        .with(csrf())
                        .param("tagName", "New Tag Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã tạo tag."));

        Tag tag = tagRepository.findAll().stream()
                .filter(t -> "New Tag Name".equals(t.getTagName()))
                .findFirst().orElseThrow();

        // 2. Create Duplicate Tag fails
        mockMvc.perform(post("/artisan/catalog/tags")
                        .with(artisanUser())
                        .with(csrf())
                        .param("tagName", "New Tag Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Tên tag đã tồn tại."));

        // 3. Update Tag success
        mockMvc.perform(post("/artisan/catalog/tags/" + tag.getTagId())
                        .with(artisanUser())
                        .with(csrf())
                        .param("tagName", "Updated Tag Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã cập nhật tag."));

        // 4. Delete Tag success
        mockMvc.perform(post("/artisan/catalog/tags/" + tag.getTagId() + "/delete")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("success", "Đã xóa tag."));
    }

    @Test
    void tcSysBF03007_catalogValidationAndDependencyConstraints() throws Exception {
        // 1. Category name empty fail
        mockMvc.perform(post("/artisan/catalog/categories")
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryName", "")
                        .param("description", "desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Tên category không được để trống!"));

        // 2. Category name too long fail
        String tooLongName = "A".repeat(256);
        mockMvc.perform(post("/artisan/catalog/categories")
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryName", tooLongName)
                        .param("description", "desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Tên không được vượt quá 255 ký tự."));

        // 3. Category name invalid characters fail
        mockMvc.perform(post("/artisan/catalog/categories")
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryName", "Category#@!$%")
                        .param("description", "desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Tên chỉ được chứa chữ, số, khoảng trắng và các ký tự . , ' - ( )."));

        // 4. Description too long fail
        String tooLongDesc = "A".repeat(501);
        mockMvc.perform(post("/artisan/catalog/categories")
                        .with(artisanUser())
                        .with(csrf())
                        .param("categoryName", "Valid Name Desc Too Long")
                        .param("description", tooLongDesc))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Mô tả không được vượt quá 500 ký tự."));

        // 5. Delete Category in use with variety fails
        Category inUseCategory = categoryRepository.save(Category.builder().categoryName("Category In Use").build());
        varietyRepository.save(Variety.builder().category(inUseCategory).varietyName("Child Variety").build());
        
        mockMvc.perform(post("/artisan/catalog/categories/" + inUseCategory.getCategoryId() + "/delete")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Không thể xóa category đang có variety."));

        // 6. Delete Category/Variety/Tag not found fails
        mockMvc.perform(post("/artisan/catalog/categories/99999/delete")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"));
    }

    private record CatalogData(
            Category category,
            Variety variety,
            ProductSegment standardSegment,
            Tag tag
    ) {
    }
}
