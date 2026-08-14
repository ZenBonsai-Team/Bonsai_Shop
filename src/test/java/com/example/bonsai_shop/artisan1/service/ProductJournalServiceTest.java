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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    void getMyProductEvents_WhenProductIsOwnedAndNotSold_ShouldReturnOrderedEvents() {
        Product product = product(101, "AVAILABLE");
        List<ProductJournalEvent> expectedEvents = List.of(event(1, product), event(2, product));

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByProductOrderByEventDateDescEventIdDesc(product))
                .thenReturn(expectedEvents);

        List<ProductJournalEvent> result = productJournalService.getMyProductEvents("artisan@test.com", 101);

        assertThat(result).isEqualTo(expectedEvents);
        verify(journalEventRepository).findByProductOrderByEventDateDescEventIdDesc(product);
    }

    @Test
    void getMyProductEvents_WhenProductIsSold_ShouldThrowException() {
        Product product = product(101, "SOLD");

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(true);

        assertThatThrownBy(() -> productJournalService.getMyProductEvents("artisan@test.com", 101))
                .isInstanceOf(RuntimeException.class);

        verify(journalEventRepository, never()).findByProductOrderByEventDateDescEventIdDesc(any(Product.class));
    }

    @Test
    void getPublicEvents_WhenProductIsNotSold_ShouldReturnPublicEventsOnly() {
        Product product = product(101, "AVAILABLE");
        List<ProductJournalEvent> expectedEvents = List.of(event(1, product));

        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByProductAndIsPublicTrueOrderByEventDateDescEventIdDesc(product))
                .thenReturn(expectedEvents);

        List<ProductJournalEvent> result = productJournalService.getPublicEvents(product);

        assertThat(result).isEqualTo(expectedEvents);
        verify(journalEventRepository).findByProductAndIsPublicTrueOrderByEventDateDescEventIdDesc(product);
    }

    @Test
    void addEvent_WhenRequestIsValid_ShouldCreateJournalEventWithMedia() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);
        MultipartFile firstFile = image("front.jpg", 1024);
        MultipartFile secondFile = image("back.jpg", 1024);
        MultipartFile thirdFile = image("detail.jpg", 1024);

        mockEditableProduct(product, artisan);
        when(mediaStorageService.storeProductMedia(firstFile)).thenReturn("https://cdn.test/front.jpg");
        when(mediaStorageService.storeProductMedia(secondFile)).thenReturn("https://cdn.test/back.jpg");
        when(mediaStorageService.storeProductMedia(thirdFile)).thenReturn("https://cdn.test/detail.jpg");

        productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                "New leaves",
                true,
                List.of(firstFile, secondFile, thirdFile)
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
                .extracting(media -> media.getMediaUrl())
                .containsExactly("https://cdn.test/front.jpg", "https://cdn.test/back.jpg", "https://cdn.test/detail.jpg");
    }

    @Test
    void addEvent_WhenEventDateIsNullOrNotToday_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now().minusDays(1),
                "GROWTH",
                "Growth update",
                null,
                true,
                threeImages()
        )).isInstanceOf(RuntimeException.class);

        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addEvent_WhenTitleIsBlank_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                " ",
                null,
                true,
                threeImages()
        )).isInstanceOf(RuntimeException.class);

        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addEvent_WhenTitleOrDescriptionTooLong_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "a".repeat(256),
                null,
                true,
                threeImages()
        )).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tiêu đề nhật ký không được vượt quá 255 ký tự");

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                "a".repeat(2001),
                true,
                threeImages()
        )).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Câu chuyện nhật ký không được vượt quá 2000 ký tự");

        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addEvent_WhenLessThanThreeValidImagesProvided_ShouldThrowException() {
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
                List.of(image("front.jpg", 1024), image("back.jpg", 1024))
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addEvent_WhenMoreThanTenImagesProvided_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);
        List<MultipartFile> files = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(index -> image("image-" + index + ".jpg", 1024))
                .toList();

        mockEditableProduct(product, artisan);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                null,
                true,
                files
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addEvent_WhenImageTypeInvalidOrOversized_ShouldThrowException() {
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
                List.of(
                        image("front.jpg", 1024),
                        image("back.jpg", 1024),
                        file("clip.mp4", "video/mp4", 1024)
                )
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addEvent_WhenEventTypeInvalid_ShouldDefaultToPhotoUpdate() {
        Product product = product(101, "AVAILABLE");
        User artisan = artisan(10);
        List<MultipartFile> files = threeImages();

        mockEditableProduct(product, artisan);
        when(mediaStorageService.storeProductMedia(any(MultipartFile.class)))
                .thenReturn("https://cdn.test/image.jpg");

        productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "ABC",
                "Growth update",
                null,
                true,
                files
        );

        ArgumentCaptor<ProductJournalEvent> eventCaptor = ArgumentCaptor.forClass(ProductJournalEvent.class);
        verify(journalEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("PHOTO_UPDATE");
    }

    @Test
    void updateEventText_WhenEventExists_ShouldUpdateTitleDescriptionAndUpdatedAt() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);
        event.setUpdatedAt(oldUpdatedAt);

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));

        productJournalService.updateEventText("artisan@test.com", 101, 1, " Updated title ", "Updated description");

        assertThat(event.getTitle()).isEqualTo("Updated title");
        assertThat(event.getDescription()).isEqualTo("Updated description");
        assertThat(event.getUpdatedAt()).isAfter(oldUpdatedAt);
        verify(journalEventRepository).save(event);
    }

    @Test
    void updateEventText_WhenEventMissingOrTitleBlank_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productJournalService.updateEventText(
                "artisan@test.com",
                101,
                1,
                "Updated title",
                "Updated description"
        )).isInstanceOf(RuntimeException.class);

        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void updateEventVisibility_WhenEventExists_ShouldUpdateVisibilityAndUpdatedAt() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        event.setIsPublic(false);
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);
        event.setUpdatedAt(oldUpdatedAt);

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));

        productJournalService.updateEventVisibility("artisan@test.com", 101, 1, true);

        assertThat(event.getIsPublic()).isTrue();
        assertThat(event.getUpdatedAt()).isAfter(oldUpdatedAt);
        verify(journalEventRepository).save(event);
    }

    @Test
    void updateEventVisibility_WhenEventMissing_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productJournalService.updateEventVisibility("artisan@test.com", 101, 1, true))
                .isInstanceOf(RuntimeException.class);

        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addMediaToEvent_WhenFilesAreValid_ShouldAppendMediaAndUpdateEvent() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);
        event.setUpdatedAt(oldUpdatedAt);
        MultipartFile firstFile = image("new-front.jpg", 1024);
        MultipartFile secondFile = image("new-back.jpg", 1024);

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));
        when(mediaStorageService.storeProductMedia(firstFile)).thenReturn("https://cdn.test/new-front.jpg");
        when(mediaStorageService.storeProductMedia(secondFile)).thenReturn("https://cdn.test/new-back.jpg");

        productJournalService.addMediaToEvent("artisan@test.com", 101, 1, List.of(firstFile, secondFile));

        assertThat(event.getMediaList()).hasSize(2);
        assertThat(event.getMediaList())
                .extracting(media -> media.getMediaUrl())
                .containsExactly("https://cdn.test/new-front.jpg", "https://cdn.test/new-back.jpg");
        assertThat(event.getUpdatedAt()).isAfter(oldUpdatedAt);
        verify(journalEventRepository).save(event);
    }

    @Test
    void addMediaToEvent_WhenNoValidImageSelected_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        MultipartFile emptyFile = image("empty.jpg", 0);
        when(emptyFile.isEmpty()).thenReturn(true);

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> productJournalService.addMediaToEvent(
                "artisan@test.com",
                101,
                1,
                List.of(emptyFile)
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addMediaToEvent_WhenMoreThanTenImagesProvided_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        List<MultipartFile> files = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(index -> image("image-" + index + ".jpg", 1024))
                .toList();

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> productJournalService.addMediaToEvent("artisan@test.com", 101, 1, files))
                .isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addMediaToEvent_WhenFileTypeInvalidOrImageOversized_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> productJournalService.addMediaToEvent(
                "artisan@test.com",
                101,
                1,
                List.of(file("clip.mp4", "video/mp4", 1024))
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
    }

    @Test
    void addMediaToEvent_WhenExistingMediaExists_ShouldContinueDisplayOrderFromCurrentMax() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        event.getMediaList().add(journalMedia(event, "https://cdn.test/old-0.jpg", 0));
        event.getMediaList().add(journalMedia(event, "https://cdn.test/old-1.jpg", 1));
        event.getMediaList().add(journalMedia(event, "https://cdn.test/old-2.jpg", 2));
        MultipartFile firstFile = image("new-3.jpg", 1024);
        MultipartFile secondFile = image("new-4.jpg", 1024);

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));
        when(mediaStorageService.storeProductMedia(firstFile)).thenReturn("https://cdn.test/new-3.jpg");
        when(mediaStorageService.storeProductMedia(secondFile)).thenReturn("https://cdn.test/new-4.jpg");

        productJournalService.addMediaToEvent("artisan@test.com", 101, 1, List.of(firstFile, secondFile));

        assertThat(event.getMediaList()).hasSize(5);
        assertThat(event.getMediaList().get(3).getDisplayOrder()).isEqualTo(3);
        assertThat(event.getMediaList().get(4).getDisplayOrder()).isEqualTo(4);
        assertThat(event.getMediaList())
                .extracting(ProductJournalMedia::getMediaUrl)
                .contains("https://cdn.test/new-3.jpg", "https://cdn.test/new-4.jpg");
        verify(journalEventRepository).save(event);
    }

    @Test
    void deleteEvent_WhenEventExists_ShouldDeleteEvent() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));

        productJournalService.deleteEvent("artisan@test.com", 101, 1);

        verify(journalEventRepository).delete(event);
    }

    @Test
    void deleteEvent_WhenEventMissingOrNotAssociatedWithProduct_ShouldThrowException() {
        Product product = product(101, "AVAILABLE");

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productJournalService.deleteEvent("artisan@test.com", 101, 1))
                .isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).deleteProductMedia(any(String.class));
        verify(journalEventRepository, never()).delete(any(ProductJournalEvent.class));
    }

    @Test
    void deleteEvent_WhenEventHasMedia_ShouldDeleteAllStoredMediaThenDeleteEvent() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        event.getMediaList().add(journalMedia(event, "https://cdn.test/first.jpg", 0));
        event.getMediaList().add(journalMedia(event, "https://cdn.test/second.jpg", 1));

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product))
                .thenReturn(Optional.of(event));

        productJournalService.deleteEvent("artisan@test.com", 101, 1);

        verify(mediaStorageService).deleteProductMedia("https://cdn.test/first.jpg");
        verify(mediaStorageService).deleteProductMedia("https://cdn.test/second.jpg");
        verify(journalEventRepository).delete(event);
    }

    @Test
    void setCoverMedia_WhenMediaExists_ShouldMoveMediaToFirstDisplayOrder() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        ProductJournalMedia first = journalMedia(event, 11, "https://cdn.test/first.jpg", 0);
        ProductJournalMedia second = journalMedia(event, 12, "https://cdn.test/second.jpg", 1);
        event.getMediaList().add(first);
        event.getMediaList().add(second);

        when(artisanProductService.getMyProduct("artisan@test.com", 101)).thenReturn(product);
        when(artisanProductService.isSold(product)).thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product)).thenReturn(Optional.of(event));

        productJournalService.setCoverMedia("artisan@test.com", 101, 1, 12);

        assertThat(second.getDisplayOrder()).isEqualTo(0);
        assertThat(first.getDisplayOrder()).isEqualTo(1);
        verify(journalEventRepository).save(event);
    }

    @Test
    void replaceMedia_WhenImageIsValid_ShouldStoreNewImageAndDeleteOldImage() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        ProductJournalMedia media = journalMedia(event, 11, "https://cdn.test/old.jpg", 0);
        event.getMediaList().add(media);
        MultipartFile replacement = image("replacement.jpg", 1024);

        when(artisanProductService.getMyProduct("artisan@test.com", 101)).thenReturn(product);
        when(artisanProductService.isSold(product)).thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product)).thenReturn(Optional.of(event));
        when(mediaStorageService.storeProductMedia(replacement)).thenReturn("https://cdn.test/new.jpg");

        productJournalService.replaceMedia("artisan@test.com", 101, 1, 11, replacement);

        assertThat(media.getMediaUrl()).isEqualTo("https://cdn.test/new.jpg");
        verify(journalEventRepository).save(event);
        verify(mediaStorageService).deleteProductMedia("https://cdn.test/old.jpg");
    }

    @Test
    void deleteMedia_WhenMediaExists_ShouldRemoveMediaAndDeleteStoredFile() {
        Product product = product(101, "AVAILABLE");
        ProductJournalEvent event = event(1, product);
        ProductJournalMedia first = journalMedia(event, 11, "https://cdn.test/first.jpg", 0);
        ProductJournalMedia second = journalMedia(event, 12, "https://cdn.test/second.jpg", 1);
        event.getMediaList().add(first);
        event.getMediaList().add(second);

        when(artisanProductService.getMyProduct("artisan@test.com", 101)).thenReturn(product);
        when(artisanProductService.isSold(product)).thenReturn(false);
        when(journalEventRepository.findByEventIdAndProduct(1, product)).thenReturn(Optional.of(event));

        productJournalService.deleteMedia("artisan@test.com", 101, 1, 11);

        assertThat(event.getMediaList()).containsExactly(second);
        assertThat(second.getDisplayOrder()).isEqualTo(0);
        verify(mediaStorageService).deleteProductMedia("https://cdn.test/first.jpg");
        verify(journalEventRepository).save(event);
    }

    @Test
    void writeOperations_WhenProductIsSold_ShouldThrowExceptionBeforeJournalMutation() {
        Product soldProduct = product(101, "SOLD");

        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(soldProduct);
        when(artisanProductService.isSold(soldProduct))
                .thenReturn(true);

        assertThatThrownBy(() -> productJournalService.addEvent(
                "artisan@test.com",
                101,
                LocalDate.now(),
                "GROWTH",
                "Growth update",
                null,
                true,
                threeImages()
        )).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> productJournalService.updateEventText("artisan@test.com", 101, 1, "Title", null))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> productJournalService.updateEventVisibility("artisan@test.com", 101, 1, true))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> productJournalService.addMediaToEvent("artisan@test.com", 101, 1, threeImages()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> productJournalService.deleteEvent("artisan@test.com", 101, 1))
                .isInstanceOf(RuntimeException.class);

        verify(journalEventRepository, never()).findByEventIdAndProduct(any(Integer.class), any(Product.class));
        verify(journalEventRepository, never()).save(any(ProductJournalEvent.class));
        verify(journalEventRepository, never()).delete(any(ProductJournalEvent.class));
        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
    }

    private void mockEditableProduct(Product product, User artisan) {
        when(artisanProductService.getMyProduct("artisan@test.com", 101))
                .thenReturn(product);
        when(artisanProductService.isSold(product))
                .thenReturn(false);
        when(artisanProductService.getArtisanUser("artisan@test.com"))
                .thenReturn(artisan);
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
                .build();
    }

    private ProductJournalMedia journalMedia(ProductJournalEvent event, String mediaUrl, Integer displayOrder) {
        return journalMedia(event, displayOrder + 1, mediaUrl, displayOrder);
    }

    private ProductJournalMedia journalMedia(ProductJournalEvent event, Integer mediaId, String mediaUrl, Integer displayOrder) {
        return ProductJournalMedia.builder()
                .mediaId(mediaId)
                .event(event)
                .mediaUrl(mediaUrl)
                .mediaType("IMAGE")
                .displayOrder(displayOrder)
                .build();
    }

    private List<MultipartFile> threeImages() {
        return List.of(
                image("front.jpg", 1024),
                image("back.jpg", 1024),
                image("detail.jpg", 1024)
        );
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
