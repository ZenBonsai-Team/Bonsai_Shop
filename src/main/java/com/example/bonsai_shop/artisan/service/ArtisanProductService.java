package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.artisan.dto.ArtisanProductFormDTO;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.ProductSegment;
import com.example.bonsai_shop.entity.ProductTag;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.product.repository.CategoryRepository;
import com.example.bonsai_shop.product.repository.ProductMediaRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSegmentRepository;
import com.example.bonsai_shop.product.repository.ProductTagRepository;
import com.example.bonsai_shop.product.repository.TagRepository;
import com.example.bonsai_shop.product.repository.VarietyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtisanProductService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> VALID_SHOT_TYPES = Set.of("FRONT", "BACK", "LEFT", "RIGHT", "DETAIL", "TRUNK", "BRANCH", "POT", "OVERVIEW");
    private static final int MAX_MEDIA_PER_UPLOAD = 10;
    private static final int MAX_TAGS_PER_PRODUCT = 12;
    private static final long MAX_IMAGE_SIZE_BYTES = 7L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 100L * 1024 * 1024;
    private static final BigDecimal MAX_PRODUCT_PRICE = new BigDecimal("999999999999");
    private static final int MAX_MEDIA_CAPTION_LENGTH = 255;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductSegmentRepository productSegmentRepository;
    private final ProductTagRepository productTagRepository;
    private final TagRepository tagRepository;
    private final VarietyRepository varietyRepository;
    private final UserRepository userRepository;
    private final ArtisanMediaStorageService mediaStorageService;

    public User getArtisanUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy artisan!"));
    }

    public List<Product> getMyProducts(String artisanEmail) {
        Integer artisanUserId = getArtisanUserId(artisanEmail);
        return productRepository.findByCreatedByUserIdOrderByCreatedAtDesc(artisanUserId);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getMyProduct(String artisanEmail, Integer productId) {
        Integer artisanUserId = getArtisanUserId(artisanEmail);
        return productRepository.findByProductIdAndCreatedByUserId(productId, artisanUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm thuộc artisan này!"));
    }

    private Integer getArtisanUserId(String artisanEmail) {
        return getArtisanUser(artisanEmail).getUserId();
    }

    @Transactional
    public Product createProduct(String artisanEmail, ArtisanProductFormDTO form) {
        User artisanUser = getArtisanUser(artisanEmail);
        Variety variety = varietyRepository.findById(form.getVarietyId())
                .orElseThrow(() -> new RuntimeException("Variety không tồn tại!"));
        ProductSegment segment = productSegmentRepository.findById(form.getSegmentId())
                .orElseThrow(() -> new RuntimeException("Segment không tồn tại!"));

        validateRequiredSpecifications(form.getAge(), form.getHeight(), form.getTrunkDiameter(), form.getStyle());
        validateProductPrice(form.getPrice());

        Product product = Product.builder()
                .createdBy(artisanUser)
                .variety(variety)
                .segment(segment)
                .productCode(createTemporaryProductCode())
                .productName(form.getProductName())
                .description(form.getDescription())
                .treeStory(form.getTreeStory())
                .age(form.getAge())
                .height(form.getHeight())
                .trunkDiameter(form.getTrunkDiameter())
                .style(form.getStyle())
                .price(form.getPrice())
                .isPublicPrice(isPublicPriceForSegment(segment))
                .productStatus("DRAFT")
                .isVisible(false)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        savedProduct.setProductCode(generateProductCode(savedProduct.getVariety()));
        Product productWithCode = productRepository.save(savedProduct);
        syncProductTags(productWithCode, form.getTagIds());
        return productWithCode;
    }

    @Transactional
    public void updateProduct(String artisanEmail, Integer productId, ArtisanProductFormDTO form) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureEditable(product);
        Variety variety = varietyRepository.findById(form.getVarietyId())
                .orElseThrow(() -> new RuntimeException("Variety không tồn tại!"));
        ProductSegment segment = productSegmentRepository.findById(form.getSegmentId())
                .orElseThrow(() -> new RuntimeException("Segment không tồn tại!"));

        validateRequiredSpecifications(form.getAge(), form.getHeight(), form.getTrunkDiameter(), form.getStyle());
        validateProductPrice(form.getPrice());

        product.setVariety(variety);
        product.setSegment(segment);
        product.setProductName(form.getProductName());
        product.setDescription(form.getDescription());
        product.setTreeStory(form.getTreeStory());
        product.setAge(form.getAge());
        product.setHeight(form.getHeight());
        product.setTrunkDiameter(form.getTrunkDiameter());
        product.setStyle(form.getStyle());
        product.setPrice(form.getPrice());
        product.setIsPublicPrice(isPublicPriceForSegment(segment));
        product.setProductStatus(form.getProductStatus());

        Product savedProduct = productRepository.save(product);
        syncProductTags(savedProduct, form.getTagIds());
    }

    public ArtisanProductFormDTO toFormDTO(Product product) {
        if (product == null) {
            return new ArtisanProductFormDTO();
        }

        return ArtisanProductFormDTO.builder()
                .varietyId(product.getVariety() == null ? null : product.getVariety().getVarietyId())
                .segmentId(product.getSegment() == null ? null : product.getSegment().getSegmentId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .treeStory(product.getTreeStory())
                .age(product.getAge())
                .height(product.getHeight())
                .trunkDiameter(product.getTrunkDiameter())
                .style(product.getStyle())
                .price(product.getPrice())
                .productStatus(product.getProductStatus())
                .tagIds(getSelectedTagIds(product))
                .build();
    }

    @Transactional
    public void deleteProduct(String artisanEmail, Integer productId) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureDraft(product);
        productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product)
                .forEach(media -> mediaStorageService.deleteProductMedia(media.getMediaUrl()));
        productRepository.delete(product);
    }

    public List<ProductMedia> getMedia(Product product) {
        List<ProductMedia> mediaList = productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product);
        ensureImageThumbnail(mediaList);
        return productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product);
    }

    @Transactional
    public void addMedia(String artisanEmail,
                         Integer productId,
                         MultipartFile file,
                         String slotType,
                         String caption,
                         Boolean isThumbnail) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureEditable(product);
        String mediaType = resolveMediaType(file);
        validateMediaFile(file, mediaType);
        if (Boolean.TRUE.equals(isThumbnail) && "VIDEO".equals(mediaType)) {
            throw new RuntimeException("Video không thể đặt làm media đại diện!");
        }
        String normalizedShotType = normalizeShotType(slotType, mediaType);
        String normalizedCaption = normalizeMediaCaption(caption);
        String mediaUrl = mediaStorageService.storeProductMedia(file);
        List<ProductMedia> existingMedia = productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product);
        boolean shouldSetThumbnail = "IMAGE".equals(mediaType) && (Boolean.TRUE.equals(isThumbnail) || existingMedia.isEmpty());

        if (shouldSetThumbnail) {
            existingMedia.forEach(media -> {
                media.setIsThumbnail(false);
                productMediaRepository.save(media);
            });
        }

        ProductMedia media = ProductMedia.builder()
                .product(product)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .slotType(normalizedShotType)
                .caption(normalizedCaption)
                .isThumbnail(shouldSetThumbnail)
                .displayOrder(getNextDisplayOrder(existingMedia))
                .build();

        productMediaRepository.save(media);
    }

    @Transactional
    public int addMediaBatch(String artisanEmail,
                             Integer productId,
                             List<MultipartFile> files,
                             List<String> mediaTypes,
                             List<String> slotTypes,
                             List<String> captions,
                             Integer thumbnailIndex) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureEditable(product);

        if (files == null || files.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một file media!");
        }
        // Chan batch qua lon truoc khi upload tung file len Cloudinary.
        if (files.size() > MAX_MEDIA_PER_UPLOAD) {
            throw new RuntimeException("Mỗi lần chỉ được tải lên tối đa " + MAX_MEDIA_PER_UPLOAD + " media!");
        }

        // Validate du lieu form song song voi List<MultipartFile> de moi file co metadata tuong ung.
        if (files.stream().anyMatch(file -> file == null || file.isEmpty())) {
            throw new RuntimeException("Vui lòng chọn file cho tất cả mục media!");
        }
        if (mediaTypes != null && mediaTypes.size() < files.size()) {
            throw new RuntimeException("Dữ liệu loại media không hợp lệ!");
        }
        if (thumbnailIndex != null && (thumbnailIndex < 0 || thumbnailIndex >= files.size())) {
            throw new RuntimeException("Dữ liệu ảnh đại diện không hợp lệ!");
        }

        List<ProductMedia> existingMedia = productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product);
        int nextDisplayOrder = getNextDisplayOrder(existingMedia);
        int selectedThumbnailIndex = thumbnailIndex == null ? findDefaultThumbnailIndex(files, mediaTypes, existingMedia.isEmpty()) : thumbnailIndex;

        if (selectedThumbnailIndex >= 0) {
            // Chi mot image duoc lam thumbnail, nen reset thumbnail cu truoc khi luu batch moi.
            existingMedia.forEach(media -> {
                media.setIsThumbnail(false);
                productMediaRepository.save(media);
            });
        }

        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            String mediaType = resolveMediaType(file, getListValue(mediaTypes, index));
            // Validate dung luong theo loai media truoc khi goi storage service upload len Cloudinary.
            validateMediaFile(file, mediaType);
            if (index == selectedThumbnailIndex && "VIDEO".equals(mediaType)) {
                throw new RuntimeException("Video không thể đặt làm media đại diện!");
            }
            String normalizedShotType = normalizeShotType(getListValue(slotTypes, index), mediaType);
            String normalizedCaption = normalizeMediaCaption(getListValue(captions, index));
            String mediaUrl = mediaStorageService.storeProductMedia(file);

            ProductMedia media = ProductMedia.builder()
                    .product(product)
                    .mediaUrl(mediaUrl)
                    .mediaType(mediaType)
                    .slotType(normalizedShotType)
                    .caption(normalizedCaption)
                    .isThumbnail(index == selectedThumbnailIndex)
                    .displayOrder(nextDisplayOrder + index)
                    .build();

            productMediaRepository.save(media);
        }

        return files.size();
    }

    @Transactional
    public void setThumbnail(String artisanEmail, Integer productId, Integer mediaId) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureEditable(product);
        ProductMedia selected = productMediaRepository.findByMediaIdAndProduct(mediaId, product)
                .orElseThrow(() -> new RuntimeException("Media không tồn tại!"));
        if (!"IMAGE".equals(selected.getMediaType())) {
            throw new RuntimeException("Chỉ ảnh mới có thể đặt làm ảnh đại diện!");
        }

        productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product)
                .forEach(media -> {
                    media.setIsThumbnail(media.getMediaId().equals(selected.getMediaId()));
                    productMediaRepository.save(media);
                });
    }

    @Transactional
    public void updateMediaOrder(String artisanEmail,
                                 Integer productId,
                                 List<Integer> mediaIds,
                                 List<Integer> displayOrders,
                                 List<String> slotTypes,
                                 List<String> captions) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureEditable(product);
        if (mediaIds == null || displayOrders == null || mediaIds.size() != displayOrders.size()) {
            throw new RuntimeException("Dữ liệu thứ tự media không hợp lệ!");
        }
        if (slotTypes != null && slotTypes.size() != mediaIds.size()) {
            throw new RuntimeException("Dữ liệu góc chụp media không hợp lệ!");
        }
        for (int index = 0; index < mediaIds.size(); index++) {
            ProductMedia media = productMediaRepository.findByMediaIdAndProduct(mediaIds.get(index), product)
                    .orElseThrow(() -> new RuntimeException("Media không tồn tại!"));
            media.setDisplayOrder(displayOrders.get(index) == null ? 1 : Math.max(displayOrders.get(index), 1));
            if (slotTypes != null) {
                media.setSlotType(normalizeShotType(slotTypes.get(index), media.getMediaType()));
            }
            if (captions != null) {
                media.setCaption(normalizeMediaCaption(captions.get(index)));
            }
            productMediaRepository.save(media);
        }
        ensureImageThumbnail(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product));
    }

    @Transactional
    public void deleteMedia(String artisanEmail, Integer productId, Integer mediaId) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureEditable(product);
        ProductMedia media = productMediaRepository.findByMediaIdAndProduct(mediaId, product)
                .orElseThrow(() -> new RuntimeException("Media không tồn tại!"));

        mediaStorageService.deleteProductMedia(media.getMediaUrl());
        productMediaRepository.delete(media);

        if (Boolean.TRUE.equals(media.getIsThumbnail())) {
            productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product).stream()
                    .filter(nextThumbnail -> "IMAGE".equals(nextThumbnail.getMediaType()))
                    .findFirst()
                    .ifPresent(nextThumbnail -> {
                        nextThumbnail.setIsThumbnail(true);
                        productMediaRepository.save(nextThumbnail);
                    });
        }
    }

    @Transactional
    public void publish(String artisanEmail, Integer productId) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureDraft(product);
        ensurePublishReady(product);
        product.setIsPublicPrice(isPublicPriceForSegment(product.getSegment()));
        product.setProductStatus("AVAILABLE");
        product.setIsVisible(true);
        productRepository.save(product);
    }

    @Transactional
    public void hideProduct(String artisanEmail, Integer productId) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureHideable(product);
        product.setIsVisible(false);
        productRepository.save(product);
    }

    @Transactional
    public void showProduct(String artisanEmail, Integer productId) {
        Product product = getMyProduct(artisanEmail, productId);
        ensureShowable(product);
        ensurePublishReady(product);
        product.setIsPublicPrice(isPublicPriceForSegment(product.getSegment()));
        product.setIsVisible(true);
        productRepository.save(product);
    }

    public List<Variety> getVarieties() {
        return varietyRepository.findAll();
    }

    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    public List<ProductSegment> getSegments() {
        return productSegmentRepository.findAll();
    }

    public List<Tag> getTags() {
        return tagRepository.findAll();
    }

    public List<Integer> getSelectedTagIds(Product product) {
        return productTagRepository.findByProduct(product).stream()
                .map(productTag -> productTag.getTag().getTagId())
                .toList();
    }

    public List<Tag> getProductTags(Product product) {
        return productTagRepository.findByProduct(product).stream()
                .map(ProductTag::getTag)
                .toList();
    }

    private void syncProductTags(Product product, List<Integer> tagIds) {
        productTagRepository.deleteByProduct(product);

        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        List<Integer> uniqueTagIds = tagIds.stream()
                .filter(tagId -> tagId != null)
                .distinct()
                .toList();
        if (uniqueTagIds.size() > MAX_TAGS_PER_PRODUCT) {
            throw new RuntimeException("Chỉ được chọn tối đa 12 thẻ cho một cây.");
        }

        List<Tag> tags = tagRepository.findAllById(uniqueTagIds);
        tags.stream()
                .map(tag -> ProductTag.builder()
                        .product(product)
                        .tag(tag)
                .build())
                .forEach(productTagRepository::save);
    }

    private void ensurePublishReady(Product product) {
        if (product.getProductName() == null || product.getProductName().isBlank()) {
            throw new RuntimeException("Vui lòng nhập tên sản phẩm trước khi publish.");
        }
        if (product.getVariety() == null) {
            throw new RuntimeException("Vui lòng chọn variety trước khi publish.");
        }
        if (product.getSegment() == null) {
            throw new RuntimeException("Vui lòng chọn segment trước khi publish.");
        }
        validateRequiredSpecifications(
                product.getAge(),
                product.getHeight(),
                product.getTrunkDiameter(),
                product.getStyle()
        );
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Vui lòng nhập giá sản phẩm hợp lệ trước khi publish.");
        }
        if (productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product).isEmpty()) {
            throw new RuntimeException("Cần ít nhất một ảnh hoặc video trước khi publish.");
        }
        ensureImageThumbnail(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product));
    }

    private void validateRequiredSpecifications(Integer age, Float height, Float trunkDiameter, String style) {
        if (age == null) {
            throw new RuntimeException("Vui lòng nhập tuổi cây.");
        }
        if (age <= 0) {
            throw new RuntimeException("Tuổi cây phải lớn hơn 0.");
        }
        if (age > 1000) {
            throw new RuntimeException("Tuổi cây không được vượt quá 1000 năm.");
        }
        if (height == null) {
            throw new RuntimeException("Vui lòng nhập chiều cao cây.");
        }
        if (height <= 0) {
            throw new RuntimeException("Chiều cao cây phải lớn hơn 0.");
        }
        if (height > 1000) {
            throw new RuntimeException("Chiều cao cây không được vượt quá 1000 cm.");
        }
        if (trunkDiameter == null) {
            throw new RuntimeException("Vui lòng nhập đường kính thân cây.");
        }
        if (trunkDiameter <= 0) {
            throw new RuntimeException("Đường kính thân cây phải lớn hơn 0.");
        }
        if (trunkDiameter > 500) {
            throw new RuntimeException("Đường kính thân cây không được vượt quá 500 cm.");
        }
        if (style == null || style.isBlank()) {
            throw new RuntimeException("Vui lòng nhập dáng cây.");
        }
        if (style.trim().length() > 100) {
            throw new RuntimeException("Dáng cây không được vượt quá 100 ký tự.");
        }
        if (!style.trim().matches("^[\\p{L}\\s'\\-]+$")) {
            throw new RuntimeException("Dáng cây chỉ được nhập chữ, khoảng trắng và dấu ' -.");
        }
    }

    private void validateProductPrice(BigDecimal price) {
        if (price == null) {
            throw new RuntimeException("Vui lòng nhập giá sản phẩm.");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá sản phẩm phải lớn hơn 0.");
        }
        if (price.compareTo(MAX_PRODUCT_PRICE) > 0) {
            throw new RuntimeException("Giá sản phẩm không được vượt quá 999.999.999.999 VNĐ.");
        }
        if (price.stripTrailingZeros().scale() > 0) {
            throw new RuntimeException("Giá sản phẩm chỉ được nhập số nguyên VNĐ.");
        }
    }

    private boolean isPublicPriceForSegment(ProductSegment segment) {
        return segment == null
                || segment.getSegmentName() == null
                || !"elite".equals(segment.getSegmentName().trim().toLowerCase(Locale.ROOT));
    }

    public boolean isSold(Product product) {
        return product != null && "SOLD".equalsIgnoreCase(product.getProductStatus());
    }

    public boolean isEditable(Product product) {
        if (product == null) {
            return false;
        }

        String status = product.getProductStatus();
        if ("SOLD".equalsIgnoreCase(status) || "RESERVED".equalsIgnoreCase(status)) {
            return false;
        }

        return "DRAFT".equalsIgnoreCase(status)
                || Boolean.FALSE.equals(product.getIsVisible());
    }

    public boolean isHideable(Product product) {
        return product != null
                && Boolean.TRUE.equals(product.getIsVisible())
                && ("AVAILABLE".equalsIgnoreCase(product.getProductStatus())
                || "SOLD".equalsIgnoreCase(product.getProductStatus()));
    }

    public boolean isVisible(Product product) {
        return product == null || product.getIsVisible() == null || Boolean.TRUE.equals(product.getIsVisible());
    }

    private void ensureNotSold(Product product) {
        if (isSold(product)) {
            throw new RuntimeException("Sản phẩm đã bán không thể chỉnh sửa.");
        }
    }

    private void ensureEditable(Product product) {
        if (!isEditable(product)) {
            throw new RuntimeException("Chỉ có thể sửa sản phẩm nháp hoặc đã ẩn.");
        }
    }

    private void ensureDraft(Product product) {
        if (product == null || !"DRAFT".equalsIgnoreCase(product.getProductStatus())) {
            throw new RuntimeException("Chỉ có thể xóa sản phẩm nháp.");
        }
    }

    private void ensureHideable(Product product) {
        if (!isHideable(product)) {
            throw new RuntimeException("Chỉ có thể ẩn sản phẩm đang được bán.");
        }
    }

    private void ensureShowable(Product product) {
        if (product == null || "DRAFT".equalsIgnoreCase(product.getProductStatus())) {
            throw new RuntimeException("Sản phẩm nháp cần đăng bán, không thể chỉ bật hiển thị.");
        }
        if ("RESERVED".equalsIgnoreCase(product.getProductStatus())) {
            throw new RuntimeException("Không thể hiện sản phẩm đang được đặt.");
        }
    }

    private String createTemporaryProductCode() {
        return "TMP-" + UUID.randomUUID();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String getListValue(List<String> values, int index) {
        return values == null || index >= values.size() ? null : values.get(index);
    }

    private String resolveMediaType(MultipartFile file) {
        return resolveMediaType(file, null);
    }

    private String resolveMediaType(MultipartFile file, String requestedMediaType) {
        if (requestedMediaType != null && !requestedMediaType.isBlank()) {
            String normalizedMediaType = requestedMediaType.trim().toUpperCase(Locale.ROOT);
            if ("VIDEO".equals(normalizedMediaType) || "IMAGE".equals(normalizedMediaType)) {
                // Uu tien loai media UI gui len, nhung chi chap nhan IMAGE/VIDEO.
                return normalizedMediaType;
            }
        }

        // Fallback theo Content-Type/extension de nhan dien video khi trinh duyet gui MIME khong day du.
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("video/")) {
            return "VIDEO";
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String normalizedFilename = filename.toLowerCase(Locale.ROOT);
            if (normalizedFilename.endsWith(".mp4") || normalizedFilename.endsWith(".webm")) {
                return "VIDEO";
            }
        }

        return "IMAGE";
    }

    private void validateMediaFile(MultipartFile file, String mediaType) {
        long maxSize = "VIDEO".equals(mediaType) ? MAX_VIDEO_SIZE_BYTES : MAX_IMAGE_SIZE_BYTES;
        // Service validate lai dung luong de khong phu thuoc hoan toan vao JavaScript phia client.
        if (file.getSize() > maxSize) {
            throw new RuntimeException(("VIDEO".equals(mediaType) ? "Video" : "Ảnh")
                    + " vượt quá dung lượng tối đa "
                    + formatMegabytes(maxSize)
                    + "MB!");
        }
    }

    private String normalizeMediaCaption(String caption) {
        String normalizedCaption = blankToNull(caption);
        if (normalizedCaption != null && normalizedCaption.length() > MAX_MEDIA_CAPTION_LENGTH) {
            throw new RuntimeException("Chú thích media không được vượt quá " + MAX_MEDIA_CAPTION_LENGTH + " ký tự!");
        }
        return normalizedCaption;
    }

    private int findDefaultThumbnailIndex(List<MultipartFile> files, List<String> mediaTypes, boolean shouldSelectDefault) {
        if (!shouldSelectDefault) {
            return -1;
        }

        for (int index = 0; index < files.size(); index++) {
            if ("IMAGE".equals(resolveMediaType(files.get(index), getListValue(mediaTypes, index)))) {
                return index;
            }
        }

        return -1;
    }

    private void ensureImageThumbnail(List<ProductMedia> mediaList) {
        boolean hasVideoThumbnail = mediaList.stream()
                .anyMatch(media -> Boolean.TRUE.equals(media.getIsThumbnail()) && "VIDEO".equals(media.getMediaType()));
        boolean hasImageThumbnail = mediaList.stream()
                .anyMatch(media -> Boolean.TRUE.equals(media.getIsThumbnail()) && "IMAGE".equals(media.getMediaType()));

        if (!hasVideoThumbnail && hasImageThumbnail) {
            return;
        }

        ProductMedia firstImage = mediaList.stream()
                .filter(media -> "IMAGE".equals(media.getMediaType()))
                .findFirst()
                .orElse(null);

        if (firstImage == null) {
            mediaList.stream()
                    .filter(media -> Boolean.TRUE.equals(media.getIsThumbnail()))
                    .forEach(media -> {
                        media.setIsThumbnail(false);
                        productMediaRepository.save(media);
                    });
            return;
        }

        mediaList.forEach(media -> {
            media.setIsThumbnail(media.getMediaId().equals(firstImage.getMediaId()));
            productMediaRepository.save(media);
        });
    }

    private long formatMegabytes(long bytes) {
        return bytes / 1024 / 1024;
    }

    private Integer getNextDisplayOrder(List<ProductMedia> existingMedia) {
        return existingMedia.stream()
                .map(ProductMedia::getDisplayOrder)
                .filter(displayOrder -> displayOrder != null)
                .max(Integer::compareTo)
                .map(displayOrder -> displayOrder + 1)
                .orElse(1);
    }

    private String normalizeShotType(String shotType, String mediaType) {
        if ("VIDEO".equals(mediaType)) {
            return null;
        }

        if (shotType == null || shotType.isBlank()) {
            throw new RuntimeException("Vui lòng chọn góc chụp!");
        }

        String normalizedShotType = shotType.trim().toUpperCase(Locale.ROOT);
        if (!VALID_SHOT_TYPES.contains(normalizedShotType)) {
            throw new RuntimeException("Góc chụp không hợp lệ!");
        }

        return normalizedShotType;
    }

    private String generateProductCode(Variety variety) {
        String categoryPart = abbreviate(variety.getCategory().getCategoryName());
        String varietyPart = abbreviate(variety.getVarietyName());
        String productCode;

        do {
            productCode = String.format("BSMS-%s-%s-%s", categoryPart, varietyPart, randomCodeSuffix());
        } while (productRepository.existsByProductCode(productCode));

        return productCode;
    }

    private String abbreviate(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");

        if (normalized.isBlank()) {
            return "GEN";
        }

        String[] words = normalized.split(" ");
        if (words.length >= 2) {
            StringBuilder initials = new StringBuilder();
            for (String word : words) {
                initials.append(word.charAt(0));
                if (initials.length() == 4) {
                    break;
                }
            }
            return initials.toString();
        }

        return normalized.length() <= 4 ? normalized : normalized.substring(0, 4);
    }

    private String randomCodeSuffix() {
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            suffix.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return suffix.toString();
    }
}


