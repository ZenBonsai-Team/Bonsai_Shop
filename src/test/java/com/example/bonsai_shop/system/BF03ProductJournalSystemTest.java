package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductJournalEvent;
import com.example.bonsai_shop.entity.ProductJournalMedia;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import com.example.bonsai_shop.owner.service.AccountService;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductJournalEventRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
// System test cho chức năng Nhat ky cham soc cay (ProductJournalService).
class BF03ProductJournalSystemTest {

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
    private ProductJournalEventRepository journalEventRepository;

    @MockitoBean
    private CloudinaryStorageService cloudinaryStorageService;

    // ======================== HELPERS ========================

    private RequestPostProcessor artisanUser() {
        User artisan = findOrCreateArtisan();
        return user(new CustomUserDetails(
                artisan,
                List.of(new SimpleGrantedAuthority("ROLE_ARTISAN"))
        ));
    }

    private User findOrCreateArtisan() {
        Role artisanRole = findRole("ARTISAN", "ROLE_ARTISAN");
        String email = "artisan.journal@test.com";

        User artisan = accountRepository.findAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    accountService.createAccount(
                            "Journal Artisan Test",
                            email,
                            "123456",
                            "0910000099",
                            artisanRole.getRoleId()
                    );
                    return accountRepository.findAll().stream()
                            .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                            .findFirst().orElseThrow();
                });

        artisan.setStatus("ACTIVE");
        artisan.setRole(artisanRole);
        return accountRepository.save(artisan);
    }

    private Role findRole(String... roleNames) {
        return roleRepository.findAll().stream()
                .filter(role -> {
                    for (String name : roleNames) {
                        if (name.equalsIgnoreCase(role.getRoleName())) return true;
                    }
                    return false;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Required role not found"));
    }

    private Product createDraftProduct() throws Exception {
        Category category = categoryRepository.save(Category.builder().categoryName("Journal Category " + System.nanoTime()).build());
        Variety variety = varietyRepository.save(Variety.builder().category(category).varietyName("Journal Variety " + System.nanoTime()).build());
        ProductSegment segment = segmentRepository.save(ProductSegment.builder().segmentName("Standard " + System.nanoTime()).build());
        Tag tag = tagRepository.save(Tag.builder().tagName("Journal Tag " + System.nanoTime()).build());
        String productName = "Journal Draft Bonsai " + System.nanoTime();

        mockMvc.perform(post("/artisan/products")
                        .with(artisanUser())
                        .with(csrf())
                        .param("productName", productName)
                        .param("varietyId", String.valueOf(variety.getVarietyId()))
                        .param("segmentId", String.valueOf(segment.getSegmentId()))
                        .param("description", "Journal product description")
                        .param("treeStory", "Journal tree story")
                        .param("age", "5")
                        .param("height", "40.0")
                        .param("trunkDiameter", "6.0")
                        .param("style", "Informal Upright")
                        .param("price", "1200000")
                        .param("tagIds", String.valueOf(tag.getTagId())))
                .andExpect(status().is3xxRedirection());

        return productRepository.findAll().stream()
                .filter(p -> productName.equals(p.getProductName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Draft product was not created"));
    }

    private void mockImageUpload() {
        when(cloudinaryStorageService.uploadImage(any(), any()))
                .thenReturn(new CloudinaryUploadResponse(
                        "https://res.cloudinary.com/test/image/upload/journal-test.jpg",
                        "journal-test",
                        "image"
                ));
    }

    private MockMultipartFile validJournalImage(String name) {
        return new MockMultipartFile("files", name + ".jpg", "image/jpeg",
                ("valid journal image " + name).getBytes());
    }

    private ProductJournalEvent createJournalEvent(Product product) {
        User artisan = findOrCreateArtisan();
        ProductJournalEvent event = ProductJournalEvent.builder()
                .product(product)
                .createdBy(artisan)
                .eventDate(LocalDate.now())
                .eventType("GROWTH")
                .title("Initial Growth Update")
                .description("Growth notes")
                .isPublic(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .mediaList(new ArrayList<>())
                .build();
        return journalEventRepository.save(event);
    }

    private ProductJournalMedia addMediaToEvent(ProductJournalEvent event) {
        ProductJournalMedia media = ProductJournalMedia.builder()
                .event(event)
                .mediaUrl("https://res.cloudinary.com/test/image/upload/journal-media.jpg")
                .mediaType("IMAGE")
                .displayOrder(0)
                .build();
        event.getMediaList().add(media);
        // saveAndFlush ensures media ID is generated before we read it back
        ProductJournalEvent saved = journalEventRepository.saveAndFlush(event);
        // Reload the event from DB to get the persisted media with generated IDs
        ProductJournalEvent reloaded = journalEventRepository.findById(saved.getEventId())
                .orElseThrow(() -> new AssertionError("Event not found after save"));
        return reloaded.getMediaList().stream()
                .filter(m -> "https://res.cloudinary.com/test/image/upload/journal-media.jpg".equals(m.getMediaUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Journal media was not persisted"));
    }

    // ======================== TESTS ========================

    @Test
    void tcSysBF03J001_artisanCanViewJournalPage() throws Exception {
        // TC: Artisan mo trang nhat ky san pham thanh cong
        Product product = createDraftProduct();

        mockMvc.perform(get("/artisan/products/" + product.getProductId() + "/journal")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/product-journal"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("journalEvents"))
                .andExpect(model().attributeExists("today"));
    }

    @Test
    void tcSysBF03J002_artisanCanAddJournalEvent() throws Exception {
        // TC: Artisan them su kien nhat ky thanh cong voi 3 anh tro len
        mockImageUpload();
        Product product = createDraftProduct();

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal")
                        .file(validJournalImage("img1"))
                        .file(validJournalImage("img2"))
                        .file(validJournalImage("img3"))
                        .with(artisanUser())
                        .with(csrf())
                        .param("eventType", "GROWTH")
                        .param("title", "Spring growth update")
                        .param("description", "Trees are growing well")
                        .param("isPublic", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("success"));

        List<ProductJournalEvent> events = journalEventRepository.findAll().stream()
                .filter(e -> e.getProduct().getProductId().equals(product.getProductId()))
                .toList();
        assertFalse(events.isEmpty());
        assertTrue(events.get(0).getIsPublic());
    }

    @Test
    void tcSysBF03J003_addEventFailsWithLessThan3Images() throws Exception {
        // TC: Them su kien that bai khi gui duoi 3 anh
        mockImageUpload();
        Product product = createDraftProduct();

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal")
                        .file(validJournalImage("img1"))
                        .file(validJournalImage("img2"))
                        .with(artisanUser())
                        .with(csrf())
                        .param("title", "Not enough images")
                        .param("isPublic", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF03J004_addEventFailsWithBlankTitle() throws Exception {
        // TC: Them su kien that bai khi tieu de trong
        mockImageUpload();
        Product product = createDraftProduct();

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal")
                        .file(validJournalImage("img1"))
                        .file(validJournalImage("img2"))
                        .file(validJournalImage("img3"))
                        .with(artisanUser())
                        .with(csrf())
                        .param("title", "")
                        .param("isPublic", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF03J005_addEventFailsWithVideoFile() throws Exception {
        // TC: Them su kien that bai khi upload file video (khong phai anh)
        Product product = createDraftProduct();
        MockMultipartFile videoFile = new MockMultipartFile("files", "test.mp4", "video/mp4", "video content".getBytes());

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal")
                        .file(videoFile)
                        .file(videoFile)
                        .file(videoFile)
                        .with(artisanUser())
                        .with(csrf())
                        .param("title", "Video upload attempt")
                        .param("isPublic", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF03J006_artisanCanUpdateEventText() throws Exception {
        // TC: Artisan cap nhat tieu de va mo ta cua su kien nhat ky
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId())
                        .with(artisanUser())
                        .with(csrf())
                        .param("title", "Updated Title")
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("success"));

        ProductJournalEvent updated = journalEventRepository.findById(event.getEventId()).orElseThrow();
        assertEquals("Updated Title", updated.getTitle());
    }

    @Test
    void tcSysBF03J007_updateEventTextFailsWithBlankTitle() throws Exception {
        // TC: Cap nhat that bai khi tieu de trong
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId())
                        .with(artisanUser())
                        .with(csrf())
                        .param("title", "")
                        .param("description", "Still valid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF03J008_artisanCanToggleEventVisibility() throws Exception {
        // TC: Artisan bat/tat hien thi cong khai cua su kien nhat ky
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId() + "/visibility")
                        .with(artisanUser())
                        .with(csrf())
                        .param("isPublic", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("success"));

        assertTrue(journalEventRepository.findById(event.getEventId()).orElseThrow().getIsPublic());

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId() + "/visibility")
                        .with(artisanUser())
                        .with(csrf())
                        .param("isPublic", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("success"));

        assertFalse(journalEventRepository.findById(event.getEventId()).orElseThrow().getIsPublic());
    }

    @Test
    void tcSysBF03J009_artisanCanDeleteJournalEvent() throws Exception {
        // TC: Artisan xoa su kien nhat ky thanh cong
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);
        Integer eventId = event.getEventId();

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + eventId + "/delete")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("success"));

        assertFalse(journalEventRepository.existsById(eventId));
    }

    @Test
    void tcSysBF03J010_deleteEventFailsForNonExistentEvent() throws Exception {
        // TC: Xoa su kien that bai khi eventId khong ton tai
        Product product = createDraftProduct();

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/99999/delete")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF03J011_artisanCanAddMediaToExistingEvent() throws Exception {
        // TC: Artisan bo sung anh vao su kien nhat ky da ton tai
        mockImageUpload();
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId() + "/media")
                        .file(validJournalImage("extra1"))
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void tcSysBF03J012_addMediaFailsWithNoFiles() throws Exception {
        // TC: Bo sung anh that bai khi khong chon file nao
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/" + event.getEventId() + "/media")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF03J013_artisanCanSetCoverMedia() throws Exception {
        // TC: Artisan dat anh dai dien cho su kien nhat ky
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);
        ProductJournalMedia media = addMediaToEvent(event);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/"
                        + event.getEventId() + "/media/" + media.getMediaId() + "/cover")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void tcSysBF03J014_setCoverFailsForNonExistentMedia() throws Exception {
        // TC: Dat anh dai dien that bai khi mediaId khong ton tai
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/"
                        + event.getEventId() + "/media/99999/cover")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void tcSysBF03J015_artisanCanDeleteJournalMedia() throws Exception {
        // TC: Artisan xoa anh khoi su kien nhat ky thanh cong
        Product product = createDraftProduct();
        ProductJournalEvent event = createJournalEvent(product);
        ProductJournalMedia media = addMediaToEvent(event);

        mockMvc.perform(post("/artisan/products/" + product.getProductId() + "/journal/"
                        + event.getEventId() + "/media/" + media.getMediaId() + "/delete")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/products/" + product.getProductId() + "/journal"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void tcSysBF03J016_addEventWithAllEventTypesNormalizedCorrectly() throws Exception {
        // TC: He thong chap nhan tat ca loai event type hop le va normalize ve PHOTO_UPDATE cho type khong hop le
        mockImageUpload();
        Product product = createDraftProduct();

        String[] validTypes = {"GROWTH", "PRUNING", "WIRING", "REPOTTING", "FERTILIZING",
                "WATERING", "PEST_TREATMENT", "HEALTH_CHECK", "ACQUISITION", "PHOTO_UPDATE"};

        for (String eventType : validTypes) {
            mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal")
                            .file(validJournalImage("i1"))
                            .file(validJournalImage("i2"))
                            .file(validJournalImage("i3"))
                            .with(artisanUser())
                            .with(csrf())
                            .param("eventType", eventType)
                            .param("title", "Event " + eventType)
                            .param("isPublic", "false"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("success"));
        }

        // Type khong hop le -> normalize ve PHOTO_UPDATE, van thanh cong
        mockMvc.perform(multipart("/artisan/products/" + product.getProductId() + "/journal")
                        .file(validJournalImage("i1"))
                        .file(validJournalImage("i2"))
                        .file(validJournalImage("i3"))
                        .with(artisanUser())
                        .with(csrf())
                        .param("eventType", "INVALID_TYPE")
                        .param("title", "Unknown event type test")
                        .param("isPublic", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        long count = journalEventRepository.findAll().stream()
                .filter(e -> e.getProduct().getProductId().equals(product.getProductId()))
                .count();
        assertEquals(11, count);
    }
}
