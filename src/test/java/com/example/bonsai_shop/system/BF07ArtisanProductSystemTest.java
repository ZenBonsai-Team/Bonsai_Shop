package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.*;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import com.example.bonsai_shop.owner.service.AccountService;
import com.example.bonsai_shop.product.repository.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
// System test tang coverage cho ArtisanProductService: update, delete, hide, show, media ops, validation.
class BF07ArtisanProductSystemTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountService accountService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VarietyRepository varietyRepository;
    @Autowired private ProductSegmentRepository segmentRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductMediaRepository productMediaRepository;

    @MockitoBean
    private CloudinaryStorageService cloudinaryStorageService;

    // ======================== HELPERS ========================

    private RequestPostProcessor artisanUser() {
        User artisan = findOrCreateArtisan();
        return user(new CustomUserDetails(artisan,
                List.of(new SimpleGrantedAuthority("ROLE_ARTISAN"))));
    }

    private User findOrCreateArtisan() {
        Role role = findRole("ARTISAN", "ROLE_ARTISAN");
        String email = "artisan.product7@test.com";
        User artisan = accountRepository.findAll().stream()
                .filter(item -> email.equalsIgnoreCase(item.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    accountService.createAccount("BF07 Artisan", email, "123456", "0910000077", role.getRoleId());
                    return accountRepository.findAll().stream()
                            .filter(item -> email.equalsIgnoreCase(item.getEmail()))
                            .findFirst().orElseThrow();
                });
        artisan.setStatus("ACTIVE");
        artisan.setRole(role);
        return accountRepository.save(artisan);
    }

    private Role findRole(String... names) {
        return roleRepository.findAll().stream()
                .filter(r -> { for (String n : names) if (n.equalsIgnoreCase(r.getRoleName())) return true; return false; })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Role not found"));
    }

    private Category createCategory() {
        return categoryRepository.save(Category.builder()
                .categoryName("BF07 Cat " + System.nanoTime()).build());
    }

    private Variety createVariety(Category cat) {
        return varietyRepository.save(Variety.builder()
                .category(cat)
                .varietyName("BF07 Var " + System.nanoTime()).build());
    }

    private ProductSegment createSegment() {
        return segmentRepository.save(ProductSegment.builder()
                .segmentName("Standard " + System.nanoTime()).build());
    }

    private Tag createTag() {
        return tagRepository.save(Tag.builder()
                .tagName("BF07 Tag " + System.nanoTime()).build());
    }

    private void mockImageUpload(String url) {
        when(cloudinaryStorageService.uploadImage(any(), any()))
                .thenReturn(new CloudinaryUploadResponse(url, "public-id", "image"));
    }

    // Tao san pham DRAFT qua controller — controller redirect sang /media khi thanh cong
    private Product createDraftProduct() throws Exception {
        Category cat = createCategory();
        Variety var = createVariety(cat);
        ProductSegment seg = createSegment();
        Tag tag = createTag();
        String name = "BF07 Bonsai " + System.nanoTime();

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser()).with(csrf())
                        .param("productName", name)
                        .param("varietyId", String.valueOf(var.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("description", "Mo ta test BF07")
                        .param("treeStory", "Lich su cay test BF07")
                        .param("age", "5")
                        .param("height", "40.0")
                        .param("trunkDiameter", "6.0")
                        .param("style", "Informal Upright")
                        .param("price", "1500000")
                        .param("tagIds", String.valueOf(tag.getTagId())))
                .andExpect(status().is3xxRedirection());

        return productRepository.findAll().stream()
                .filter(p -> name.equals(p.getProductName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Draft product not created"));
    }

    // Them 1 anh vao san pham va tra ve ProductMedia
    // Controller nhan: files (List), slotTypes (List), thumbnailIndex (Integer)
    private ProductMedia addImageToProduct(Product product, String url) throws Exception {
        mockImageUpload(url);
        // Ten param la 'files' (List<MultipartFile>) theo controller
        MockMultipartFile img = new MockMultipartFile("files", "test.jpg", "image/jpeg", "data".getBytes());

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/media")
                        .file(img)
                        .with(artisanUser()).with(csrf())
                        .param("slotTypes", "FRONT")
                        .param("thumbnailIndex", "0"))
                .andExpect(status().is3xxRedirection());

        return productMediaRepository.findAll().stream()
                .filter(m -> url.equals(m.getMediaUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Media not saved: " + url));
    }

    // ======================== TESTS ========================

    // --- MY PRODUCTS / EDIT FORM ---

    @Test
    void tcSysBF07001_artisanCanViewMyProductsList() throws Exception {
        // TC: Artisan mo trang danh sach san pham cua minh
        // View name la "artisan/products" (co 's' cuoi) theo controller
        mockMvc.perform(get("/artisan/products").with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/products"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    void tcSysBF07002_artisanCanOpenCreateProductForm() throws Exception {
        // TC: Artisan mo form tao moi san pham
        mockMvc.perform(get("/artisan/products/new").with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("varieties"))
                .andExpect(model().attributeExists("segments"))
                .andExpect(model().attributeExists("tags"));
    }

    @Test
    void tcSysBF07003_artisanCanOpenEditFormForOwnProduct() throws Exception {
        // TC: Artisan mo form chinh sua san pham cua minh
        Product product = createDraftProduct();

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/edit")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("varieties"));
    }

    // --- UPDATE PRODUCT ---

    @Test
    void tcSysBF07004_artisanCanUpdateDraftProduct() throws Exception {
        // TC: Artisan cap nhat thong tin san pham DRAFT thanh cong
        // Controller redirect sang /preview khi thanh cong
        Product product = createDraftProduct();
        Category cat2 = createCategory();
        Variety var2 = createVariety(cat2);
        ProductSegment seg2 = createSegment();

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .with(artisanUser()).with(csrf())
                        .param("productName", "Updated Bonsai Name")
                        .param("varietyId", String.valueOf(var2.getVarietyId()))
                        .param("segmentId", String.valueOf(seg2.getSegmentId()))
                        .param("description", "Updated description")
                        .param("treeStory", "Updated story")
                        .param("age", "10")
                        .param("height", "60.0")
                        .param("trunkDiameter", "8.0")
                        .param("style", "Slant")
                        .param("price", "3000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/preview"));

        Product updated = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("Updated Bonsai Name", updated.getProductName());
        assertEquals(10, updated.getAge());
    }

    @Test
    void tcSysBF07005_updateProductFailsWithInvalidAge() throws Exception {
        // TC: Cap nhat that bai khi tuoi cay am — BindingResult tra ve form (200) voi hasErrors
        Product product = createDraftProduct();
        Variety vari = product.getVariety();
        ProductSegment seg = product.getSegment();

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .with(artisanUser()).with(csrf())
                        .param("productName", "Test")
                        .param("varietyId", String.valueOf(vari.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("age", "-1")   // invalid: @Min(1)
                        .param("height", "30.0")
                        .param("trunkDiameter", "5.0")
                        .param("style", "Upright")
                        .param("price", "1000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void tcSysBF07006_updateProductFailsWithZeroPrice() throws Exception {
        // TC: Cap nhat that bai khi gia bang 0 — BindingResult @DecimalMin(0.01) tra ve form (200)
        Product product = createDraftProduct();
        Variety vari = product.getVariety();
        ProductSegment seg = product.getSegment();

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .with(artisanUser()).with(csrf())
                        .param("productName", "Test")
                        .param("varietyId", String.valueOf(vari.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("age", "5")
                        .param("height", "30.0")
                        .param("trunkDiameter", "5.0")
                        .param("style", "Upright")
                        .param("price", "0"))  // invalid: @DecimalMin(0.01)
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void tcSysBF07007_updateProductFailsWithInvalidStyle() throws Exception {
        // TC: Cap nhat that bai khi dang cay co ky tu dac biet — BindingResult @Pattern tra ve form (200)
        Product product = createDraftProduct();
        Variety vari = product.getVariety();
        ProductSegment seg = product.getSegment();

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .with(artisanUser()).with(csrf())
                        .param("productName", "Test")
                        .param("varietyId", String.valueOf(vari.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("age", "5")
                        .param("height", "30.0")
                        .param("trunkDiameter", "5.0")
                        .param("style", "Style@123!")  // invalid: @Pattern(^[\p{L}\s'-]+$)
                        .param("price", "1000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().hasErrors());
    }

    // --- DELETE PRODUCT ---

    @Test
    void tcSysBF07008_artisanCanDeleteDraftProduct() throws Exception {
        // TC: Artisan xoa san pham DRAFT thanh cong — redirect /artisan/products
        Product product = createDraftProduct();
        Integer productId = product.getProductId();

        mockMvc.perform(post("/artisan/products/" + productId + "/delete")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products"));

        assertFalse(productRepository.findById(productId).isPresent());
    }

    @Test
    void tcSysBF07009_deleteFailsForNonExistentProduct() throws Exception {
        // TC: Xoa san pham khong ton tai tra ve loi redirect voi flash error
        mockMvc.perform(post("/artisan/products/99999/delete")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    // --- PUBLISH / HIDE / SHOW ---

    @Test
    void tcSysBF07010_publishFailsWithoutMedia() throws Exception {
        // TC: Publish that bai khi san pham khong co media — redirect voi flash error
        Product product = createDraftProduct();

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        Product unchanged = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("DRAFT", unchanged.getProductStatus());
    }

    @Test
    void tcSysBF07011_artisanCanHideAvailableProduct() throws Exception {
        // TC: Artisan an san pham dang ban thanh cong
        Product product = createDraftProduct();

        String imgUrl = "https://res.cloudinary.com/test/image/upload/bf07-hide.jpg";
        addImageToProduct(product, imgUrl);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        Product published = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("AVAILABLE", published.getProductStatus());

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/hide")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Product hidden = productRepository.findById(product.getProductId()).orElseThrow();
        assertFalse(hidden.getIsVisible());
    }

    @Test
    void tcSysBF07012_hideFailsForDraftProduct() throws Exception {
        // TC: An san pham DRAFT phai that bai — redirect voi flash error
        Product product = createDraftProduct();

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/hide")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF07013_artisanCanShowHiddenProduct() throws Exception {
        // TC: Artisan hien lai san pham da an thanh cong
        Product product = createDraftProduct();

        String imgUrl = "https://res.cloudinary.com/test/image/upload/bf07-show.jpg";
        addImageToProduct(product, imgUrl);

        // Publish
        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Hide
        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/hide")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Show again
        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/show")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Product shown = productRepository.findById(product.getProductId()).orElseThrow();
        assertTrue(shown.getIsVisible());
    }

    @Test
    void tcSysBF07014_showFailsForDraftProduct() throws Exception {
        // TC: Hien san pham DRAFT phai that bai — redirect voi flash error
        Product product = createDraftProduct();

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/show")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    // --- MEDIA OPERATIONS ---

    @Test
    void tcSysBF07015_artisanCanViewMediaPage() throws Exception {
        // TC: Artisan mo trang quan ly media cua san pham — view la "artisan/product-media"
        Product product = createDraftProduct();

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/media")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-media"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("mediaList"));
    }

    @Test
    void tcSysBF07016_artisanCanSetThumbnail() throws Exception {
        // TC: Artisan chon thumbnail cho san pham
        Product product = createDraftProduct();
        String url1 = "https://res.cloudinary.com/test/image/upload/bf07-thumb1.jpg";
        String url2 = "https://res.cloudinary.com/test/image/upload/bf07-thumb2.jpg";

        addImageToProduct(product, url1);
        ProductMedia media2 = addImageToProduct(product, url2);

        mockMvc.perform(post("/artisan/products/" + product.getProductId()
                                + "/media/" + media2.getMediaId() + "/thumbnail")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        ProductMedia updated = productMediaRepository.findById(media2.getMediaId()).orElseThrow();
        assertTrue(updated.getIsThumbnail());
    }

    @Test
    void tcSysBF07017_setThumbnailFailsForNonExistentMedia() throws Exception {
        // TC: Set thumbnail that bai khi mediaId khong ton tai — flash error
        Product product = createDraftProduct();

        mockMvc.perform(post("/artisan/products/" + product.getProductId()
                                + "/media/99999/thumbnail")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF07018_artisanCanDeleteMedia() throws Exception {
        // TC: Artisan xoa media cua san pham
        Product product = createDraftProduct();
        String imgUrl = "https://res.cloudinary.com/test/image/upload/bf07-del.jpg";
        ProductMedia media = addImageToProduct(product, imgUrl);
        Integer mediaId = media.getMediaId();

        // deleteFile returns void — use doNothing() for void mock
        org.mockito.Mockito.doNothing().when(cloudinaryStorageService).deleteFile(any(), any());

        mockMvc.perform(post("/artisan/products/" + product.getProductId()
                                + "/media/" + mediaId + "/delete")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        assertFalse(productMediaRepository.findById(mediaId).isPresent());
    }

    @Test
    void tcSysBF07019_artisanCanUpdateMediaOrder() throws Exception {
        // TC: Artisan cap nhat thu tu media thanh cong
        Product product = createDraftProduct();
        String url1 = "https://res.cloudinary.com/test/image/upload/bf07-ord1.jpg";
        String url2 = "https://res.cloudinary.com/test/image/upload/bf07-ord2.jpg";
        ProductMedia m1 = addImageToProduct(product, url1);
        ProductMedia m2 = addImageToProduct(product, url2);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/media/order")
                        .with(artisanUser()).with(csrf())
                        .param("mediaIds", String.valueOf(m1.getMediaId()), String.valueOf(m2.getMediaId()))
                        .param("displayOrders", "2", "1")
                        .param("slotTypes", "BACK", "FRONT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        ProductMedia reloadedM1 = productMediaRepository.findById(m1.getMediaId()).orElseThrow();
        assertEquals(2, reloadedM1.getDisplayOrder());
    }

    @Test
    void tcSysBF07020_addMediaFailsWithInvalidShotType() throws Exception {
        // TC: Upload anh that bai khi shot type khong hop le — flash error
        Product product = createDraftProduct();

        mockImageUpload("https://res.cloudinary.com/test/image/upload/bf07-invalid.jpg");
        MockMultipartFile img = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/media")
                        .file(img)
                        .with(artisanUser()).with(csrf())
                        .param("slotType", "INVALID_SHOT")  // not in VALID_SHOT_TYPES
                        .param("isThumbnail", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    // --- VALIDATION ON CREATE ---

    @Test
    void tcSysBF07021_createProductFailsWithNegativeHeight() throws Exception {
        // TC: Tao san pham that bai khi chieu cao am — BindingResult @DecimalMin(0.01) tra ve form (200)
        Category cat = createCategory();
        Variety vari = createVariety(cat);
        ProductSegment seg = createSegment();

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser()).with(csrf())
                        .param("productName", "Test Negative Height")
                        .param("varietyId", String.valueOf(vari.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("age", "5")
                        .param("height", "-10.0")  // invalid: @DecimalMin(0.01)
                        .param("trunkDiameter", "5.0")
                        .param("style", "Upright")
                        .param("price", "1000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void tcSysBF07022_createProductFailsWithNegativeTrunkDiameter() throws Exception {
        // TC: Tao san pham that bai khi duong kinh than cay am — BindingResult @DecimalMin(0.01) tra ve form (200)
        Category cat = createCategory();
        Variety vari = createVariety(cat);
        ProductSegment seg = createSegment();

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser()).with(csrf())
                        .param("productName", "Test Negative Trunk")
                        .param("varietyId", String.valueOf(vari.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("age", "5")
                        .param("height", "40.0")
                        .param("trunkDiameter", "-3.0")  // invalid: @DecimalMin(0.01)
                        .param("style", "Upright")
                        .param("price", "1000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void tcSysBF07023_createProductFailsWithDecimalPrice() throws Exception {
        // TC: Tao san pham that bai khi gia co phan thap phan — BindingResult @Digits(fraction=0) tra ve form (200)
        Category cat = createCategory();
        Variety vari = createVariety(cat);
        ProductSegment seg = createSegment();

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser()).with(csrf())
                        .param("productName", "Test Decimal Price")
                        .param("varietyId", String.valueOf(vari.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("age", "5")
                        .param("height", "40.0")
                        .param("trunkDiameter", "5.0")
                        .param("style", "Upright")
                        .param("price", "1500000.50"))  // invalid: @Digits(fraction=0)
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void tcSysBF07024_createProductFailsWhenAgeExceeds1000() throws Exception {
        // TC: Tao san pham that bai khi tuoi cay vuot qua 1000 — BindingResult @Max(1000) tra ve form (200)
        Category cat = createCategory();
        Variety vari = createVariety(cat);
        ProductSegment seg = createSegment();

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser()).with(csrf())
                        .param("productName", "Test Age Too Old")
                        .param("varietyId", String.valueOf(vari.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("age", "1001")  // invalid: @Max(1000)
                        .param("height", "40.0")
                        .param("trunkDiameter", "5.0")
                        .param("style", "Upright")
                        .param("price", "1000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void tcSysBF07025_createProductWithMultipleTags() throws Exception {
        // TC: Tao san pham thanh cong voi nhieu tag — redirect sang /media
        Category cat = createCategory();
        Variety var = createVariety(cat);
        ProductSegment seg = createSegment();
        Tag tag1 = createTag();
        Tag tag2 = createTag();
        Tag tag3 = createTag();
        String name = "Multi-Tag Bonsai " + System.nanoTime();

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser()).with(csrf())
                        .param("productName", name)
                        .param("varietyId", String.valueOf(var.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("description", "Description")
                        .param("treeStory", "Story")
                        .param("age", "3")
                        .param("height", "25.0")
                        .param("trunkDiameter", "4.0")
                        .param("style", "Cascade")
                        .param("price", "2000000")
                        .param("tagIds", String.valueOf(tag1.getTagId()),
                                String.valueOf(tag2.getTagId()),
                                String.valueOf(tag3.getTagId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/media"));

        Product saved = productRepository.findAll().stream()
                .filter(p -> name.equals(p.getProductName())).findFirst().orElseThrow();
        assertEquals("DRAFT", saved.getProductStatus());
    }

    @Test
    void tcSysBF07026_previewPageShowsProductData() throws Exception {
        // TC: Artisan xem trang preview san pham — view la "artisan/product-preview"
        Product product = createDraftProduct();

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/preview")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-preview"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    void tcSysBF07027_publishSucceedsWhenProductHasImage() throws Exception {
        // TC: Publish thanh cong khi san pham da co anh — redirect /artisan/products
        Product product = createDraftProduct();
        String imgUrl = "https://res.cloudinary.com/test/image/upload/bf07-pub.jpg";
        addImageToProduct(product, imgUrl);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/publish")
                        .with(artisanUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        Product published = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals("AVAILABLE", published.getProductStatus());
        assertTrue(published.getIsVisible());
    }

    @Test
    void tcSysBF07028_updateTagsSyncsCorrectly() throws Exception {
        // TC: Cap nhat tag san pham xoa tag cu, them tag moi — redirect /preview
        Product product = createDraftProduct();
        Variety var = product.getVariety();
        ProductSegment seg = product.getSegment();
        Tag newTag = createTag();

        mockMvc.perform(post("/artisan/products/" + product.getProductId())
                        .with(artisanUser()).with(csrf())
                        .param("productName", product.getProductName())
                        .param("varietyId", String.valueOf(var.getVarietyId()))
                        .param("segmentId", String.valueOf(seg.getSegmentId()))
                        .param("description", "Desc")
                        .param("treeStory", "Story")
                        .param("age", "5")
                        .param("height", "40.0")
                        .param("trunkDiameter", "6.0")
                        .param("style", "Informal Upright")
                        .param("price", "1500000")
                        .param("tagIds", String.valueOf(newTag.getTagId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/artisan/products/*/preview"));
    }
}
