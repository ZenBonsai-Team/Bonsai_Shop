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
// Service quản lý dữ liệu catalog dùng khi artisan đăng sản phẩm.
public class ArtisanCatalogService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final String CATALOG_NAME_PATTERN = "^[\\p{L}\\p{N}\\s.,'\\-()]+$";

    private final CategoryRepository categoryRepository;
    private final VarietyRepository varietyRepository;
    private final ProductRepository productRepository;
    private final ProductTagRepository productTagRepository;
    private final TagRepository tagRepository;

    // Lấy danh sách category theo thứ tự tên.
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    // Lấy danh sách variety kèm category cha.
    public List<Variety> getVarieties() {
        return varietyRepository.findAll();
    }

    // Lấy danh sách tag theo thứ tự tên.
    public List<Tag> getTags() {
        return tagRepository.findAll();
    }

    // Xác định category đang được variety hoặc sản phẩm sử dụng.
    public Set<Integer> getCategoryIdsInUse() {
        return categoryRepository.findAll().stream()
                .filter(category -> productRepository.existsByVarietyCategoryCategoryId(category.getCategoryId())
                        || varietyRepository.existsByCategoryCategoryId(category.getCategoryId()))
                .map(Category::getCategoryId)
                .collect(Collectors.toSet());
    }

    // Xác định variety đang được sản phẩm sử dụng.
    public Set<Integer> getVarietyIdsInUse() {
        return varietyRepository.findAll().stream()
                .filter(variety -> productRepository.existsByVarietyVarietyId(variety.getVarietyId()))
                .map(Variety::getVarietyId)
                .collect(Collectors.toSet());
    }

    // Xác định tag đang được sản phẩm sử dụng.
    public Set<Integer> getTagIdsInUse() {
        return tagRepository.findAll().stream()
                .filter(tag -> productTagRepository.existsForTagId(tag.getTagId()))
                .map(Tag::getTagId)
                .collect(Collectors.toSet());
    }

    // Đếm số variety theo từng category.
    public Map<Integer, Long> getVarietyCountByCategoryId() {
        return varietyRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        variety -> variety.getCategory().getCategoryId(),
                        Collectors.counting()
                ));
    }

    // Đếm số sản phẩm theo category thông qua variety.
    public Map<Integer, Long> getProductCountByCategoryId() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Category::getCategoryId,
                        category -> productRepository.countByVarietyCategoryCategoryId(category.getCategoryId())
                ));
    }

    // Đếm số sản phẩm theo từng variety.
    public Map<Integer, Long> getProductCountByVarietyId() {
        return varietyRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Variety::getVarietyId,
                        variety -> productRepository.countByVarietyVarietyId(variety.getVarietyId())
                ));
    }

    // Đếm số sản phẩm theo từng tag.
    public Map<Integer, Long> getProductCountByTagId() {
        return tagRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Tag::getTagId,
                        tag -> productTagRepository.countByTagTagId(tag.getTagId())
                ));
    }

    @Transactional
    // Tạo category mới sau khi chuẩn hóa và kiểm tra trùng tên.
    public void createCategory(String categoryName, String description) {
        String normalizedCategoryName = requireText(categoryName, "Tên category không được để trống!");
        ensureCategoryNameUnique(normalizedCategoryName, null);
        categoryRepository.save(Category.builder()
                .categoryName(normalizedCategoryName)
                .description(normalizeDescription(description))
                .build());
    }

    @Transactional
    // Tạo variety mới trong category hợp lệ.
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
    // Tạo tag mới sau khi kiểm tra trùng tên.
    public void createTag(String tagName) {
        String normalizedTagName = requireText(tagName, "Tên tag không được để trống!");
        ensureTagNameUnique(normalizedTagName, null);
        tagRepository.save(Tag.builder()
                .tagName(normalizedTagName)
                .build());
    }

    @Transactional
    // Cập nhật category đang tồn tại.
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
    // Chặn xóa category khi vẫn còn dữ liệu phụ thuộc.
    public void deleteCategory(Integer categoryId) {
        if (varietyRepository.existsByCategoryCategoryId(categoryId)) {
            throw new RuntimeException("Không thể xóa category đang có variety.");
        }
        categoryRepository.deleteById(categoryId);
    }

    @Transactional
    // Cập nhật variety và category cha.
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
    // Chặn xóa variety khi vẫn còn sản phẩm phụ thuộc.
    public void deleteVariety(Integer varietyId) {
        if (productRepository.existsByVarietyVarietyId(varietyId)) {
            throw new RuntimeException("Không thể xóa variety đang được sản phẩm sử dụng.");
        }
        varietyRepository.deleteById(varietyId);
    }

    @Transactional
    // Cập nhật tag đang tồn tại.
    public void updateTag(Integer tagId, String tagName) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag không tồn tại!"));
        String normalizedTagName = requireText(tagName, "Tên tag không được để trống!");
        ensureTagNameUnique(normalizedTagName, tagId);

        tag.setTagName(normalizedTagName);
        tagRepository.save(tag);
    }

    @Transactional
    // Chặn xóa tag khi vẫn còn sản phẩm phụ thuộc.
    public void deleteTag(Integer tagId) {
        if (productTagRepository.existsForTagId(tagId)) {
            throw new RuntimeException("Không thể xóa tag đang được sản phẩm sử dụng.");
        }
        tagRepository.deleteById(tagId);
    }

    // Chuẩn hóa text bắt buộc và kiểm tra độ dài/ký tự hợp lệ.
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

    // Chuẩn hóa mô tả tùy chọn và kiểm tra độ dài.
    private String normalizeDescription(String description) {
        String normalizedDescription = blankToNull(description);
        if (normalizedDescription != null && normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new RuntimeException("Mô tả không được vượt quá " + MAX_DESCRIPTION_LENGTH + " ký tự.");
        }
        return normalizedDescription;
    }

    // Đảm bảo tên category không trùng với bản ghi khác.
    private void ensureCategoryNameUnique(String categoryName, Integer currentCategoryId) {
        boolean isDuplicate = currentCategoryId == null
                ? categoryRepository.existsByCategoryNameIgnoreCase(categoryName)
                : categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(categoryName, currentCategoryId);
        if (isDuplicate) {
            throw new RuntimeException("Tên category đã tồn tại.");
        }
    }

    // Đảm bảo tên variety không trùng trong cùng category.
    private void ensureVarietyNameUnique(Integer categoryId, String varietyName, Integer currentVarietyId) {
        boolean isDuplicate = currentVarietyId == null
                ? varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCase(categoryId, varietyName)
                : varietyRepository.existsByCategoryCategoryIdAndVarietyNameIgnoreCaseAndVarietyIdNot(categoryId, varietyName, currentVarietyId);
        if (isDuplicate) {
            throw new RuntimeException("Tên variety đã tồn tại trong category này.");
        }
    }

    // Đảm bảo tên tag không trùng với bản ghi khác.
    private void ensureTagNameUnique(String tagName, Integer currentTagId) {
        boolean isDuplicate = currentTagId == null
                ? tagRepository.existsByTagNameIgnoreCase(tagName)
                : tagRepository.existsByTagNameIgnoreCaseAndTagIdNot(tagName, currentTagId);
        if (isDuplicate) {
            throw new RuntimeException("Tên tag đã tồn tại.");
        }
    }

    // Đổi chuỗi rỗng thành null để lưu dữ liệu nhất quán.
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
