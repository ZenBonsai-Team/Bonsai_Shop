package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductJournalEvent;
import com.example.bonsai_shop.entity.ProductJournalMedia;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.ProductJournalEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
// Service quản lý nhật ký chăm sóc/cập nhật trạng thái cây.
public class ProductJournalService {

    private static final int MAX_MEDIA_PER_UPLOAD = 10;
    private static final int MIN_INITIAL_IMAGE_COUNT = 3;
    private static final long MAX_IMAGE_SIZE_BYTES = 7L * 1024 * 1024;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private static final Set<String> VALID_EVENT_TYPES = Set.of(
            "PHOTO_UPDATE",
            "GROWTH",
            "PRUNING",
            "WIRING",
            "REPOTTING",
            "FERTILIZING",
            "WATERING",
            "PEST_TREATMENT",
            "HEALTH_CHECK",
            "ACQUISITION"
    );

    private final ArtisanProductService artisanProductService;
    private final ArtisanMediaStorageService mediaStorageService;
    private final ProductJournalEventRepository journalEventRepository;

    // Lấy nhật ký sản phẩm thuộc artisan hiện tại.
    public List<ProductJournalEvent> getMyProductEvents(String artisanEmail, Integer productId) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ensureNotSold(product);
        return journalEventRepository.findByProductOrderByEventDateDescEventIdDesc(product);
    }

    // Lấy các sự kiện nhật ký công khai cho khách xem.
    public List<ProductJournalEvent> getPublicEvents(Product product) {
        if (artisanProductService.isSold(product)) {
            return List.of();
        }
        return journalEventRepository.findByProductAndIsPublicTrueOrderByEventDateDescEventIdDesc(product);
    }

    @Transactional
    // Thêm sự kiện nhật ký mới kèm ảnh bắt buộc ban đầu.
    public void addEvent(String artisanEmail,
                         Integer productId,
                         LocalDate eventDate,
                         String eventType,
                         String title,
                         String description,
                         Boolean isPublic,
                         List<MultipartFile> files) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ensureNotSold(product);
        User artisan = artisanProductService.getArtisanUser(artisanEmail);

        LocalDate today = LocalDate.now();
        if (eventDate == null || !eventDate.equals(today)) {
            throw new RuntimeException("Ngày cập nhật chỉ được là ngày hiện tại.");
        }
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Vui lòng nhập tiêu đề cập nhật.");
        }
        String normalizedTitle = normalizeTitle(title);
        String normalizedDescription = normalizeDescription(description);
        List<MultipartFile> validFiles = getValidFiles(files);
        if (validFiles.size() < MIN_INITIAL_IMAGE_COUNT) {
            throw new RuntimeException("Vui lòng chọn tối thiểu 3 ảnh để tạo cập nhật nhật ký.");
        }
        validateUploadBatch(validFiles);

        ProductJournalEvent event = ProductJournalEvent.builder()
                .product(product)
                .createdBy(artisan)
                .eventDate(eventDate)
                .eventType(normalizeEventType(eventType))
                .title(normalizedTitle)
                .description(normalizedDescription)
                .isPublic(Boolean.TRUE.equals(isPublic))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        addUploadedMedia(event, validFiles);
        journalEventRepository.save(event);
    }

    @Transactional
    // Xóa sự kiện nhật ký nếu artisan còn được phép sửa sản phẩm.
    public void deleteEvent(String artisanEmail, Integer productId, Integer eventId) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ensureNotSold(product);
        ProductJournalEvent event = journalEventRepository.findByEventIdAndProduct(eventId, product)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cập nhật cây."));

        if (event.getMediaList() != null) {
            event.getMediaList().forEach(media -> mediaStorageService.deleteProductMedia(media.getMediaUrl()));
        }
        journalEventRepository.delete(event);
    }

    @Transactional
    // Cập nhật tiêu đề và mô tả sự kiện.
    public void updateEventText(String artisanEmail,
                                Integer productId,
                                Integer eventId,
                                String title,
                                String description) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ensureNotSold(product);
        ProductJournalEvent event = journalEventRepository.findByEventIdAndProduct(eventId, product)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cập nhật cây."));

        if (title == null || title.isBlank()) {
            throw new RuntimeException("Vui lòng nhập tiêu đề cập nhật.");
        }

        String normalizedTitle = normalizeTitle(title);
        String normalizedDescription = normalizeDescription(description);

        event.setTitle(normalizedTitle);
        event.setDescription(normalizedDescription);
        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
    }

    @Transactional
    // Bật/tắt công khai sự kiện nhật ký.
    public void updateEventVisibility(String artisanEmail, Integer productId, Integer eventId, Boolean isPublic) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ensureNotSold(product);
        ProductJournalEvent event = journalEventRepository.findByEventIdAndProduct(eventId, product)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cập nhật cây."));

        event.setIsPublic(Boolean.TRUE.equals(isPublic));
        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
    }

    @Transactional
    // Thêm ảnh vào sự kiện nhật ký hiện có.
    public void addMediaToEvent(String artisanEmail, Integer productId, Integer eventId, List<MultipartFile> files) {
        ProductJournalEvent event = getEditableEvent(artisanEmail, productId, eventId);

        List<MultipartFile> validFiles = getValidFiles(files);
        if (validFiles.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một ảnh để bổ sung.");
        }
        validateUploadBatch(validFiles);

        addUploadedMedia(event, validFiles);
        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
    }

    @Transactional
    // Đặt ảnh đại diện cho sự kiện nhật ký.
    public void setCoverMedia(String artisanEmail, Integer productId, Integer eventId, Integer mediaId) {
        ProductJournalEvent event = getEditableEvent(artisanEmail, productId, eventId);
        ProductJournalMedia coverMedia = findEventMedia(event, mediaId);

        int displayOrder = 1;
        coverMedia.setDisplayOrder(0);
        for (ProductJournalMedia media : event.getMediaList()) {
            if (!media.getMediaId().equals(mediaId)) {
                media.setDisplayOrder(displayOrder++);
            }
        }

        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
    }

    @Transactional
    // Thay thế file ảnh của media nhật ký.
    public void replaceMedia(String artisanEmail,
                             Integer productId,
                             Integer eventId,
                             Integer mediaId,
                             MultipartFile file) {
        ProductJournalEvent event = getEditableEvent(artisanEmail, productId, eventId);
        ProductJournalMedia media = findEventMedia(event, mediaId);
        List<MultipartFile> validFiles = getValidFiles(file == null ? null : List.of(file));
        if (validFiles.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ảnh thay thế.");
        }
        validateUploadBatch(validFiles);

        String oldMediaUrl = media.getMediaUrl();
        String newMediaUrl = mediaStorageService.storeProductMedia(validFiles.get(0));
        media.setMediaUrl(newMediaUrl);
        media.setMediaType("IMAGE");
        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
        mediaStorageService.deleteProductMedia(oldMediaUrl);
    }

    @Transactional
    // Xóa ảnh khỏi sự kiện và chuẩn hóa lại thứ tự.
    public void deleteMedia(String artisanEmail, Integer productId, Integer eventId, Integer mediaId) {
        ProductJournalEvent event = getEditableEvent(artisanEmail, productId, eventId);
        ProductJournalMedia media = findEventMedia(event, mediaId);

        event.getMediaList().remove(media);
        mediaStorageService.deleteProductMedia(media.getMediaUrl());
        normalizeMediaOrder(event);
        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
    }

    // Upload và gắn danh sách ảnh vào sự kiện.
    private void addUploadedMedia(ProductJournalEvent event, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        int displayOrder = event.getMediaList() == null ? 0 : event.getMediaList().stream()
                .map(ProductJournalMedia::getDisplayOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
        for (MultipartFile file : files) {
            String mediaUrl = mediaStorageService.storeProductMedia(file);
            event.getMediaList().add(ProductJournalMedia.builder()
                    .event(event)
                    .mediaUrl(mediaUrl)
                    .mediaType("IMAGE")
                    .displayOrder(displayOrder++)
                    .build());
        }
    }

    // Lấy sự kiện thuộc sản phẩm của artisan và đảm bảo còn sửa được.
    private ProductJournalEvent getEditableEvent(String artisanEmail, Integer productId, Integer eventId) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ensureNotSold(product);
        return journalEventRepository.findByEventIdAndProduct(eventId, product)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cập nhật cây."));
    }

    // Tìm media trong đúng sự kiện nhật ký.
    private ProductJournalMedia findEventMedia(ProductJournalEvent event, Integer mediaId) {
        if (mediaId == null || event.getMediaList() == null) {
            throw new RuntimeException("Không tìm thấy ảnh nhật ký.");
        }
        return event.getMediaList().stream()
                .filter(media -> mediaId.equals(media.getMediaId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh nhật ký."));
    }

    // Đánh lại displayOrder liên tục sau khi thay đổi media.
    private void normalizeMediaOrder(ProductJournalEvent event) {
        if (event.getMediaList() == null) {
            return;
        }
        for (int index = 0; index < event.getMediaList().size(); index++) {
            event.getMediaList().get(index).setDisplayOrder(index);
        }
    }

    // Lọc bỏ file null hoặc rỗng từ request upload.
    private List<MultipartFile> getValidFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    // Kiểm tra số lượng file trong một lần upload.
    private void validateUploadBatch(List<MultipartFile> files) {
        if (files.size() > MAX_MEDIA_PER_UPLOAD) {
            throw new RuntimeException("Mỗi lần chỉ được tải lên tối đa 10 ảnh nhật ký.");
        }
        files.forEach(this::validateImageFile);
    }

    // Validate file ảnh theo định dạng và dung lượng.
    private void validateImageFile(MultipartFile file) {
        if (!isImageFile(file)) {
            throw new RuntimeException("Nhật ký cây chỉ hỗ trợ tải ảnh. Vui lòng không tải video.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new RuntimeException("Ảnh nhật ký vượt quá dung lượng tối đa 7MB.");
        }
    }

    // Nhận diện ảnh qua content-type hoặc phần mở rộng.
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            return contentType.startsWith("image/");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }
        String normalizedFilename = filename.toLowerCase();
        return normalizedFilename.endsWith(".jpg")
                || normalizedFilename.endsWith(".jpeg")
                || normalizedFilename.endsWith(".png")
                || normalizedFilename.endsWith(".webp")
                || normalizedFilename.endsWith(".gif");
    }

    // Chuẩn hóa và validate tiêu đề nhật ký.
    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Vui lòng nhập tiêu đề cập nhật.");
        }
        String normalizedTitle = title.trim();
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            throw new RuntimeException("Tiêu đề nhật ký không được vượt quá " + MAX_TITLE_LENGTH + " ký tự.");
        }
        return normalizedTitle;
    }

    // Chuẩn hóa và validate mô tả nhật ký.
    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String normalizedDescription = description.trim();
        if (normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new RuntimeException("Ghi chú chăm sóc không được vượt quá " + MAX_DESCRIPTION_LENGTH + " ký tự.");
        }
        return normalizedDescription;
    }

    // Chặn sửa nhật ký khi sản phẩm đã bán.
    private void ensureNotSold(Product product) {
        if (artisanProductService.isSold(product)) {
            throw new RuntimeException("Sản phẩm đã bán nên không thể thao tác nhật ký cây.");
        }
    }

    // Chuẩn hóa loại sự kiện về tập giá trị cho phép.
    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return "PHOTO_UPDATE";
        }
        String normalized = eventType.trim().toUpperCase();
        return VALID_EVENT_TYPES.contains(normalized) ? normalized : "PHOTO_UPDATE";
    }
}
