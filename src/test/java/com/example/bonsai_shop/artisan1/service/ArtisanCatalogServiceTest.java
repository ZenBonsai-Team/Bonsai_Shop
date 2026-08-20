package com.example.bonsai_shop.artisan1.service;

import com.example.bonsai_shop.artisan.service.ArtisanCatalogService;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductTagRepository;
import com.example.bonsai_shop.product.repository.TagRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtisanCatalogServiceTest {

    private CategoryRepository categoryRepository;
    private VarietyRepository varietyRepository;
    private ProductRepository productRepository;
    private ProductTagRepository productTagRepository;
    private TagRepository tagRepository;
    private ArtisanCatalogService artisanCatalogService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        varietyRepository = mock(VarietyRepository.class);
        productRepository = mock(ProductRepository.class);
        productTagRepository = mock(ProductTagRepository.class);
        tagRepository = mock(TagRepository.class);

        artisanCatalogService = new ArtisanCatalogService(
                categoryRepository,
                varietyRepository,
                productRepository,
                productTagRepository,
                tagRepository
        );
    }

    @Test
    void getCatalogData_WhenRepositoriesReturnData_ShouldReturnCatalogData() {
        Category category = category(1, "Outdoor");
        Variety variety = variety(10, category, "Japanese Maple");
        Tag tag = tag(100, "Mini");

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(varietyRepository.findAll()).thenReturn(List.of(variety));
        when(tagRepository.findAll()).thenReturn(List.of(tag));

        assertThat(artisanCatalogService.getCategories()).containsExactly(category);
        assertThat(artisanCatalogService.getVarieties()).containsExactly(variety);
        assertThat(artisanCatalogService.getTags()).containsExactly(tag);
    }

    @Test
    void getIdsInUse_WhenCatalogItemsAreReferenced_ShouldReturnReferencedIdsOnly() {
        Category categoryWithVariety = category(1, "Outdoor");
        Category categoryWithProduct = category(2, "Indoor");
        Category unusedCategory = category(3, "Unused");
        Variety usedVariety = variety(10, categoryWithVariety, "Japanese Maple");
        Variety unusedVariety = variety(20, unusedCategory, "Pine");
        Tag unusedTag = tag(100, "Mini");
        Tag usedTag = tag(200, "Premium");

        when(categoryRepository.findAll()).thenReturn(List.of(categoryWithVariety, categoryWithProduct, unusedCategory));
        when(varietyRepository.existsByCategoryCategoryId(1)).thenReturn(true);
        when(productRepository.existsByVarietyCategoryCategoryId(2)).thenReturn(true);
        when(varietyRepository.findAll()).thenReturn(List.of(usedVariety, unusedVariety));
        when(productRepository.existsByVarietyVarietyId(10)).thenReturn(true);
        when(tagRepository.findAll()).thenReturn(List.of(unusedTag, usedTag));
        when(productTagRepository.existsForTagId(200)).thenReturn(true);

        Set<Integer> categoryIds = artisanCatalogService.getCategoryIdsInUse();
        Set<Integer> varietyIds = artisanCatalogService.getVarietyIdsInUse();
        Set<Integer> tagIds = artisanCatalogService.getTagIdsInUse();

        assertThat(categoryIds).containsExactlyInAnyOrder(1, 2);
        assertThat(varietyIds).containsExactly(10);
        assertThat(tagIds).containsExactly(200);
    }

    @Test
    void getCatalogUsageCounts_WhenRepositoriesReturnCounts_ShouldReturnCountMaps() {
        Category categoryOne = category(1, "Outdoor");
        Category categoryTwo = category(2, "Indoor");
        Variety varietyOne = variety(10, categoryOne, "Japanese Maple");
        Variety varietyTwo = variety(20, categoryTwo, "Pine");
        Tag tagOne = tag(100, "Mini");
        Tag tagTwo = tag(200, "Premium");

        when(categoryRepository.findAll()).thenReturn(List.of(categoryOne, categoryTwo));
        when(varietyRepository.findAll()).thenReturn(List.of(varietyOne, varietyTwo));
        when(tagRepository.findAll()).thenReturn(List.of(tagOne, tagTwo));
        when(productRepository.countByVarietyCategoryCategoryId(1)).thenReturn(3L);
        when(productRepository.countByVarietyCategoryCategoryId(2)).thenReturn(0L);
        when(productRepository.countByVarietyVarietyId(10)).thenReturn(2L);
        when(productRepository.countByVarietyVarietyId(20)).thenReturn(1L);
        when(productTagRepository.countByTagTagId(100)).thenReturn(4L);
        when(productTagRepository.countByTagTagId(200)).thenReturn(0L);

        Map<Integer, Long> varietyCountsByCategory = artisanCatalogService.getVarietyCountByCategoryId();
        Map<Integer, Long> productCountsByCategory = artisanCatalogService.getProductCountByCategoryId();
        Map<Integer, Long> productCountsByVariety = artisanCatalogService.getProductCountByVarietyId();
        Map<Integer, Long> productCountsByTag = artisanCatalogService.getProductCountByTagId();

        assertThat(varietyCountsByCategory).containsEntry(1, 1L).containsEntry(2, 1L);
        assertThat(productCountsByCategory).containsEntry(1, 3L).containsEntry(2, 0L);
        assertThat(productCountsByVariety).containsEntry(10, 2L).containsEntry(20, 1L);
        assertThat(productCountsByTag).containsEntry(100, 4L).containsEntry(200, 0L);
    }

    @Test
    void createCategory_WhenNameIsValidAndUnique_ShouldTrimAndSaveCategory() {
        when(categoryRepository.existsByCategoryNameIgnoreCase("Outdoor")).thenReturn(false);

        artisanCatalogService.createCategory(" Outdoor ", " Sunny bonsai ");

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getCategoryName()).isEqualTo("Outdoor");
        assertThat(categoryCaptor.getValue().getDescription()).isEqualTo("Sunny bonsai");
    }

    @Test
    void createCategory_WhenNameIsBlank_ShouldRejectCategory() {
        assertThatThrownBy(() -> artisanCatalogService.createCategory("   ", "Description"))
                .isInstanceOf(RuntimeException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_WhenNameIsDuplicate_ShouldRejectCategory() {
        when(categoryRepository.existsByCategoryNameIgnoreCase("Outdoor")).thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.createCategory(" Outdoor ", "Description"))
                .isInstanceOf(RuntimeException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_WhenCategoryExistsAndNameIsUnique_ShouldUpdateCategory() {
        Category category = category(1, "Old");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot("Outdoor", 1)).thenReturn(false);

        artisanCatalogService.updateCategory(1, " Outdoor ", " Updated description ");

        assertThat(category.getCategoryName()).isEqualTo("Outdoor");
        assertThat(category.getDescription()).isEqualTo("Updated description");
        verify(categoryRepository).save(category);
    }

    @Test
    void deleteCategory_WhenCategoryContainsVarieties_ShouldRejectDelete() {
        when(varietyRepository.existsByCategoryCategoryId(1)).thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.deleteCategory(1))
                .isInstanceOf(RuntimeException.class);

        verify(categoryRepository, never()).deleteById(any(Integer.class));
    }

    @Test
    void createVariety_WhenCategoryExistsAndNameIsUnique_ShouldSaveVariety() {
        Category category = category(1, "Outdoor");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCase(1, "Japanese Maple"))
                .thenReturn(false);

        artisanCatalogService.createVariety(1, " Japanese Maple ", " Small leaves ");

        ArgumentCaptor<Variety> varietyCaptor = ArgumentCaptor.forClass(Variety.class);
        verify(varietyRepository).save(varietyCaptor.capture());
        assertThat(varietyCaptor.getValue().getCategory()).isEqualTo(category);
        assertThat(varietyCaptor.getValue().getVarietyName()).isEqualTo("Japanese Maple");
        assertThat(varietyCaptor.getValue().getDescription()).isEqualTo("Small leaves");
    }

    @Test
    void createVariety_WhenCategoryNotFound_ShouldRejectVariety() {
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanCatalogService.createVariety(1, "Japanese Maple", "Description"))
                .isInstanceOf(RuntimeException.class);

        verify(varietyRepository, never()).save(any(Variety.class));
    }

    @Test
    void updateVariety_WhenVarietyAndCategoryExistAndNameIsUnique_ShouldUpdateVariety() {
        Category targetCategory = category(1, "Outdoor");
        Variety variety = variety(10, category(2, "Old Category"), "Old Variety");

        when(varietyRepository.findById(10)).thenReturn(Optional.of(variety));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(targetCategory));
        when(varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCaseAndVarietyIdNot(
                1,
                "Japanese Maple",
                10
        )).thenReturn(false);

        artisanCatalogService.updateVariety(10, 1, " Japanese Maple ", " Updated variety ");

        assertThat(variety.getCategory()).isEqualTo(targetCategory);
        assertThat(variety.getVarietyName()).isEqualTo("Japanese Maple");
        assertThat(variety.getDescription()).isEqualTo("Updated variety");
        verify(varietyRepository).save(variety);
    }

    @Test
    void deleteVariety_WhenVarietyIsUsedByProduct_ShouldRejectDelete() {
        when(productRepository.existsByVarietyVarietyId(10)).thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.deleteVariety(10))
                .isInstanceOf(RuntimeException.class);

        verify(varietyRepository, never()).deleteById(any(Integer.class));
    }

    @Test
    void createTag_WhenNameIsValidAndUnique_ShouldTrimAndSaveTag() {
        when(tagRepository.existsByTagNameIgnoreCase("Mini")).thenReturn(false);

        artisanCatalogService.createTag(" Mini ");

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getTagName()).isEqualTo("Mini");
    }

    @Test
    void updateTag_WhenNameIsDuplicate_ShouldRejectUpdate() {
        Tag tag = tag(100, "Old Tag");
        when(tagRepository.findById(100)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByTagNameIgnoreCaseAndTagIdNot("Mini", 100)).thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.updateTag(100, " Mini "))
                .isInstanceOf(RuntimeException.class);

        assertThat(tag.getTagName()).isEqualTo("Old Tag");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void deleteTag_WhenTagIsUsedByProduct_ShouldRejectDelete() {
        when(productTagRepository.existsForTagId(100)).thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.deleteTag(100))
                .isInstanceOf(RuntimeException.class);

        verify(tagRepository, never()).deleteById(any(Integer.class));
    }

    @Test
    void createCategory_WhenDescriptionExceedsMaximumLength_ShouldRejectCategory() {
        when(categoryRepository.existsByCategoryNameIgnoreCase("Outdoor")).thenReturn(false);

        assertThatThrownBy(() -> artisanCatalogService.createCategory("Outdoor", "a".repeat(501)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("500");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    private Category category(Integer categoryId, String categoryName) {
        return Category.builder()
                .categoryId(categoryId)
                .categoryName(categoryName)
                .build();
    }

    private Variety variety(Integer varietyId, Category category, String varietyName) {
        return Variety.builder()
                .varietyId(varietyId)
                .category(category)
                .varietyName(varietyName)
                .build();
    }

    private Tag tag(Integer tagId, String tagName) {
        return Tag.builder()
                .tagId(tagId)
                .tagName(tagName)
                .build();
    }
}
