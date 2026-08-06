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
public class ProductJournalService {

    private static final int MAX_MEDIA_PER_UPLOAD = 10;
    private static final long MAX_IMAGE_SIZE_BYTES = 7L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 100L * 1024 * 1024;

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

    public List<ProductJournalEvent> getMyProductEvents(String artisanEmail, Integer productId) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ensureNotSold(product);
        return journalEventRepository.findByProductOrderByEventDateDescEventIdDesc(product);
    }

    public List<ProductJournalEvent> getPublicEvents(Product product) {
        if (artisanProductService.isSold(product)) {
            return List.of();
        }
        return journalEventRepository.findByProductAndIsPublicTrueOrderByEventDateDescEventIdDesc(product);
    }

    @Transactional
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
        if (!hasUploadedFiles(files)) {
            throw new RuntimeException("Vui lòng chọn ít nhất một ảnh/video để tạo cập nhật.");
        }

        ProductJournalEvent event = ProductJournalEvent.builder()
                .product(product)
                .createdBy(artisan)
                .eventDate(eventDate)
                .eventType(normalizeEventType(eventType))
                .title(title.trim())
                .description(description)
                .isPublic(Boolean.TRUE.equals(isPublic))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        addUploadedMedia(event, files);
        journalEventRepository.save(event);
    }

    @Transactional
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

        event.setTitle(title.trim());
        event.setDescription(description);
        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
    }

    @Transactional
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
    public void addMediaToEvent(String artisanEmail, Integer productId, Integer eventId, List<MultipartFile> files) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ensureNotSold(product);
        ProductJournalEvent event = journalEventRepository.findByEventIdAndProduct(eventId, product)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cập nhật cây."));

        if (!hasUploadedFiles(files)) {
            throw new RuntimeException("Vui lòng chọn ít nhất một ảnh/video để bổ sung.");
        }

        addUploadedMedia(event, files);
        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
    }

    private void addUploadedMedia(ProductJournalEvent event, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (validFiles.isEmpty()) {
            return;
        }
        if (validFiles.size() > MAX_MEDIA_PER_UPLOAD) {
            throw new RuntimeException("Má»—i láº§n chá»‰ Ä‘Æ°á»£c táº£i lÃªn tá»‘i Ä‘a 10 media nháº­t kÃ½.");
        }
        validFiles.forEach(file -> validateMediaFile(file, resolveMediaType(file)));

        int displayOrder = event.getMediaList() == null ? 0 : event.getMediaList().stream()
                .map(ProductJournalMedia::getDisplayOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
        for (MultipartFile file : validFiles) {
            String mediaUrl = mediaStorageService.storeProductMedia(file);
            String mediaType = resolveMediaType(file);
            event.getMediaList().add(ProductJournalMedia.builder()
                    .event(event)
                    .mediaUrl(mediaUrl)
                    .mediaType(mediaType)
                    .displayOrder(displayOrder++)
                    .build());
        }
    }

    private boolean hasUploadedFiles(List<MultipartFile> files) {
        return files != null && files.stream().anyMatch(file -> file != null && !file.isEmpty());
    }

    private void validateMediaFile(MultipartFile file, String mediaType) {
        long maxSize = "VIDEO".equals(mediaType) ? MAX_VIDEO_SIZE_BYTES : MAX_IMAGE_SIZE_BYTES;
        if (file.getSize() > maxSize) {
            String label = "VIDEO".equals(mediaType) ? "Video" : "Ảnh";
            String sizeLabel = "VIDEO".equals(mediaType) ? "100MB" : "7MB";
            throw new RuntimeException(label + " nhật ký vượt quá dung lượng tối đa " + sizeLabel + ".");
        }
    }

    private String resolveMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean isVideo = (contentType != null && contentType.startsWith("video/"))
                || (filename != null && (filename.toLowerCase().endsWith(".mp4")
                || filename.toLowerCase().endsWith(".webm")));
        return isVideo ? "VIDEO" : "IMAGE";
    }

    private void ensureNotSold(Product product) {
        if (artisanProductService.isSold(product)) {
            throw new RuntimeException("Sản phẩm đã bán nên không thể thao tác nhật ký cây.");
        }
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return "PHOTO_UPDATE";
        }
        String normalized = eventType.trim().toUpperCase();
        return VALID_EVENT_TYPES.contains(normalized) ? normalized : "PHOTO_UPDATE";
    }
}
