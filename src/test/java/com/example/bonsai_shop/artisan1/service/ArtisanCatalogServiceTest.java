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
    void retrievalMethods_WhenRepositoriesReturnData_ShouldReturnCatalogData() {
        List<Category> categories = List.of(category(1, "Outdoor"));
        List<Variety> varieties = List.of(variety(10, categories.get(0), "Kim giòn"));
        List<Tag> tags = List.of(tag(100, "Mini"));

        when(categoryRepository.findAll()).thenReturn(categories);
        when(varietyRepository.findAll()).thenReturn(varieties);
        when(tagRepository.findAll()).thenReturn(tags);

        assertThat(artisanCatalogService.getCategories()).isEqualTo(categories);
        assertThat(artisanCatalogService.getVarieties()).isEqualTo(varieties);
        assertThat(artisanCatalogService.getTags()).isEqualTo(tags);
    }

    @Test
    void usageMethods_WhenItemsAreReferenced_ShouldReturnIdsCurrentlyInUse() {
        Category categoryOne = category(1, "Outdoor");
        Category categoryTwo = category(2, "Indoor");
        Variety varietyOne = variety(10, categoryOne, "Kim giòn");
        Variety varietyTwo = variety(20, categoryTwo, "Sanh");
        Tag tagOne = tag(100, "Mini");
        Tag tagTwo = tag(200, "Premium");

        when(categoryRepository.findAll()).thenReturn(List.of(categoryOne, categoryTwo));
        when(varietyRepository.existsByCategoryCategoryId(1)).thenReturn(true);
        when(productRepository.existsByVarietyCategoryCategoryId(2)).thenReturn(true);
        when(varietyRepository.findAll()).thenReturn(List.of(varietyOne, varietyTwo));
        when(productRepository.existsByVarietyVarietyId(10)).thenReturn(true);
        when(tagRepository.findAll()).thenReturn(List.of(tagOne, tagTwo));
        when(productTagRepository.existsForTagId(200)).thenReturn(true);

        Set<Integer> categoryIds = artisanCatalogService.getCategoryIdsInUse();
        Set<Integer> varietyIds = artisanCatalogService.getVarietyIdsInUse();
        Set<Integer> tagIds = artisanCatalogService.getTagIdsInUse();

        assertThat(categoryIds).containsExactlyInAnyOrder(1, 2);
        assertThat(varietyIds).containsExactly(10);
        assertThat(tagIds).containsExactly(200);
    }

    @Test
    void createCategory_WhenNameIsValidAndUnique_ShouldSaveCategory() {
        when(categoryRepository.existsByCategoryNameIgnoreCase("Outdoor"))
                .thenReturn(false);

        artisanCatalogService.createCategory(" Outdoor ", "  Sunny bonsai  ");

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getCategoryName()).isEqualTo("Outdoor");
        assertThat(categoryCaptor.getValue().getDescription()).isEqualTo("Sunny bonsai");
    }

    @Test
    void createCategory_WhenNameBlankOrDuplicate_ShouldThrowException() {
        assertThatThrownBy(() -> artisanCatalogService.createCategory(" ", "Description"))
                .isInstanceOf(RuntimeException.class);

        when(categoryRepository.existsByCategoryNameIgnoreCase("Outdoor"))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.createCategory("Outdoor", "Description"))
                .isInstanceOf(RuntimeException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_WhenCategoryExistsAndNameUnique_ShouldUpdateCategory() {
        Category category = category(1, "Old");

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot("Outdoor", 1))
                .thenReturn(false);

        artisanCatalogService.updateCategory(1, " Outdoor ", " Updated description ");

        assertThat(category.getCategoryName()).isEqualTo("Outdoor");
        assertThat(category.getDescription()).isEqualTo("Updated description");
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_WhenCategoryMissingOrNameDuplicate_ShouldThrowException() {
        when(categoryRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanCatalogService.updateCategory(1, "Outdoor", "Description"))
                .isInstanceOf(RuntimeException.class);

        Category category = category(2, "Old");
        when(categoryRepository.findById(2))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot("Outdoor", 2))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.updateCategory(2, "Outdoor", "Description"))
                .isInstanceOf(RuntimeException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_WhenCategoryIsNotReferenced_ShouldDeleteCategory() {
        when(varietyRepository.existsByCategoryCategoryId(1))
                .thenReturn(false);

        artisanCatalogService.deleteCategory(1);

        verify(categoryRepository).deleteById(1);
    }

    @Test
    void deleteCategory_WhenCategoryHasVariety_ShouldThrowException() {
        when(varietyRepository.existsByCategoryCategoryId(1))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.deleteCategory(1))
                .isInstanceOf(RuntimeException.class);

        verify(categoryRepository, never()).deleteById(any(Integer.class));
    }

    @Test
    void createVariety_WhenCategoryExistsAndNameUnique_ShouldSaveVariety() {
        Category category = category(1, "Outdoor");

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));
        when(varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCase(1, "Kim giòn"))
                .thenReturn(false);

        artisanCatalogService.createVariety(1, " Kim giòn ", " Small leaves ");

        ArgumentCaptor<Variety> varietyCaptor = ArgumentCaptor.forClass(Variety.class);
        verify(varietyRepository).save(varietyCaptor.capture());
        assertThat(varietyCaptor.getValue().getCategory()).isEqualTo(category);
        assertThat(varietyCaptor.getValue().getVarietyName()).isEqualTo("Kim giòn");
        assertThat(varietyCaptor.getValue().getDescription()).isEqualTo("Small leaves");
    }

    @Test
    void createVariety_WhenCategoryMissingOrNameInvalidOrDuplicate_ShouldThrowException() {
        when(categoryRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanCatalogService.createVariety(1, "Kim giòn", "Description"))
                .isInstanceOf(RuntimeException.class);

        Category category = category(2, "Outdoor");
        when(categoryRepository.findById(2))
                .thenReturn(Optional.of(category));
        when(varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCase(2, "Kim giòn"))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.createVariety(2, "Kim giòn", "Description"))
                .isInstanceOf(RuntimeException.class);

        verify(varietyRepository, never()).save(any(Variety.class));
    }

    @Test
    void updateVariety_WhenVarietyAndCategoryExistAndNameUnique_ShouldUpdateVariety() {
        Category category = category(1, "Outdoor");
        Variety variety = variety(10, category(2, "Old Category"), "Old Variety");

        when(varietyRepository.findById(10))
                .thenReturn(Optional.of(variety));
        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));
        when(varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCaseAndVarietyIdNot(1, "Kim giòn", 10))
                .thenReturn(false);

        artisanCatalogService.updateVariety(10, 1, " Kim giòn ", " Updated variety ");

        assertThat(variety.getCategory()).isEqualTo(category);
        assertThat(variety.getVarietyName()).isEqualTo("Kim giòn");
        assertThat(variety.getDescription()).isEqualTo("Updated variety");
        verify(varietyRepository).save(variety);
    }

    @Test
    void updateVariety_WhenVarietyOrCategoryMissingOrNameDuplicate_ShouldThrowException() {
        when(varietyRepository.findById(10))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanCatalogService.updateVariety(10, 1, "Kim giòn", "Description"))
                .isInstanceOf(RuntimeException.class);

        Variety variety = variety(20, category(1, "Outdoor"), "Old Variety");
        when(varietyRepository.findById(20))
                .thenReturn(Optional.of(variety));
        when(categoryRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanCatalogService.updateVariety(20, 1, "Kim giòn", "Description"))
                .isInstanceOf(RuntimeException.class);

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category(1, "Outdoor")));
        when(varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCaseAndVarietyIdNot(1, "Kim giòn", 20))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.updateVariety(20, 1, "Kim giòn", "Description"))
                .isInstanceOf(RuntimeException.class);

        verify(varietyRepository, never()).save(any(Variety.class));
    }

    @Test
    void deleteVariety_WhenVarietyIsNotReferenced_ShouldDeleteVariety() {
        when(productRepository.existsByVarietyVarietyId(10))
                .thenReturn(false);

        artisanCatalogService.deleteVariety(10);

        verify(varietyRepository).deleteById(10);
    }

    @Test
    void deleteVariety_WhenVarietyIsUsedByProduct_ShouldThrowException() {
        when(productRepository.existsByVarietyVarietyId(10))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.deleteVariety(10))
                .isInstanceOf(RuntimeException.class);

        verify(varietyRepository, never()).deleteById(any(Integer.class));
    }

    @Test
    void createTag_WhenNameIsValidAndUnique_ShouldSaveTag() {
        when(tagRepository.existsByTagNameIgnoreCase("Mini"))
                .thenReturn(false);

        artisanCatalogService.createTag(" Mini ");

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getTagName()).isEqualTo("Mini");
    }

    @Test
    void createTag_WhenNameBlankOrDuplicate_ShouldThrowException() {
        assertThatThrownBy(() -> artisanCatalogService.createTag(" "))
                .isInstanceOf(RuntimeException.class);

        when(tagRepository.existsByTagNameIgnoreCase("Mini"))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.createTag("Mini"))
                .isInstanceOf(RuntimeException.class);

        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void updateTag_WhenTagExistsAndNameUnique_ShouldUpdateTag() {
        Tag tag = tag(100, "Old Tag");

        when(tagRepository.findById(100))
                .thenReturn(Optional.of(tag));
        when(tagRepository.existsByTagNameIgnoreCaseAndTagIdNot("Mini", 100))
                .thenReturn(false);

        artisanCatalogService.updateTag(100, " Mini ");

        assertThat(tag.getTagName()).isEqualTo("Mini");
        verify(tagRepository).save(tag);
    }

    @Test
    void updateTag_WhenTagMissingOrNameDuplicate_ShouldThrowException() {
        when(tagRepository.findById(100))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanCatalogService.updateTag(100, "Mini"))
                .isInstanceOf(RuntimeException.class);

        Tag tag = tag(200, "Old Tag");
        when(tagRepository.findById(200))
                .thenReturn(Optional.of(tag));
        when(tagRepository.existsByTagNameIgnoreCaseAndTagIdNot("Mini", 200))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.updateTag(200, "Mini"))
                .isInstanceOf(RuntimeException.class);

        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void deleteTag_WhenTagIsNotReferenced_ShouldDeleteTag() {
        when(productTagRepository.existsForTagId(100))
                .thenReturn(false);

        artisanCatalogService.deleteTag(100);

        verify(tagRepository).deleteById(100);
    }

    @Test
    void deleteTag_WhenTagIsUsedByProduct_ShouldThrowException() {
        when(productTagRepository.existsForTagId(100))
                .thenReturn(true);

        assertThatThrownBy(() -> artisanCatalogService.deleteTag(100))
                .isInstanceOf(RuntimeException.class);

        verify(tagRepository, never()).deleteById(any(Integer.class));
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
