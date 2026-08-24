package com.example.bonsai_shop.artisan1.controller;

import com.example.bonsai_shop.artisan.controller.ArtisanProductController;
import com.example.bonsai_shop.artisan.dto.ArtisanProductFormDTO;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.artisan.service.ProductJournalService;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductJournalEvent;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ArtisanProductControllerIntegrationTest {

        private ArtisanProductService artisanProductService;
        private ProductJournalService productJournalService;
        private CloudinaryStorageService cloudinaryStorageService;
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                artisanProductService = mock(ArtisanProductService.class);
                productJournalService = mock(ProductJournalService.class);
                cloudinaryStorageService = mock(CloudinaryStorageService.class);
                ArtisanProductController controller = new ArtisanProductController(artisanProductService,
                                productJournalService, cloudinaryStorageService);
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                                .build();

                UserDetails userDetails = org.springframework.security.core.userdetails.User
                                .withUsername("artisan@test.com")
                                .password("password")
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ARTISAN")))
                                .build();
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void myProducts_WhenArtisanRequestsProducts_ShouldDisplayProductList() throws Exception {
                List<Product> products = List.of(product(101), product(102));
                when(artisanProductService.getMyProducts("artisan@test.com"))
                                .thenReturn(products);

                mockMvc.perform(get("/artisan/products"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/products"))
                                .andExpect(model().attribute("products", products));

                verify(artisanProductService).getMyProducts("artisan@test.com");
        }

        @Test
        void createForm_WhenArtisanRequestsNewProductForm_ShouldDisplayLookupData() throws Exception {
                mockLookupData();

                mockMvc.perform(get("/artisan/products/new"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-form"))
                                .andExpect(model().attributeExists("productForm", "categories", "varieties", "segments",
                                                "tags"))
                                .andExpect(model().attribute("product", org.hamcrest.Matchers.nullValue()));
        }

        @Test
        void create_WhenFormIsValid_ShouldCreateProductAndRedirectToMediaPage() throws Exception {
                Product savedProduct = product(101);
                when(artisanProductService.createProduct(any(String.class), any(ArtisanProductFormDTO.class)))
                                .thenReturn(savedProduct);

                mockMvc.perform(validProductPost("/artisan/products"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/media"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).createProduct(any(String.class), any(ArtisanProductFormDTO.class));
        }

        @Test
        void create_WhenFormIsInvalid_ShouldReturnFormViewWithBindingErrorsAndLookupData() throws Exception {
                mockLookupData();

                mockMvc.perform(post("/artisan/products")
                                .param("productName", "")
                                .param("style", "Style_123")
                                .param("age", "-1")
                                .param("height", "0")
                                .param("trunkDiameter", "0")
                                .param("price", "-1000"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-form"))
                                .andExpect(model().attributeHasFieldErrors(
                                                "productForm",
                                                "varietyId",
                                                "segmentId",
                                                "productName",
                                                "age",
                                                "height",
                                                "trunkDiameter",
                                                "style",
                                                "price"))
                                .andExpect(model().attributeExists("categories", "varieties", "segments", "tags"));

                verify(artisanProductService, never()).createProduct(any(String.class),
                                any(ArtisanProductFormDTO.class));
        }

        @Test
        void create_WhenServiceRejectsValidForm_ShouldReturnFormViewWithError() throws Exception {
                mockLookupData();
                when(artisanProductService.createProduct(any(String.class), any(ArtisanProductFormDTO.class)))
                                .thenThrow(new RuntimeException("Business validation failed"));

                mockMvc.perform(validProductPost("/artisan/products"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-form"))
                                .andExpect(model().attribute("error", "Business validation failed"))
                                .andExpect(model().attributeExists("productForm", "categories", "varieties", "segments",
                                                "tags"));
        }

        @Test
        void editForm_WhenProductIsEditable_ShouldDisplayPopulatedForm() throws Exception {
                Product product = product(101);
                ArtisanProductFormDTO form = validForm();
                mockLookupData();
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);
                when(artisanProductService.isEditable(product))
                                .thenReturn(true);
                when(artisanProductService.toFormDTO(product))
                                .thenReturn(form);

                mockMvc.perform(get("/artisan/products/101/edit"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-form"))
                                .andExpect(model().attribute("product", product))
                                .andExpect(model().attribute("productForm", form))
                                .andExpect(model().attributeExists("categories", "varieties", "segments", "tags"));
        }

        @Test
        void editForm_WhenProductIsNotEditable_ShouldRedirectToPreviewWithError() throws Exception {
                Product product = product(101);
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);
                when(artisanProductService.isEditable(product))
                                .thenReturn(false);

                mockMvc.perform(get("/artisan/products/101/edit"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/preview"))
                                .andExpect(flash().attributeExists("error"));
        }

        @Test
        void update_WhenFormIsValid_ShouldUpdateProductAndRedirectToPreview() throws Exception {
                Product product = product(101);
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);

                mockMvc.perform(validProductPost("/artisan/products/101"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/preview"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).updateProduct(any(String.class), any(Integer.class),
                                any(ArtisanProductFormDTO.class));
        }

        @Test
        void update_WhenFormIsInvalid_ShouldReturnFormViewWithBindingErrorsAndLookupData() throws Exception {
                Product product = product(101);
                mockLookupData();
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);

                mockMvc.perform(post("/artisan/products/101")
                                .param("productName", "")
                                .param("style", "Style_123")
                                .param("age", "-1")
                                .param("height", "0")
                                .param("trunkDiameter", "0")
                                .param("price", "-1000"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-form"))
                                .andExpect(model().attribute("product", product))
                                .andExpect(model().attributeHasFieldErrors(
                                                "productForm",
                                                "varietyId",
                                                "segmentId",
                                                "productName",
                                                "age",
                                                "height",
                                                "trunkDiameter",
                                                "style",
                                                "price"))
                                .andExpect(model().attributeExists("categories", "varieties", "segments", "tags"));

                verify(artisanProductService, never()).updateProduct(any(String.class), any(Integer.class),
                                any(ArtisanProductFormDTO.class));
        }

        @Test
        void update_WhenServiceRejectsValidForm_ShouldReturnFormViewWithError() throws Exception {
                Product product = product(101);
                mockLookupData();
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);
                doThrow(new RuntimeException("Update violates business rule"))
                                .when(artisanProductService)
                                .updateProduct(any(String.class), any(Integer.class), any(ArtisanProductFormDTO.class));

                mockMvc.perform(validProductPost("/artisan/products/101"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-form"))
                                .andExpect(model().attribute("error", "Update violates business rule"))
                                .andExpect(model().attributeExists("productForm", "categories", "varieties", "segments",
                                                "tags"));
        }

        @Test
        void delete_WhenServiceDeletesProductSuccessfully_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/delete"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).deleteProduct("artisan@test.com", 101);
        }

        @Test
        void delete_WhenServiceRejectsDeletion_ShouldRedirectWithError() throws Exception {
                doThrow(new RuntimeException("Cannot delete product"))
                                .when(artisanProductService).deleteProduct("artisan@test.com", 101);

                mockMvc.perform(post("/artisan/products/101/delete"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products"))
                                .andExpect(flash().attribute("error", "Cannot delete product"));
        }

        @Test
        void mediaForm_WhenProductHasMedia_ShouldDisplayMediaManagementPage() throws Exception {
                Product product = product(101);
                List<ProductMedia> mediaList = List.of(media(201, "IMAGE", true));
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);
                when(artisanProductService.getMedia(product))
                                .thenReturn(mediaList);
                when(artisanProductService.isSold(product))
                                .thenReturn(false);
                when(artisanProductService.isEditable(product))
                                .thenReturn(true);

                mockMvc.perform(get("/artisan/products/101/media"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-media"))
                                .andExpect(model().attribute("product", product))
                                .andExpect(model().attribute("mediaList", mediaList))
                                .andExpect(model().attribute("isSold", false))
                                .andExpect(model().attribute("isEditable", true));
        }

        @Test
        void addMedia_WhenUploadBatchIsValid_ShouldRedirectWithSuccess() throws Exception {
                when(artisanProductService.addMediaBatch(eq("artisan@test.com"), eq(101), anyList(), anyList(),
                                anyList(), anyList(), eq(0)))
                                .thenReturn(2);

                mockMvc.perform(multipart("/artisan/products/101/media")
                                .file("files", "image-1".getBytes())
                                .file("files", "image-2".getBytes())
                                .param("mediaTypes", "IMAGE", "IMAGE")
                                .param("slotTypes", "FRONT", "BACK")
                                .param("captions", "Front", "Back")
                                .param("thumbnailIndex", "0"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/media"))
                                .andExpect(flash().attributeExists("success"));
        }

        @Test
        void addMedia_WhenUploadBatchInvalid_ShouldRedirectWithError() throws Exception {
                when(artisanProductService.addMediaBatch(eq("artisan@test.com"), eq(101), anyList(), anyList(),
                                anyList(), anyList(), eq(0)))
                                .thenThrow(new RuntimeException("Invalid media"));

                mockMvc.perform(multipart("/artisan/products/101/media")
                                .file("files", "bad-media".getBytes())
                                .param("mediaTypes", "VIDEO")
                                .param("slotTypes", "FRONT")
                                .param("captions", "Bad")
                                .param("thumbnailIndex", "0"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/media"))
                                .andExpect(flash().attribute("error", "Invalid media"));
        }

        @Test
        void setThumbnail_WhenMediaIsEligible_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/media/201/thumbnail"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/media"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).setThumbnail("artisan@test.com", 101, 201);
        }

        @Test
        void setThumbnail_WhenMediaIsIneligible_ShouldRedirectWithError() throws Exception {
                doThrow(new RuntimeException("Only image can be thumbnail"))
                                .when(artisanProductService).setThumbnail("artisan@test.com", 101, 201);

                mockMvc.perform(post("/artisan/products/101/media/201/thumbnail"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/media"))
                                .andExpect(flash().attribute("error", "Only image can be thumbnail"));
        }

        @Test
        void updateMediaOrder_WhenDataIsValid_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/media/order")
                                .param("mediaIds", "201", "202")
                                .param("displayOrders", "2", "1")
                                .param("slotTypes", "BACK", "FRONT")
                                .param("captions", "Back", "Front"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/media"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).updateMediaOrder(
                                eq("artisan@test.com"),
                                eq(101),
                                eq(List.of(201, 202)),
                                eq(List.of(2, 1)),
                                eq(List.of("BACK", "FRONT")),
                                eq(List.of("Back", "Front")));
        }

        @Test
        void updateMediaOrder_WhenDataIsInvalid_ShouldRedirectWithError() throws Exception {
                doThrow(new RuntimeException("Invalid media order"))
                                .when(artisanProductService)
                                .updateMediaOrder(any(String.class), any(Integer.class), any(), any(), any(), any());

                mockMvc.perform(post("/artisan/products/101/media/order")
                                .param("mediaIds", "201", "202")
                                .param("displayOrders", "1"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/media"))
                                .andExpect(flash().attribute("error", "Invalid media order"));
        }

        @Test
        void deleteMedia_WhenServiceDeletesSuccessfully_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/media/201/delete"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/media"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).deleteMedia("artisan@test.com", 101, 201);
        }

        @Test
        void journal_WhenProductIsNotSold_ShouldDisplayJournalPage() throws Exception {
                Product product = product(101);
                List<ProductJournalEvent> events = List.of(journalEvent(301, product));
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);
                when(artisanProductService.isSold(product))
                                .thenReturn(false);
                when(productJournalService.getMyProductEvents("artisan@test.com", 101))
                                .thenReturn(events);

                mockMvc.perform(get("/artisan/products/101/journal"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-journal"))
                                .andExpect(model().attribute("product", product))
                                .andExpect(model().attribute("journalEvents", events))
                                .andExpect(model().attributeExists("today"));
        }

        @Test
        void journal_WhenProductIsSold_ShouldRedirectToPreviewWithError() throws Exception {
                Product product = product(101);
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);
                when(artisanProductService.isSold(product))
                                .thenReturn(true);

                mockMvc.perform(get("/artisan/products/101/journal"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/preview"))
                                .andExpect(flash().attributeExists("error"));
        }

        @Test
        void addJournalEvent_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(multipart("/artisan/products/101/journal")
                                .file("files", "image-1".getBytes())
                                .file("files", "image-2".getBytes())
                                .file("files", "image-3".getBytes())
                                .param("eventType", "GROWTH")
                                .param("title", "Growth update")
                                .param("description", "New leaves")
                                .param("isPublic", "true"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/journal"))
                                .andExpect(flash().attributeExists("success"));

                verify(productJournalService).addEvent(
                                eq("artisan@test.com"),
                                eq(101),
                                any(LocalDate.class),
                                eq("GROWTH"),
                                eq("Growth update"),
                                eq("New leaves"),
                                eq(true),
                                anyList());
        }

        @Test
        void addJournalEvent_WhenServiceRejectsNoMedia_ShouldRedirectWithError() throws Exception {
                doThrow(new RuntimeException("Media required"))
                                .when(productJournalService)
                                .addEvent(any(String.class), any(Integer.class), any(LocalDate.class),
                                                any(String.class), any(String.class), any(), any(Boolean.class), any());

                mockMvc.perform(post("/artisan/products/101/journal")
                                .param("eventType", "GROWTH")
                                .param("title", "Growth update")
                                .param("description", "New leaves")
                                .param("isPublic", "true"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/journal"))
                                .andExpect(flash().attribute("error", "Media required"));
        }

        @Test
        void deleteJournalEvent_WhenServiceDeletesSuccessfully_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/journal/301/delete"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/journal"))
                                .andExpect(flash().attributeExists("success"));

                verify(productJournalService).deleteEvent("artisan@test.com", 101, 301);
        }

        @Test
        void updateJournalEventText_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/journal/301")
                                .param("title", "Updated title")
                                .param("description", "Updated description"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/journal"))
                                .andExpect(flash().attributeExists("success"));

                verify(productJournalService).updateEventText("artisan@test.com", 101, 301, "Updated title",
                                "Updated description");
        }

        @Test
        void updateJournalEventVisibility_WhenIsPublicChanges_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/journal/301/visibility")
                                .param("isPublic", "true"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/journal"))
                                .andExpect(flash().attributeExists("success"));

                verify(productJournalService).updateEventVisibility("artisan@test.com", 101, 301, true);
        }

        @Test
        void addJournalEventMedia_WhenFilesAreValid_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(multipart("/artisan/products/101/journal/301/media")
                                .file("files", "image-1".getBytes())
                                .file("files", "image-2".getBytes()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products/101/journal"))
                                .andExpect(flash().attributeExists("success"));

                verify(productJournalService).addMediaToEvent(eq("artisan@test.com"), eq(101), eq(301), anyList());
        }

        @Test
        void preview_WhenProductHasMediaAndTags_ShouldDisplayPreviewPage() throws Exception {
                Product product = product(101);
                ProductMedia thumbnail = media(201, "IMAGE", true);
                ProductMedia video = media(202, "VIDEO", false);
                List<ProductMedia> mediaList = List.of(thumbnail, video);
                List<Tag> tags = List.of(tag(100), tag(200));

                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenReturn(product);
                when(artisanProductService.getMedia(product))
                                .thenReturn(mediaList);
                when(artisanProductService.getProductTags(product))
                                .thenReturn(tags);
                when(artisanProductService.isSold(product)).thenReturn(false);
                when(artisanProductService.isEditable(product)).thenReturn(true);
                when(artisanProductService.isHideable(product)).thenReturn(true);
                when(artisanProductService.isVisible(product)).thenReturn(true);

                mockMvc.perform(get("/artisan/products/101/preview"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("artisan/product-preview"))
                                .andExpect(model().attribute("product", product))
                                .andExpect(model().attribute("mediaList", mediaList))
                                .andExpect(model().attribute("thumbnail", thumbnail))
                                .andExpect(model().attribute("tags", tags))
                                .andExpect(model().attribute("imageCount", 1L))
                                .andExpect(model().attribute("videoCount", 1L))
                                .andExpect(model().attribute("isSold", false))
                                .andExpect(model().attribute("isEditable", true))
                                .andExpect(model().attribute("isHideable", true))
                                .andExpect(model().attribute("isVisible", true));
        }

        @Test
        void publish_WhenServicePublishesSuccessfully_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/publish"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).publish("artisan@test.com", 101);
        }

        @Test
        void publish_WhenServiceRejectsPublication_ShouldRedirectWithError() throws Exception {
                doThrow(new RuntimeException("Publish prerequisites not met"))
                                .when(artisanProductService).publish("artisan@test.com", 101);

                mockMvc.perform(post("/artisan/products/101/publish"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products"))
                                .andExpect(flash().attribute("error", "Publish prerequisites not met"));
        }

        @Test
        void hide_WhenServiceHidesSuccessfully_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/hide"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).hideProduct("artisan@test.com", 101);
        }

        @Test
        void show_WhenServiceShowsSuccessfully_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/artisan/products/101/show"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/artisan/products"))
                                .andExpect(flash().attributeExists("success"));

                verify(artisanProductService).showProduct("artisan@test.com", 101);
        }

        @Test
        void preview_WhenProductAccessIsRejectedByService_ShouldPropagateAccessFailure() {
                when(artisanProductService.getMyProduct("artisan@test.com", 101))
                                .thenThrow(new RuntimeException("Product does not belong to artisan"));

                assertThatThrownBy(() -> mockMvc.perform(get("/artisan/products/101/preview")))
                                .hasCauseInstanceOf(RuntimeException.class);
        }

        @Test
        void myProducts_WhenNoAuthenticatedPrincipalExists_ShouldNotCallControllerService() {
                SecurityContextHolder.clearContext();

                assertThatThrownBy(() -> mockMvc.perform(get("/artisan/products")))
                                .hasCauseInstanceOf(NullPointerException.class);

                verify(artisanProductService, never()).getMyProducts(any(String.class));
        }

        private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validProductPost(
                        String url) {
                return post(url)
                                .param("varietyId", "1")
                                .param("segmentId", "2")
                                .param("productName", "Bonsai đẹp")
                                .param("description", "Mô tả")
                                .param("treeStory", "Câu chuyện")
                                .param("age", "5")
                                .param("height", "45.5")
                                .param("trunkDiameter", "4.2")
                                .param("style", "Dáng trực")
                                .param("price", "1500000")
                                .param("tagIds", "100", "200");
        }

        private void mockLookupData() {
                Category category = category(1);
                when(artisanProductService.getCategories()).thenReturn(List.of(category));
                when(artisanProductService.getVarieties()).thenReturn(List.of(variety(1, category)));
                when(artisanProductService.getSegments()).thenReturn(List.of(segment(2)));
                when(artisanProductService.getTags()).thenReturn(List.of(tag(100), tag(200)));
        }

        private ArtisanProductFormDTO validForm() {
                return ArtisanProductFormDTO.builder()
                                .varietyId(1)
                                .segmentId(2)
                                .productName("Bonsai đẹp")
                                .age(5)
                                .height(45.5F)
                                .trunkDiameter(4.2F)
                                .style("Dáng trực")
                                .price(new BigDecimal("1500000"))
                                .tagIds(List.of(100, 200))
                                .build();
        }

        private Product product(Integer productId) {
                return Product.builder()
                                .productId(productId)
                                .productName("Bonsai " + productId)
                                .createdBy(User.builder().email("artisan@test.com").build())
                                .build();
        }

        private Category category(Integer categoryId) {
                return Category.builder()
                                .categoryId(categoryId)
                                .categoryName("Outdoor")
                                .build();
        }

        private Variety variety(Integer varietyId, Category category) {
                return Variety.builder()
                                .varietyId(varietyId)
                                .varietyName("Kim giòn")
                                .category(category)
                                .build();
        }

        private ProductSegment segment(Integer segmentId) {
                return ProductSegment.builder()
                                .segmentId(segmentId)
                                .segmentName("Standard")
                                .build();
        }

        private Tag tag(Integer tagId) {
                return Tag.builder()
                                .tagId(tagId)
                                .tagName("Tag " + tagId)
                                .build();
        }

        private ProductMedia media(Integer mediaId, String mediaType, Boolean isThumbnail) {
                return ProductMedia.builder()
                                .mediaId(mediaId)
                                .mediaType(mediaType)
                                .mediaUrl("https://cdn.test/media-" + mediaId)
                                .isThumbnail(isThumbnail)
                                .build();
        }

        private ProductJournalEvent journalEvent(Integer eventId, Product product) {
                return ProductJournalEvent.builder()
                                .eventId(eventId)
                                .product(product)
                                .eventDate(LocalDate.now())
                                .eventType("GROWTH")
                                .title("Growth update")
                                .build();
        }
}
