package com.example.bonsai_shop.artisan1.controller;

import com.example.bonsai_shop.artisan.controller.ArtisanCatalogController;
import com.example.bonsai_shop.artisan.service.ArtisanCatalogService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.Variety;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ArtisanCatalogControllerIntegrationTest {

    private ArtisanCatalogService artisanCatalogService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        artisanCatalogService = mock(ArtisanCatalogService.class);
        ArtisanCatalogController controller = new ArtisanCatalogController(artisanCatalogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void catalog_WhenRequested_ShouldDisplayCatalogManagementPage() throws Exception {
        Category category = category(1);
        Variety variety = variety(10, category);
        Tag tag = tag(100);
        when(artisanCatalogService.getCategories()).thenReturn(List.of(category));
        when(artisanCatalogService.getVarieties()).thenReturn(List.of(variety));
        when(artisanCatalogService.getTags()).thenReturn(List.of(tag));
        when(artisanCatalogService.getCategoryIdsInUse()).thenReturn(Set.of(1));
        when(artisanCatalogService.getVarietyIdsInUse()).thenReturn(Set.of(10));
        when(artisanCatalogService.getTagIdsInUse()).thenReturn(Set.of(100));

        mockMvc.perform(get("/artisan/catalog"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/catalog"))
                .andExpect(model().attribute("categories", List.of(category)))
                .andExpect(model().attribute("varieties", List.of(variety)))
                .andExpect(model().attribute("tags", List.of(tag)))
                .andExpect(model().attribute("categoryIdsInUse", Set.of(1)))
                .andExpect(model().attribute("varietyIdsInUse", Set.of(10)))
                .andExpect(model().attribute("tagIdsInUse", Set.of(100)));
    }

    @Test
    void createCategory_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        mockMvc.perform(post("/artisan/catalog/categories")
                        .param("categoryName", "Outdoor")
                        .param("description", "Outdoor bonsai"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).createCategory("Outdoor", "Outdoor bonsai");
    }

    @Test
    void createCategory_WhenServiceRejectsCreation_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Category invalid or duplicate"))
                .when(artisanCatalogService).createCategory("Outdoor", "Outdoor bonsai");

        mockMvc.perform(post("/artisan/catalog/categories")
                        .param("categoryName", "Outdoor")
                        .param("description", "Outdoor bonsai"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Category invalid or duplicate"));
    }

    @Test
    void updateCategory_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        mockMvc.perform(post("/artisan/catalog/categories/1")
                        .param("categoryName", "Updated Outdoor")
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).updateCategory(1, "Updated Outdoor", "Updated description");
    }

    @Test
    void deleteCategory_WhenServiceAllowsOrRejectsDeletion_ShouldRedirectWithExpectedFlash() throws Exception {
        mockMvc.perform(post("/artisan/catalog/categories/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).deleteCategory(1);

        doThrow(new RuntimeException("Category is in use"))
                .when(artisanCatalogService).deleteCategory(2);

        mockMvc.perform(post("/artisan/catalog/categories/2/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Category is in use"));
    }

    @Test
    void createVariety_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        mockMvc.perform(post("/artisan/catalog/varieties")
                        .param("categoryId", "1")
                        .param("varietyName", "Kim giòn")
                        .param("description", "Small leaves"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).createVariety(1, "Kim giòn", "Small leaves");
    }

    @Test
    void createVariety_WhenCategoryInvalid_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Category does not exist"))
                .when(artisanCatalogService).createVariety(999, "Kim giòn", "Small leaves");

        mockMvc.perform(post("/artisan/catalog/varieties")
                        .param("categoryId", "999")
                        .param("varietyName", "Kim giòn")
                        .param("description", "Small leaves"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Category does not exist"));
    }

    @Test
    void updateVariety_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        mockMvc.perform(post("/artisan/catalog/varieties/10")
                        .param("categoryId", "1")
                        .param("varietyName", "Updated Kim giòn")
                        .param("description", "Updated leaves"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).updateVariety(10, 1, "Updated Kim giòn", "Updated leaves");
    }

    @Test
    void deleteVariety_WhenServiceAllowsOrRejectsDeletion_ShouldRedirectWithExpectedFlash() throws Exception {
        mockMvc.perform(post("/artisan/catalog/varieties/10/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).deleteVariety(10);

        doThrow(new RuntimeException("Variety is in use"))
                .when(artisanCatalogService).deleteVariety(20);

        mockMvc.perform(post("/artisan/catalog/varieties/20/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Variety is in use"));
    }

    @Test
    void createTag_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        mockMvc.perform(post("/artisan/catalog/tags")
                        .param("tagName", "Mini"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).createTag("Mini");
    }

    @Test
    void createTag_WhenServiceRejectsCreation_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Tag invalid or duplicate"))
                .when(artisanCatalogService).createTag("Mini");

        mockMvc.perform(post("/artisan/catalog/tags")
                        .param("tagName", "Mini"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Tag invalid or duplicate"));
    }

    @Test
    void updateTag_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        mockMvc.perform(post("/artisan/catalog/tags/100")
                        .param("tagName", "Updated Mini"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).updateTag(100, "Updated Mini");
    }

    @Test
    void deleteTag_WhenServiceAllowsOrRejectsDeletion_ShouldRedirectWithExpectedFlash() throws Exception {
        mockMvc.perform(post("/artisan/catalog/tags/100/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attributeExists("success"));

        verify(artisanCatalogService).deleteTag(100);

        doThrow(new RuntimeException("Tag is in use"))
                .when(artisanCatalogService).deleteTag(200);

        mockMvc.perform(post("/artisan/catalog/tags/200/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/catalog"))
                .andExpect(flash().attribute("error", "Tag is in use"));
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
                .category(category)
                .varietyName("Kim giòn")
                .build();
    }

    private Tag tag(Integer tagId) {
        return Tag.builder()
                .tagId(tagId)
                .tagName("Mini")
                .build();
    }
}
