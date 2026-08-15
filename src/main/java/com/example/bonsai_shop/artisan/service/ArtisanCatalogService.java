package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductTagRepository;
import com.example.bonsai_shop.product.repository.TagRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtisanCatalogService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final String CATALOG_NAME_PATTERN = "^[\\p{L}\\p{N}\\s.,'\\-()]+$";

    private final CategoryRepository categoryRepository;
    private final VarietyRepository varietyRepository;
    private final ProductRepository productRepository;
    private final ProductTagRepository productTagRepository;
    private final TagRepository tagRepository;

    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    public List<Variety> getVarieties() {
        return varietyRepository.findAll();
    }

    public List<Tag> getTags() {
        return tagRepository.findAll();
    }

    public Set<Integer> getCategoryIdsInUse() {
        return categoryRepository.findAll().stream()
                .filter(category -> productRepository.existsByVarietyCategoryCategoryId(category.getCategoryId())
                        || varietyRepository.existsByCategoryCategoryId(category.getCategoryId()))
                .map(Category::getCategoryId)
                .collect(Collectors.toSet());
    }

    public Set<Integer> getVarietyIdsInUse() {
        return varietyRepository.findAll().stream()
                .filter(variety -> productRepository.existsByVarietyVarietyId(variety.getVarietyId()))
                .map(Variety::getVarietyId)
                .collect(Collectors.toSet());
    }

    public Set<Integer> getTagIdsInUse() {
        return tagRepository.findAll().stream()
                .filter(tag -> productTagRepository.existsForTagId(tag.getTagId()))
                .map(Tag::getTagId)
                .collect(Collectors.toSet());
    }

    public Map<Integer, Long> getVarietyCountByCategoryId() {
        return varietyRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        variety -> variety.getCategory().getCategoryId(),
                        Collectors.counting()
                ));
    }

    public Map<Integer, Long> getProductCountByCategoryId() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Category::getCategoryId,
                        category -> productRepository.countByVarietyCategoryCategoryId(category.getCategoryId())
                ));
    }

    public Map<Integer, Long> getProductCountByVarietyId() {
        return varietyRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Variety::getVarietyId,
                        variety -> productRepository.countByVarietyVarietyId(variety.getVarietyId())
                ));
    }

    public Map<Integer, Long> getProductCountByTagId() {
        return tagRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Tag::getTagId,
                        tag -> productTagRepository.countByTagTagId(tag.getTagId())
                ));
    }

    @Transactional
    public void createCategory(String categoryName, String description) {
        String normalizedCategoryName = requireText(categoryName, "Tên category không được để trống!");
        ensureCategoryNameUnique(normalizedCategoryName, null);
        categoryRepository.save(Category.builder()
                .categoryName(normalizedCategoryName)
                .description(normalizeDescription(description))
                .build());
    }

    @Transactional
    public void createVariety(Integer categoryId, String varietyName, String description) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại!"));
        String normalizedVarietyName = requireText(varietyName, "Tên variety không được để trống!");
        ensureVarietyNameUnique(categoryId, normalizedVarietyName, null);

        varietyRepository.save(Variety.builder()
                .category(category)
                .varietyName(normalizedVarietyName)
                .description(normalizeDescription(description))
                .build());
    }

    @Transactional
    public void createTag(String tagName) {
        String normalizedTagName = requireText(tagName, "Tên tag không được để trống!");
        ensureTagNameUnique(normalizedTagName, null);
        tagRepository.save(Tag.builder()
                .tagName(normalizedTagName)
                .build());
    }

    @Transactional
    public void updateCategory(Integer categoryId, String categoryName, String description) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại!"));
        String normalizedCategoryName = requireText(categoryName, "Tên category không được để trống!");
        ensureCategoryNameUnique(normalizedCategoryName, categoryId);

        category.setCategoryName(normalizedCategoryName);
        category.setDescription(normalizeDescription(description));
        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Integer categoryId) {
        if (varietyRepository.existsByCategoryCategoryId(categoryId)) {
            throw new RuntimeException("Không thể xóa category đang có variety.");
        }
        categoryRepository.deleteById(categoryId);
    }

    @Transactional
    public void updateVariety(Integer varietyId, Integer categoryId, String varietyName, String description) {
        Variety variety = varietyRepository.findById(varietyId)
                .orElseThrow(() -> new RuntimeException("Variety không tồn tại!"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại!"));
        String normalizedVarietyName = requireText(varietyName, "Tên variety không được để trống!");
        ensureVarietyNameUnique(categoryId, normalizedVarietyName, varietyId);

        variety.setCategory(category);
        variety.setVarietyName(normalizedVarietyName);
        variety.setDescription(normalizeDescription(description));
        varietyRepository.save(variety);
    }

    @Transactional
    public void deleteVariety(Integer varietyId) {
        if (productRepository.existsByVarietyVarietyId(varietyId)) {
            throw new RuntimeException("Không thể xóa variety đang được sản phẩm sử dụng.");
        }
        varietyRepository.deleteById(varietyId);
    }

    @Transactional
    public void updateTag(Integer tagId, String tagName) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag không tồn tại!"));
        String normalizedTagName = requireText(tagName, "Tên tag không được để trống!");
        ensureTagNameUnique(normalizedTagName, tagId);

        tag.setTagName(normalizedTagName);
        tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Integer tagId) {
        if (productTagRepository.existsForTagId(tagId)) {
            throw new RuntimeException("Không thể xóa tag đang được sản phẩm sử dụng.");
        }
        tagRepository.deleteById(tagId);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() > MAX_NAME_LENGTH) {
            throw new RuntimeException("Tên không được vượt quá " + MAX_NAME_LENGTH + " ký tự.");
        }
        if (!normalizedValue.matches(CATALOG_NAME_PATTERN)) {
            throw new RuntimeException("Tên chỉ được chứa chữ, số, khoảng trắng và các ký tự . , ' - ( ).");
        }
        return normalizedValue;
    }

    private String normalizeDescription(String description) {
        String normalizedDescription = blankToNull(description);
        if (normalizedDescription != null && normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new RuntimeException("Mô tả không được vượt quá " + MAX_DESCRIPTION_LENGTH + " ký tự.");
        }
        return normalizedDescription;
    }

    private void ensureCategoryNameUnique(String categoryName, Integer currentCategoryId) {
        boolean isDuplicate = currentCategoryId == null
                ? categoryRepository.existsByCategoryNameIgnoreCase(categoryName)
                : categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(categoryName, currentCategoryId);
        if (isDuplicate) {
            throw new RuntimeException("Tên category đã tồn tại.");
        }
    }

    private void ensureVarietyNameUnique(Integer categoryId, String varietyName, Integer currentVarietyId) {
        boolean isDuplicate = currentVarietyId == null
                ? varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCase(categoryId, varietyName)
                : varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCaseAndVarietyIdNot(categoryId, varietyName, currentVarietyId);
        if (isDuplicate) {
            throw new RuntimeException("Tên variety đã tồn tại trong category này.");
        }
    }

    private void ensureTagNameUnique(String tagName, Integer currentTagId) {
        boolean isDuplicate = currentTagId == null
                ? tagRepository.existsByTagNameIgnoreCase(tagName)
                : tagRepository.existsByTagNameIgnoreCaseAndTagIdNot(tagName, currentTagId);
        if (isDuplicate) {
            throw new RuntimeException("Tên tag đã tồn tại.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
