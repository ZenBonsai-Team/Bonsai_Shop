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
        return journalEventRepository.findByProductOrderByEventDateDescEventIdDesc(product);
    }

    public List<ProductJournalEvent> getPublicEvents(Product product) {
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
        User artisan = artisanProductService.getArtisanUser(artisanEmail);

        LocalDate today = LocalDate.now();
        if (eventDate == null || !eventDate.equals(today)) {
            throw new RuntimeException("Ngày cập nhật chỉ được là ngày hiện tại.");
        }
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Vui lòng nhập tiêu đề cập nhật.");
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
        ProductJournalEvent event = journalEventRepository.findByEventIdAndProduct(eventId, product)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cập nhật cây."));

        event.setIsPublic(Boolean.TRUE.equals(isPublic));
        event.setUpdatedAt(LocalDateTime.now());
        journalEventRepository.save(event);
    }

    @Transactional
    public void addMediaToEvent(String artisanEmail, Integer productId, Integer eventId, List<MultipartFile> files) {
        Product product = artisanProductService.getMyProduct(artisanEmail, productId);
        ProductJournalEvent event = journalEventRepository.findByEventIdAndProduct(eventId, product)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cập nhật cây."));

        if (files == null || files.stream().allMatch(file -> file == null || file.isEmpty())) {
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

        int displayOrder = event.getMediaList() == null ? 0 : event.getMediaList().stream()
                .map(ProductJournalMedia::getDisplayOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String mediaUrl = mediaStorageService.storeProductMedia(file);
            String contentType = file.getContentType();
            String mediaType = contentType != null && contentType.startsWith("video/") ? "VIDEO" : "IMAGE";
            event.getMediaList().add(ProductJournalMedia.builder()
                    .event(event)
                    .mediaUrl(mediaUrl)
                    .mediaType(mediaType)
                    .displayOrder(displayOrder++)
                    .build());
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