package com.example.bonsai_shop.seller.service;

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
public class SellerProductService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> VALID_SHOT_TYPES = Set.of("FRONT", "BACK", "LEFT", "RIGHT", "TOP", "DETAIL", "ROOT", "TRUNK", "BRANCH", "POT", "OVERVIEW");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductSegmentRepository productSegmentRepository;
    private final ProductTagRepository productTagRepository;
    private final TagRepository tagRepository;
    private final VarietyRepository varietyRepository;
    private final UserRepository userRepository;
    private final SellerMediaStorageService mediaStorageService;

    public User getSeller(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy seller!"));
    }

    public List<Product> getMyProducts(String sellerEmail) {
        Integer sellerId = getSellerId(sellerEmail);
        return productRepository.findBySellerUserIdOrderByCreatedAtDesc(sellerId);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getMyProduct(String sellerEmail, Integer productId) {
        Integer sellerId = getSellerId(sellerEmail);
        return productRepository.findByProductIdAndSellerUserId(productId, sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm thuộc seller này!"));
    }

    private Integer getSellerId(String sellerEmail) {
        return getSeller(sellerEmail).getUserId();
    }

    @Transactional
    public Product createProduct(String sellerEmail,
                                 Integer varietyId,
                                 Integer segmentId,
                                 String productName,
                                 String description,
                                 Integer age,
                                 Float height,
                                 Float trunkDiameter,
                                 String style,
                                 BigDecimal price,
                                 Boolean isPublicPrice,
                                 String productStatus,
                                 List<Integer> tagIds) {
        User seller = getSeller(sellerEmail);
        Variety variety = varietyRepository.findById(varietyId)
                .orElseThrow(() -> new RuntimeException("Variety không tồn tại!"));
        ProductSegment segment = productSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Segment không tồn tại!"));

        validateRequiredSpecifications(age, height, trunkDiameter, style);

        Product product = Product.builder()
                .seller(seller)
                .variety(variety)
                .segment(segment)
                .productCode(createTemporaryProductCode())
                .productName(productName)
                .description(description)
                .age(age)
                .height(height)
                .trunkDiameter(trunkDiameter)
                .style(style)
                .price(price)
                .isPublicPrice(Boolean.TRUE.equals(isPublicPrice))
                .productStatus(productStatus == null || productStatus.isBlank() ? "DRAFT" : productStatus)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        savedProduct.setProductCode(generateProductCode(savedProduct.getVariety()));
        Product productWithCode = productRepository.save(savedProduct);
        syncProductTags(productWithCode, tagIds);
        return productWithCode;
    }

    @Transactional
    public void updateProduct(String sellerEmail,
                                 Integer productId,
                                 Integer varietyId,
                                 Integer segmentId,
                                 String productName,
                                 String description,
                                 Integer age,
                                 Float height,
                                 Float trunkDiameter,
                                 String style,
                                 BigDecimal price,
                                 Boolean isPublicPrice,
                                 String productStatus,
                                 List<Integer> tagIds) {
        Product product = getMyProduct(sellerEmail, productId);
        ensureEditable(product);
        Variety variety = varietyRepository.findById(varietyId)
                .orElseThrow(() -> new RuntimeException("Variety không tồn tại!"));
        ProductSegment segment = productSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Segment không tồn tại!"));

        validateRequiredSpecifications(age, height, trunkDiameter, style);

        product.setVariety(variety);
        product.setSegment(segment);
        product.setProductName(productName);
        product.setDescription(description);
        product.setAge(age);
        product.setHeight(height);
        product.setTrunkDiameter(trunkDiameter);
        product.setStyle(style);
        product.setPrice(price);
        product.setIsPublicPrice(Boolean.TRUE.equals(isPublicPrice));
        product.setProductStatus(productStatus);

        Product savedProduct = productRepository.save(product);
        syncProductTags(savedProduct, tagIds);
    }

    @Transactional
    public void deleteProduct(String sellerEmail, Integer productId) {
        Product product = getMyProduct(sellerEmail, productId);
        ensureDraft(product);
        productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product)
                .forEach(media -> mediaStorageService.deleteProductMedia(media.getMediaUrl()));
        productRepository.delete(product);
    }

    public List<ProductMedia> getMedia(Product product) {
        return productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product);
    }

    @Transactional
    public void addMedia(String sellerEmail,
                         Integer productId,
                         MultipartFile file,
                         String slotType,
                         String caption,
                         Boolean isThumbnail) {
        Product product = getMyProduct(sellerEmail, productId);
        ensureEditable(product);
        String mediaUrl = mediaStorageService.storeProductMedia(file);
        String contentType = file.getContentType();
        String mediaType = contentType != null && contentType.startsWith("video/") ? "VIDEO" : "IMAGE";
        String normalizedShotType = normalizeShotType(slotType, mediaType);
        List<ProductMedia> existingMedia = productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product);

        if (Boolean.TRUE.equals(isThumbnail)) {
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
                .caption(caption)
                .isThumbnail(Boolean.TRUE.equals(isThumbnail))
                .displayOrder(getNextDisplayOrder(existingMedia))
                .build();

        productMediaRepository.save(media);
    }

    @Transactional
    public void setThumbnail(String sellerEmail, Integer productId, Integer mediaId) {
        Product product = getMyProduct(sellerEmail, productId);
        ensureEditable(product);
        ProductMedia selected = productMediaRepository.findByMediaIdAndProduct(mediaId, product)
                .orElseThrow(() -> new RuntimeException("Media không tồn tại!"));

        productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product)
                .forEach(media -> {
                    media.setIsThumbnail(media.getMediaId().equals(selected.getMediaId()));
                    productMediaRepository.save(media);
                });
    }

    @Transactional
    public void updateMediaOrder(String sellerEmail,
                                 Integer productId,
                                 List<Integer> mediaIds,
                                 List<Integer> displayOrders) {
        Product product = getMyProduct(sellerEmail, productId);
        ensureEditable(product);
        if (mediaIds == null || displayOrders == null || mediaIds.size() != displayOrders.size()) {
            throw new RuntimeException("Dữ liệu thứ tự media không hợp lệ!");
        }

        for (int index = 0; index < mediaIds.size(); index++) {
            ProductMedia media = productMediaRepository.findByMediaIdAndProduct(mediaIds.get(index), product)
                    .orElseThrow(() -> new RuntimeException("Media không tồn tại!"));
            media.setDisplayOrder(displayOrders.get(index) == null ? 0 : displayOrders.get(index));
            productMediaRepository.save(media);
        }
    }

    @Transactional
    public void deleteMedia(String sellerEmail, Integer productId, Integer mediaId) {
        Product product = getMyProduct(sellerEmail, productId);
        ensureEditable(product);
        ProductMedia media = productMediaRepository.findByMediaIdAndProduct(mediaId, product)
                .orElseThrow(() -> new RuntimeException("Media không tồn tại!"));

        mediaStorageService.deleteProductMedia(media.getMediaUrl());
        productMediaRepository.delete(media);
    }

    @Transactional
    public void publish(String sellerEmail, Integer productId) {
        Product product = getMyProduct(sellerEmail, productId);
        ensureEditable(product);
        ensurePublishReady(product);
        product.setProductStatus("AVAILABLE");
        productRepository.save(product);
    }

    @Transactional
    public void hideProduct(String sellerEmail, Integer productId) {
        Product product = getMyProduct(sellerEmail, productId);
        ensureHideable(product);
        product.setProductStatus("HIDDEN");
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

        List<Tag> tags = tagRepository.findAllById(tagIds);
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
    }

    private void validateRequiredSpecifications(Integer age, Float height, Float trunkDiameter, String style) {
        if (age == null) {
            throw new RuntimeException("Vui lòng nhập tuổi cây.");
        }
        if (height == null) {
            throw new RuntimeException("Vui lòng nhập chiều cao cây.");
        }
        if (trunkDiameter == null) {
            throw new RuntimeException("Vui lòng nhập đường kính thân cây.");
        }
        if (style == null || style.isBlank()) {
            throw new RuntimeException("Vui lòng nhập style cây.");
        }
    }
    public boolean isSold(Product product) {
        return product != null && "SOLD".equalsIgnoreCase(product.getProductStatus());
    }

    public boolean isEditable(Product product) {
        return product != null
                && ("DRAFT".equalsIgnoreCase(product.getProductStatus())
                || "HIDDEN".equalsIgnoreCase(product.getProductStatus()));
    }

    public boolean isHideable(Product product) {
        return product != null && "AVAILABLE".equalsIgnoreCase(product.getProductStatus());
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

    private String createTemporaryProductCode() {
        return "TMP-" + UUID.randomUUID();
    }

    private Integer getNextDisplayOrder(List<ProductMedia> existingMedia) {
        return existingMedia.stream()
                .map(ProductMedia::getDisplayOrder)
                .filter(displayOrder -> displayOrder != null)
                .max(Integer::compareTo)
                .map(displayOrder -> displayOrder + 1)
                .orElse(0);
    }

    private String normalizeShotType(String shotType, String mediaType) {
        if ("VIDEO".equals(mediaType) && (shotType == null || shotType.isBlank())) {
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
