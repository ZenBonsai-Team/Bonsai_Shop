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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
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

        ProductSegment eliteSegment = segmentRepository.save(ProductSegment.builder()
                .segmentName("Elite")
                .build());

        Tag tag = tagRepository.save(Tag.builder()
                .tagName("BF03 Tag")
                .build());

        return new CatalogData(category, variety, standardSegment, eliteSegment, tag);
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

    private Product createDraftProductWithMedia() throws Exception {
        Product product = createDraftProduct();
        mockImageUpload();

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/media")
                        .file(validImage())
                        .with(artisanUser())
                        .with(csrf())
                        .param("mediaTypes", "IMAGE")
                        .param("slotTypes", "OVERVIEW")
                        .param("captions", "Overview image")
                        .param("thumbnailIndex", "0"))
                .andExpect(status().is3xxRedirection());

        return productRepository.findById(product.getProductId()).orElseThrow();
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
    void tcSysBF03001_artisanCanOpenCreateProductForm() throws Exception {

        catalogData();
        findOrCreateArtisan();

        mockMvc.perform(formLogin("/login")
                        .user("artisan.bf03@test.com")
                        .password("123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan"));

        mockMvc.perform(get("/artisan/products")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/products"))
                .andExpect(model().attributeExists("products"));

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
    }

    @Test
    void tcSysBF03002_artisanCanCreateDraftProduct() throws Exception {

        CatalogData data = catalogData();
        String productName = "BF03 Created Bonsai";

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
    }

    @Test
    void tcSysBF03003_artisanCanUploadMediaAndSetThumbnail() throws Exception {

        Product product = createDraftProduct();
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
    }

    @Test
    void tcSysBF03004_artisanCanEditDraftProductAndElitePriceIsHidden() throws Exception {

        Product product = createDraftProduct();
        CatalogData data = catalogData();

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .with(artisanUser())
                        .with(csrf())
                        .param("productName", "BF03 Elite Bonsai")
                        .param("varietyId", String.valueOf(data.variety().getVarietyId()))
                        .param("segmentId", String.valueOf(data.eliteSegment().getSegmentId()))
                        .param("description", "Updated BF03 description")
                        .param("treeStory", "Updated BF03 tree story")
                        .param("age", "15")
                        .param("height", "70.5")
                        .param("trunkDiameter", "12.5")
                        .param("style", "Cascade")
                        .param("price", "5000000")
                        .param("productStatus", "DRAFT")
                        .param("tagIds", String.valueOf(data.tag().getTagId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/preview"))
                .andExpect(flash().attributeExists("success"));

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();

        assertEquals("BF03 Elite Bonsai", updatedProduct.getProductName());
        assertEquals("Updated BF03 tree story", updatedProduct.getTreeStory());
        assertEquals(data.eliteSegment().getSegmentId(), updatedProduct.getSegment().getSegmentId());
        assertEquals(Boolean.FALSE, updatedProduct.getIsPublicPrice());

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/preview")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-preview"))
                .andExpect(model().attribute("product", hasProperty(
                        "productName",
                        equalTo("BF03 Elite Bonsai")
                )));
    }

    @Test
    void tcSysBF03005_artisanCanPublishReadyDraftProduct() throws Exception {

        Product product = createDraftProductWithMedia();

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"))
                .andExpect(flash().attributeExists("success"));

        Product publishedProduct = productRepository.findById(product.getProductId()).orElseThrow();

        assertEquals("AVAILABLE", publishedProduct.getProductStatus());
        assertEquals(Boolean.TRUE, publishedProduct.getIsVisible());
    }

    @Test
    void tcSysBF03006_customerCanSeePublishedProductInMarketplace() throws Exception {

        Product product = createDraftProductWithMedia();

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Product publishedProduct = productRepository.findById(product.getProductId()).orElseThrow();

        mockMvc.perform(get("/marketplace")
                        .param("keyword", publishedProduct.getProductName()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/marketplace"))
                .andExpect(model().attributeExists("products"))
                .andExpect(content().string(containsString(publishedProduct.getProductName())));

        assertEquals("AVAILABLE", publishedProduct.getProductStatus());
        assertEquals(Boolean.TRUE, publishedProduct.getIsVisible());
    }

    @Test
    void tcSysBF03007_invalidMediaUploadIsRejected() throws Exception {

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
    void tcSysBF03008_publishIsBlockedWhenMandatoryDataOrMediaIsMissing() throws Exception {

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

    private record CatalogData(
            Category category,
            Variety variety,
            ProductSegment standardSegment,
            ProductSegment eliteSegment,
            Tag tag
    ) {
    }
}
