package com.example.bonsai_shop.seller.service;

import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductTagRepository;
import com.example.bonsai_shop.product.repository.TagRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerCatalogService {

    private final CategoryRepository categoryRepository;
    private final VarietyRepository varietyRepository;
    private final ProductSegmentRepository productSegmentRepository;
    private final ProductRepository productRepository;
    private final ProductTagRepository productTagRepository;
    private final TagRepository tagRepository;

    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    public List<Variety> getVarieties() {
        return varietyRepository.findAll();
    }

    public List<ProductSegment> getSegments() {
        return productSegmentRepository.findAll();
    }

    public List<Tag> getTags() {
        return tagRepository.findAll();
    }

    @Transactional
    public void createCategory(String categoryName, String description) {
        categoryRepository.save(Category.builder()
                .categoryName(requireText(categoryName, "Tên category không được để trống!"))
                .description(blankToNull(description))
                .build());
    }

    @Transactional
    public void createVariety(Integer categoryId, String varietyName, String description) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại!"));

        varietyRepository.save(Variety.builder()
                .category(category)
                .varietyName(requireText(varietyName, "Tên variety không được để trống!"))
                .description(blankToNull(description))
                .build());
    }

    @Transactional
    public void createSegment(String segmentName) {
        productSegmentRepository.save(ProductSegment.builder()
                .segmentName(requireText(segmentName, "Tên segment không được để trống!"))
                .build());
    }

    @Transactional
    public void createTag(String tagName) {
        tagRepository.save(Tag.builder()
                .tagName(requireText(tagName, "Tên tag không được để trống!"))
                .build());
    }

    @Transactional
    public void updateCategory(Integer categoryId, String categoryName, String description) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại!"));
        category.setCategoryName(requireText(categoryName, "Tên category không được để trống!"));
        category.setDescription(blankToNull(description));
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

        variety.setCategory(category);
        variety.setVarietyName(requireText(varietyName, "Tên variety không được để trống!"));
        variety.setDescription(blankToNull(description));
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
    public void updateSegment(Integer segmentId, String segmentName) {
        ProductSegment segment = productSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Segment không tồn tại!"));
        segment.setSegmentName(requireText(segmentName, "Tên segment không được để trống!"));
        productSegmentRepository.save(segment);
    }

    @Transactional
    public void deleteSegment(Integer segmentId) {
        if (productRepository.existsBySegmentSegmentId(segmentId)) {
            throw new RuntimeException("Không thể xóa segment đang được sản phẩm sử dụng.");
        }
        productSegmentRepository.deleteById(segmentId);
    }

    @Transactional
    public void updateTag(Integer tagId, String tagName) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag không tồn tại!"));
        tag.setTagName(requireText(tagName, "Tên tag không được để trống!"));
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
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
