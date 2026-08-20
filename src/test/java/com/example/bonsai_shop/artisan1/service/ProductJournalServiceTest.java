package com.example.bonsai_shop.artisan1.service;

import com.example.bonsai_shop.artisan.service.ArtisanMediaStorageService;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.artisan.service.ProductJournalService;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductJournalEvent;
import com.example.bonsai_shop.entity.ProductJournalMedia;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.ProductJournalEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductJournalServiceTest {

    private ArtisanProductService artisanProductService;
    private ArtisanMediaStorageService mediaStorageService;
    private ProductJournalEventRepository journalEventRepository;
    private ProductJournalService productJournalService;

    @BeforeEach
    void setUp() {
        artisanProductService = mock(ArtisanProductService.class);
        mediaStorageService = mock(ArtisanMediaStorageService.class);
        journalEventRepository = mock(ProductJournalEventRepository.class);

        productJournalService = new ProductJournalService(
                artisanProductService,
                mediaStorageService,
                journalEventRepository
        );
    }

    @Test
    void getMyProductEvents_WhenProductIsNotSold_ShouldReturnOrderedEvents() {
        Product product = product(101, "AVAILABLE");
        List<ProductJournalEvent> expectedEvents = List.of(event(1, product), event(2, product));

        when(artisanProductService.getMyProduct("artisan@test.com", 101)).thenReturn(product);
        when(artisanProductService.isSold(product)).thenReturn(false);
        when(journalEventRepository.findByProductOrderByEventDateDescEventIdDesc(product))
                .thenReturn(expectedEvents);

        List<ProductJournalEvent> result = productJournalService.getMyProductEvents("artisan@test.com", 101);

        assertThat(result).isEqualTo(expectedEvents);
        verify(journalEventRepository).findByProductOrderByEventDateDescEventIdDesc(product);
    }

    @Test
    void getMyProductEvents_WhenProductIsSold_ShouldRejectJournalAccess() {
        Product product = product(101, "SOLD");

        when(artisanProductService.getMyProduct("artisan@test.com", 101)).thenReturn(product);
        when(artisanProductService.isSold(product)).thenReturn(true);

        assertThatThrownBy(() -> productJournalService.getMyProductEvents("artisan@test.com", 101))
                .isInstanceOf(RuntimeException.class);

        verify(journalEventRepository, never()).findByProductOrderByEventDateDescEventIdDesc(any(Product.class));
    }

    @Test
    void addEvent_WhenRequestIsValidWithThreeImages_ShouldCreateEventAndUploadMedia() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);
        List<MultipartFile> images = images(3);

        mockEditableProduct(product, artisan);
        mockStoredUrls(images);

        productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                "New leaves",
                true,
                images
        );

        ArgumentCaptor<ProductJournalEvent> eventCaptor = ArgumentCaptor.forClass(ProductJournalEvent.class);
        verify(journalEventRepository).save(eventCaptor.capture());
        ProductJournalEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getProduct()).isEqualTo(product);
        assertThat(savedEvent.getCreatedBy()).isEqualTo(artisan);
        assertThat(savedEvent.getEventType()).isEqualTo("GROWTH");
        assertThat(savedEvent.getTitle()).isEqualTo("Growth update");
        assertThat(savedEvent.getDescription()).isEqualTo("New leaves");
        assertThat(savedEvent.getIsPublic()).isTrue();
        assertThat(savedEvent.getMediaList()).hasSize(3);
        assertThat(savedEvent.getMediaList())
                .extracting(ProductJournalMedia::getDisplayOrder)
                .containsExactly(0, 1, 2);
    }

    @Test
    void addEvent_WhenEventDateIsNull_ShouldRejectBeforeUploadAndSave() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                null,
                "GROWTH",
                "Growth update",
                null,
                true,
                images(3)
        )).isInstanceOf(RuntimeException.class);

        verifyNoUploadOrSave();
    }

    @Test
    void addEvent_WhenTitleExceedsMaximumLength_ShouldRejectBeforeUploadAndSave() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "a".repeat(101),
                null,
                true,
                images(3)
        )).isInstanceOf(RuntimeException.class);

        verifyNoUploadOrSave();
    }

    @Test
    void addEvent_WhenDescriptionExceedsMaximumLength_ShouldRejectBeforeUploadAndSave() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                "a".repeat(501),
                true,
                images(3)
        )).isInstanceOf(RuntimeException.class);

        verifyNoUploadOrSave();
    }

    @Test
    void addEvent_WhenFewerThanThreeValidImagesProvided_ShouldRejectBeforeUploadAndSave() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                null,
                true,
                images(2)
        )).isInstanceOf(RuntimeException.class);

        verifyNoUploadOrSave();
    }

    @Test
    void addEvent_WhenMoreThanTenImagesProvided_ShouldRejectBeforeUploadAndSave() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                null,
                true,
                images(11)
        )).isInstanceOf(RuntimeException.class);

        verifyNoUploadOrSave();
    }

    @Test
    void addEvent_WhenFileIsNotImage_ShouldRejectBeforeUploadAndSave() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                null,
                true,
                List.of(image("front.jpg", 1024), image("back.jpg", 1024), file("clip.mp4", "video/mp4", 1024))
        )).isInstanceOf(RuntimeException.class);

        verifyNoUploadOrSave();
    }

    @Test
    void addEvent_WhenEventTypeInvalid_ShouldDefaultToPhotoUpdate() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);
        List<MultipartFile> images = images(3);

        mockEditableProduct(product, artisan);
        mockStoredUrls(images);

        productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "UNKNOWN",
                "Growth update",
                null,
                true,
                images
        );

        ArgumentCaptor<ProductJournalEvent> eventCaptor = ArgumentCaptor.forClass(ProductJournalEvent.class);
        verify(journalEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("PHOTO_UPDATE");
    }

    @Test
    void addEvent_WhenVisibilityIsNull_ShouldSaveEventAsPrivate() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);
        List<MultipartFile> images = images(3);

        mockEditableProduct(product, artisan);
        mockStoredUrls(images);

        productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                null,
                null,
                images
        );

        ArgumentCaptor<ProductJournalEvent> eventCaptor = ArgumentCaptor.forClass(ProductJournalEvent.class);
        verify(journalEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getIsPublic()).isFalse();
    }

    @Test
    void addMediaToEvent_WhenImagesAreValid_ShouldAppendMediaFromCurrentMaxDisplayOrder() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        event.getMediaList().add(journalMedia(event, 11, "https://cdn.test/old.jpg", 5));
        List<MultipartFile> newImages = images(2);

        mockExistingEditableEvent(product, event);
        mockStoredUrls(newImages);

        productJournalService.addMediaToEvent("artisan@test.com", 101, 1, newImages);

        assertThat(event.getMediaList()).hasSize(3);
        assertThat(event.getMediaList())
                .extracting(ProductJournalMedia::getDisplayOrder)
                .containsExactly(5, 6, 7);
        verify(journalEventRepository).save(event);
    }

    @Test
    void setCoverMedia_WhenSecondMediaSelected_ShouldMoveItToFirstDisplayOrder() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        ProductJournalMedia first = journalMedia(event, 11, "https://cdn.test/first.jpg", 0);
        ProductJournalMedia second = journalMedia(event, 12, "https://cdn.test/second.jpg", 1);
        event.getMediaList().add(first);
        event.getMediaList().add(second);

        mockExistingEditableEvent(product, event);

        productJournalService.setCoverMedia("artisan@test.com", 101, 1, 12);

        assertThat(second.getDisplayOrder()).isZero();
        assertThat(first.getDisplayOrder()).isEqualTo(1);
        assertThat(event.getUpdatedAt()).isNotNull();
        verify(journalEventRepository).save(event);
    }

    @Test
    void replaceMedia_WhenReplacementImageIsValid_ShouldStoreNewImageAndDeleteOldImage() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        ProductJournalMedia media = journalMedia(event, 11, "https://cdn.test/old.jpg", 0);
        MultipartFile replacement = image("new.jpg", 1024);
        event.getMediaList().add(media);

        mockExistingEditableEvent(product, event);
        when(mediaStorageService.storeProductMedia(replacement)).thenReturn("https://cdn.test/new.jpg");

        productJournalService.replaceMedia("artisan@test.com", 101, 1, 11, replacement);

        assertThat(media.getMediaUrl()).isEqualTo("https://cdn.test/new.jpg");
        assertThat(media.getMediaType()).isEqualTo("IMAGE");
        assertThat(event.getUpdatedAt()).isNotNull();
        verify(journalEventRepository).save(event);
        verify(mediaStorageService).deleteProductMedia("https://cdn.test/old.jpg");
    }

    @Test
    void writeOperations_WhenProductIsSold_ShouldRejectBeforeMutation() {
        Product product = product(101, "SOLD");

        when(artisanProductService.getMyProduct("artisan@test.com", 101)).thenReturn(product);
        when(artisanProductService.isSold(product)).thenReturn(true);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                null,
                true,
                images(3)
        )).isInstanceOf(RuntimeException.class);

        verify(journalEventRepository, never()).findByEventIdAndProduct(any(Integer.class), any(Product.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
        verify(journalEventRepository, never()).delete(any(ProductJournalEvent.class));
        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
    }

    private void mockEditableProduct(Product product, User artisan) {
        when(artisanProductService.getMyProduct("artisan@test.com", 101)).thenReturn(product);
        when(artisanProductService.isSold(product)).thenReturn(false);
        when(artisanProductService.getArtisanUser("artisan@test.com")).thenReturn(artisan);
    }

    private void mockExistingEditableEvent(Product product, ProductJournalEvent event) {
        when(artisanProductService.getMyProduct("artisan@test.com", 101)).thenReturn(product);
        when(artisanProductService.isSold(product)).thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product)).thenReturn(Optional.of(event));
    }

    private void mockStoredUrls(List<MultipartFile> files) {
        for (int index = 0; index < files.size(); index++) {
            when(mediaStorageService.storeProductMedia(files.get(index)))
                    .thenReturn("https://cdn.test/image-" + index + ".jpg");
        }
    }

    private void verifyNoUploadOrSave() {
        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    private Product product(Integer productId, String status) {
        return Product.builder()
                .productId(productId)
                .productName("Bonsai " + productId)
                .productStatus(status)
                .build();
    }

    private User artisan(Integer userId) {
        return User.builder()
                .userId(userId)
                .email("artisan@test.com")
                .fullName("Artisan")
                .build();
    }

    private ProductJournalEvent event(Integer eventId, Product product) {
        return ProductJournalEvent.builder()
                .eventId(eventId)
                .product(product)
                .eventDate(LocalDate.now())
                .eventType("GROWTH")
                .title("Growth update")
                .mediaList(new java.util.ArrayList<>())
                .build();
    }

    private ProductJournalMedia journalMedia(ProductJournalEvent event,
                                             Integer mediaId,
                                             String mediaUrl,
                                             Integer displayOrder) {
        return ProductJournalMedia.builder()
                .mediaId(mediaId)
                .event(event)
                .mediaUrl(mediaUrl)
                .mediaType("IMAGE")
                .displayOrder(displayOrder)
                .build();
    }

    private List<MultipartFile> images(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> image("image-" + index + ".jpg", 1024))
                .toList();
    }

    private MultipartFile image(String filename, long size) {
        return file(filename, "image/jpeg", size);
    }

    private MultipartFile file(String originalFilename, String contentType, long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(originalFilename);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getSize()).thenReturn(size);
        when(file.isEmpty()).thenReturn(false);
        return file;
    }
}
