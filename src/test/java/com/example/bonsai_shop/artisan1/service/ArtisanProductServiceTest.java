package com.example.bonsai_shop.artisan1.service;

import com.example.bonsai_shop.artisan.service.ArtisanMediaStorageService;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.artisan.dto.ArtisanProductFormDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtisanProductServiceTest {

    private ProductRepository productRepository;
    private ProductMediaRepository productMediaRepository;
    private ProductSegmentRepository productSegmentRepository;
    private ProductTagRepository productTagRepository;
    private TagRepository tagRepository;
    private VarietyRepository varietyRepository;
    private UserRepository userRepository;
    private ArtisanMediaStorageService mediaStorageService;
    private ArtisanProductService artisanProductService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        productMediaRepository = mock(ProductMediaRepository.class);
        productSegmentRepository = mock(ProductSegmentRepository.class);
        productTagRepository = mock(ProductTagRepository.class);
        tagRepository = mock(TagRepository.class);
        varietyRepository = mock(VarietyRepository.class);
        userRepository = mock(UserRepository.class);
        mediaStorageService = mock(ArtisanMediaStorageService.class);

        artisanProductService = new ArtisanProductService(
                productRepository,
                categoryRepository,
                productMediaRepository,
                productSegmentRepository,
                productTagRepository,
                tagRepository,
                varietyRepository,
                userRepository,
                mediaStorageService
        );
    }

    @Test
    void getArtisanUser_WhenEmailExists_ShouldReturnUser() {
        User artisanUser = artisan(10, "artisan@test.com");

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));

        User result = artisanProductService.getArtisanUser("artisan@test.com");

        assertThat(result).isEqualTo(artisanUser);
        verify(userRepository).findByEmail("artisan@test.com");
    }

    @Test
    void getArtisanUser_WhenEmailDoesNotExist_ShouldThrowException() {
        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanProductService.getArtisanUser("missing@test.com"))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository).findByEmail("missing@test.com");
    }

    @Test
    void getMyProducts_WhenArtisanExists_ShouldReturnOwnedProducts() {
        User artisanUser = artisan(10, "artisan@test.com");
        List<Product> expectedProducts = List.of(
                product(101, artisanUser),
                product(102, artisanUser)
        );

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(productRepository.findByCreatedByUserIdOrderByCreatedAtDesc(10))
                .thenReturn(expectedProducts);

        List<Product> result = artisanProductService.getMyProducts("artisan@test.com");

        assertThat(result).isEqualTo(expectedProducts);
        verify(userRepository).findByEmail("artisan@test.com");
        verify(productRepository).findByCreatedByUserIdOrderByCreatedAtDesc(10);
    }

    @Test
    void getMyProduct_WhenProductNotOwnedByCurrentArtisan_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(productRepository.findByProductIdAndCreatedByUserId(101, 10))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanProductService.getMyProduct("artisan@test.com", 101))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository).findByEmail("artisan@test.com");
        verify(productRepository).findByProductIdAndCreatedByUserId(101, 10);
    }

    @Test
    void createProduct_WhenValidForm_ShouldCreateDraftProduct() {
        User artisanUser = artisan(10, "artisan@test.com");
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Standard");
        ArtisanProductFormDTO form = validForm();

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.existsByProductCode(any(String.class)))
                .thenReturn(false);

        Product result = artisanProductService.createProduct("artisan@test.com", form);

        assertThat(result.getProductStatus()).isEqualTo("DRAFT");
        assertThat(result.getIsVisible()).isFalse();
        assertThat(result.getViewCount()).isZero();
        assertThat(result.getProductCode()).startsWith("BSMS-JM-KG-");
        verify(productRepository, times(2)).save(any(Product.class));
        verify(productTagRepository).deleteByProduct(result);
    }

    @Test
    void createProduct_WhenVarietyDoesNotExist_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        ArtisanProductFormDTO form = validForm();

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                .isInstanceOf(RuntimeException.class);

        verify(productSegmentRepository, never()).findById(any(Integer.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_WhenSegmentDoesNotExist_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Variety variety = variety(1);
        ArtisanProductFormDTO form = validForm();

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_WhenRequiredTreeSpecificationInvalid_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Standard");
        ArtisanProductFormDTO form = validForm();
        form.setAge(null);

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));

        assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_WhenStyleContainsInvalidCharacters_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Standard");
        ArtisanProductFormDTO form = validForm();
        form.setStyle("Dáng trực 123");

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));

        assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_WhenPriceExceedsLimit_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Standard");
        ArtisanProductFormDTO form = validForm();
        form.setPrice(new BigDecimal("1000000000000"));

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));

        assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Giá sản phẩm không được vượt quá");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_WhenSegmentIsElite_ShouldNotMakePricePublic() {
        User artisanUser = artisan(10, "artisan@test.com");
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Elite");
        ArtisanProductFormDTO form = validForm();

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.existsByProductCode(any(String.class)))
                .thenReturn(false);

        artisanProductService.createProduct("artisan@test.com", form);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(2)).save(productCaptor.capture());
        assertThat(productCaptor.getAllValues().get(0).getIsPublicPrice()).isFalse();
    }

    @Test
    void createProduct_WhenDuplicateTagIdsProvided_ShouldSaveOnlyDistinctProductTags() {
        User artisanUser = artisan(10, "artisan@test.com");
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Standard");
        ArtisanProductFormDTO form = validForm();
        form.setTagIds(Arrays.asList(1, 1, 2, null));

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.existsByProductCode(any(String.class)))
                .thenReturn(false);
        when(tagRepository.findAllById(List.of(1, 2)))
                .thenReturn(List.of(tag(1), tag(2)));

        Product result = artisanProductService.createProduct("artisan@test.com", form);

        ArgumentCaptor<ProductTag> productTagCaptor = ArgumentCaptor.forClass(ProductTag.class);
        verify(tagRepository).findAllById(List.of(1, 2));
        verify(productTagRepository, times(2)).save(productTagCaptor.capture());
        assertThat(productTagCaptor.getAllValues())
                .extracting(productTag -> productTag.getTag().getTagId())
                .containsExactly(1, 2);
        assertThat(productTagCaptor.getAllValues())
                .allSatisfy(productTag -> assertThat(productTag.getProduct()).isEqualTo(result));
    }

    @Test
    void createProduct_WhenMoreThanTwelveUniqueTagsProvided_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Standard");
        ArtisanProductFormDTO form = validForm();
        form.setTagIds(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13));

        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.existsByProductCode(any(String.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                .isInstanceOf(RuntimeException.class);

        verify(tagRepository, never()).findAllById(any());
        verify(productTagRepository, never()).save(any(ProductTag.class));
    }

    @Test
    void updateProduct_WhenDraftProductIsValid_ShouldUpdateFieldsAndSyncTags() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = product(101, artisanUser);
        product.setProductStatus("DRAFT");
        product.setIsVisible(false);
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Standard");
        ArtisanProductFormDTO form = validForm();
        form.setProductName("Updated Bonsai");
        form.setDescription("Updated description");
        form.setTagIds(List.of(1, 2));

        mockOwnedProductLookup(artisanUser, product);
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));
        when(productRepository.save(product))
                .thenReturn(product);
        when(tagRepository.findAllById(List.of(1, 2)))
                .thenReturn(List.of(tag(1), tag(2)));

        artisanProductService.updateProduct("artisan@test.com", 101, form);

        assertThat(product.getProductName()).isEqualTo("Updated Bonsai");
        assertThat(product.getDescription()).isEqualTo("Updated description");
        assertThat(product.getVariety()).isEqualTo(variety);
        assertThat(product.getSegment()).isEqualTo(segment);
        verify(productRepository).save(product);
        verify(productTagRepository).deleteByProduct(product);
        verify(productTagRepository, times(2)).save(any(ProductTag.class));
    }

    @Test
    void updateProduct_WhenAvailableProductIsHidden_ShouldUpdateSuccessfully() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = product(101, artisanUser);
        product.setProductStatus("AVAILABLE");
        product.setIsVisible(false);
        Variety variety = variety(1);
        ProductSegment segment = segment(2, "Standard");
        ArtisanProductFormDTO form = validForm();
        form.setProductStatus("AVAILABLE");

        mockOwnedProductLookup(artisanUser, product);
        when(varietyRepository.findById(1))
                .thenReturn(Optional.of(variety));
        when(productSegmentRepository.findById(2))
                .thenReturn(Optional.of(segment));
        when(productRepository.save(product))
                .thenReturn(product);

        artisanProductService.updateProduct("artisan@test.com", 101, form);

        assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
        assertThat(product.getIsVisible()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_WhenProductIsSold_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = product(101, artisanUser);
        product.setProductStatus("SOLD");
        ArtisanProductFormDTO form = validForm();

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.updateProduct("artisan@test.com", 101, form))
                .isInstanceOf(RuntimeException.class);

        verify(varietyRepository, never()).findById(any(Integer.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void toFormDTO_WhenProductHasAssociations_ShouldMapMatchingFieldsAndIds() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = product(101, artisanUser);
        product.setVariety(variety(1));
        product.setSegment(segment(2, "Standard"));
        product.setDescription("Description");
        product.setTreeStory("Story");
        product.setAge(7);
        product.setHeight(60.5F);
        product.setTrunkDiameter(6.5F);
        product.setStyle("Dáng xiêu");
        product.setPrice(new BigDecimal("2500000"));
        product.setProductStatus("DRAFT");

        when(productTagRepository.findByProduct(product))
                .thenReturn(List.of(productTag(product, tag(1)), productTag(product, tag(2))));

        ArtisanProductFormDTO result = artisanProductService.toFormDTO(product);

        assertThat(result.getVarietyId()).isEqualTo(1);
        assertThat(result.getSegmentId()).isEqualTo(2);
        assertThat(result.getProductName()).isEqualTo("Bonsai 101");
        assertThat(result.getDescription()).isEqualTo("Description");
        assertThat(result.getTreeStory()).isEqualTo("Story");
        assertThat(result.getAge()).isEqualTo(7);
        assertThat(result.getHeight()).isEqualTo(60.5F);
        assertThat(result.getTrunkDiameter()).isEqualTo(6.5F);
        assertThat(result.getStyle()).isEqualTo("Dáng xiêu");
        assertThat(result.getPrice()).isEqualByComparingTo("2500000");
        assertThat(result.getProductStatus()).isEqualTo("DRAFT");
        assertThat(result.getTagIds()).containsExactly(1, 2);
    }

    @Test
    void deleteProduct_WhenProductIsDraft_ShouldDeleteStoredMediaAndProduct() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = product(101, artisanUser);
        product.setProductStatus("DRAFT");
        ProductMedia firstMedia = media(product, "https://cdn.test/first.jpg");
        ProductMedia secondMedia = media(product, "https://cdn.test/second.jpg");

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of(firstMedia, secondMedia));

        artisanProductService.deleteProduct("artisan@test.com", 101);

        verify(mediaStorageService).deleteProductMedia("https://cdn.test/first.jpg");
        verify(mediaStorageService).deleteProductMedia("https://cdn.test/second.jpg");
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_WhenProductIsNotDraft_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = product(101, artisanUser);
        product.setProductStatus("AVAILABLE");

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.deleteProduct("artisan@test.com", 101))
                .isInstanceOf(RuntimeException.class);

        verify(productMediaRepository, never()).findByProductOrderByDisplayOrderAscMediaIdAsc(any(Product.class));
        verify(mediaStorageService, never()).deleteProductMedia(any(String.class));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void addMedia_WhenFirstImageUploaded_ShouldMakeItDefaultThumbnail() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile file = file("front.jpg", "image/jpeg", 1024);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of());
        when(mediaStorageService.storeProductMedia(file))
                .thenReturn("https://cdn.test/front.jpg");

        artisanProductService.addMedia("artisan@test.com", 101, file, "FRONT", "Front view", false);

        ArgumentCaptor<ProductMedia> mediaCaptor = ArgumentCaptor.forClass(ProductMedia.class);
        verify(mediaStorageService).storeProductMedia(file);
        verify(productMediaRepository).save(mediaCaptor.capture());
        ProductMedia savedMedia = mediaCaptor.getValue();
        assertThat(savedMedia.getMediaUrl()).isEqualTo("https://cdn.test/front.jpg");
        assertThat(savedMedia.getMediaType()).isEqualTo("IMAGE");
        assertThat(savedMedia.getSlotType()).isEqualTo("FRONT");
        assertThat(savedMedia.getCaption()).isEqualTo("Front view");
        assertThat(savedMedia.getIsThumbnail()).isTrue();
        assertThat(savedMedia.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void addMedia_WhenNewImageRequestedAsThumbnail_ShouldReplaceCurrentThumbnail() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        ProductMedia existingThumbnail = media(product, "https://cdn.test/old.jpg");
        existingThumbnail.setMediaId(201);
        existingThumbnail.setIsThumbnail(true);
        existingThumbnail.setDisplayOrder(1);
        MultipartFile file = file("new.jpg", "image/jpeg", 1024);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of(existingThumbnail));
        when(mediaStorageService.storeProductMedia(file))
                .thenReturn("https://cdn.test/new.jpg");

        artisanProductService.addMedia("artisan@test.com", 101, file, "BACK", "Back view", true);

        ArgumentCaptor<ProductMedia> mediaCaptor = ArgumentCaptor.forClass(ProductMedia.class);
        verify(productMediaRepository, times(2)).save(mediaCaptor.capture());
        assertThat(existingThumbnail.getIsThumbnail()).isFalse();
        ProductMedia newMedia = mediaCaptor.getAllValues().get(1);
        assertThat(newMedia.getMediaUrl()).isEqualTo("https://cdn.test/new.jpg");
        assertThat(newMedia.getIsThumbnail()).isTrue();
        assertThat(newMedia.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void addMedia_WhenVideoRequestedAsThumbnail_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile file = file("clip.mp4", "video/mp4", 1024);

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMedia("artisan@test.com", 101, file, "FRONT", null, true))
                .isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMedia_WhenImageShotTypeInvalid_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile file = file("front.jpg", "image/jpeg", 1024);

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMedia("artisan@test.com", 101, file, "TOP", null, false))
                .isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMedia_WhenVideoHasShotType_ShouldSaveWithNullSlotType() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile file = file("clip.mp4", "video/mp4", 1024);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of());
        when(mediaStorageService.storeProductMedia(file))
                .thenReturn("https://cdn.test/clip.mp4");

        artisanProductService.addMedia("artisan@test.com", 101, file, "FRONT", "Video clip", false);

        ArgumentCaptor<ProductMedia> mediaCaptor = ArgumentCaptor.forClass(ProductMedia.class);
        verify(productMediaRepository).save(mediaCaptor.capture());
        ProductMedia savedMedia = mediaCaptor.getValue();
        assertThat(savedMedia.getMediaType()).isEqualTo("VIDEO");
        assertThat(savedMedia.getSlotType()).isNull();
        assertThat(savedMedia.getIsThumbnail()).isFalse();
    }

    @Test
    void addMedia_WhenImageExceedsMaximumSize_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile file = file("large.jpg", "image/jpeg", 7L * 1024 * 1024 + 1);

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMedia("artisan@test.com", 101, file, "FRONT", null, false))
                .isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMedia_WhenVideoExceedsMaximumSize_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile file = file("large.mp4", "video/mp4", 100L * 1024 * 1024 + 1);

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMedia("artisan@test.com", 101, file, "FRONT", null, false))
                .isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMediaBatch_WhenValidFilesProvided_ShouldSaveAllWithConsecutiveDisplayOrders() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile firstFile = file("front.jpg", "image/jpeg", 1024);
        MultipartFile secondFile = file("back.jpg", "image/jpeg", 1024);
        MultipartFile thirdFile = file("clip.mp4", "video/mp4", 1024);
        List<MultipartFile> files = List.of(firstFile, secondFile, thirdFile);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of());
        when(mediaStorageService.storeProductMedia(firstFile)).thenReturn("https://cdn.test/front.jpg");
        when(mediaStorageService.storeProductMedia(secondFile)).thenReturn("https://cdn.test/back.jpg");
        when(mediaStorageService.storeProductMedia(thirdFile)).thenReturn("https://cdn.test/clip.mp4");

        int result = artisanProductService.addMediaBatch(
                "artisan@test.com",
                101,
                files,
                List.of("IMAGE", "IMAGE", "VIDEO"),
                List.of("FRONT", "BACK", "FRONT"),
                List.of("Front", "Back", "Clip"),
                1
        );

        ArgumentCaptor<ProductMedia> mediaCaptor = ArgumentCaptor.forClass(ProductMedia.class);
        assertThat(result).isEqualTo(3);
        verify(productMediaRepository, times(3)).save(mediaCaptor.capture());
        assertThat(mediaCaptor.getAllValues())
                .extracting(ProductMedia::getDisplayOrder)
                .containsExactly(1, 2, 3);
        assertThat(mediaCaptor.getAllValues())
                .extracting(ProductMedia::getMediaUrl)
                .containsExactly("https://cdn.test/front.jpg", "https://cdn.test/back.jpg", "https://cdn.test/clip.mp4");
        assertThat(mediaCaptor.getAllValues())
                .extracting(ProductMedia::getIsThumbnail)
                .containsExactly(false, true, false);
    }

    @Test
    void addMediaBatch_WhenNoFilesSelected_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMediaBatch(
                "artisan@test.com",
                101,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMediaBatch_WhenMoreThanTenFilesProvided_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        List<MultipartFile> files = IntStream.rangeClosed(1, 11)
                .mapToObj(index -> file("image-" + index + ".jpg", "image/jpeg", 1024))
                .toList();

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMediaBatch(
                "artisan@test.com",
                101,
                files,
                null,
                null,
                null,
                null
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMediaBatch_WhenAnyFileIsEmpty_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile validFile = file("front.jpg", "image/jpeg", 1024);
        MultipartFile emptyFile = file("empty.jpg", "image/jpeg", 0);
        when(emptyFile.isEmpty()).thenReturn(true);

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMediaBatch(
                "artisan@test.com",
                101,
                List.of(validFile, emptyFile),
                null,
                null,
                null,
                null
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMediaBatch_WhenMediaTypeListIsShorterThanFiles_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        List<MultipartFile> files = List.of(
                file("front.jpg", "image/jpeg", 1024),
                file("back.jpg", "image/jpeg", 1024)
        );

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMediaBatch(
                "artisan@test.com",
                101,
                files,
                List.of("IMAGE"),
                List.of("FRONT", "BACK"),
                List.of("Front", "Back"),
                null
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMediaBatch_WhenThumbnailIndexOutsideFileRange_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        List<MultipartFile> files = List.of(
                file("front.jpg", "image/jpeg", 1024),
                file("back.jpg", "image/jpeg", 1024)
        );

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.addMediaBatch(
                "artisan@test.com",
                101,
                files,
                List.of("IMAGE", "IMAGE"),
                List.of("FRONT", "BACK"),
                List.of("Front", "Back"),
                2
        )).isInstanceOf(RuntimeException.class);

        verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void addMediaBatch_WhenFirstBatchHasNoExplicitThumbnail_ShouldSelectFirstImageAsThumbnail() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        MultipartFile videoFile = file("clip.mp4", "video/mp4", 1024);
        MultipartFile firstImageFile = file("front.jpg", "image/jpeg", 1024);
        MultipartFile secondImageFile = file("back.jpg", "image/jpeg", 1024);
        List<MultipartFile> files = List.of(videoFile, firstImageFile, secondImageFile);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of());
        when(mediaStorageService.storeProductMedia(videoFile)).thenReturn("https://cdn.test/clip.mp4");
        when(mediaStorageService.storeProductMedia(firstImageFile)).thenReturn("https://cdn.test/front.jpg");
        when(mediaStorageService.storeProductMedia(secondImageFile)).thenReturn("https://cdn.test/back.jpg");

        artisanProductService.addMediaBatch(
                "artisan@test.com",
                101,
                files,
                List.of("VIDEO", "IMAGE", "IMAGE"),
                List.of("FRONT", "FRONT", "BACK"),
                List.of("Clip", "Front", "Back"),
                null
        );

        ArgumentCaptor<ProductMedia> mediaCaptor = ArgumentCaptor.forClass(ProductMedia.class);
        verify(productMediaRepository, times(3)).save(mediaCaptor.capture());
        assertThat(mediaCaptor.getAllValues())
                .extracting(ProductMedia::getMediaType)
                .containsExactly("VIDEO", "IMAGE", "IMAGE");
        assertThat(mediaCaptor.getAllValues())
                .extracting(ProductMedia::getIsThumbnail)
                .containsExactly(false, true, false);
    }

    @Test
    void setThumbnail_WhenSelectedMediaIsImage_ShouldMakeOnlySelectedMediaThumbnail() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        ProductMedia firstMedia = media(product, 201, "https://cdn.test/first.jpg", "IMAGE", true);
        ProductMedia secondMedia = media(product, 202, "https://cdn.test/second.jpg", "IMAGE", false);
        ProductMedia videoMedia = media(product, 203, "https://cdn.test/clip.mp4", "VIDEO", false);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByMediaIdAndProduct(202, product))
                .thenReturn(Optional.of(secondMedia));
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of(firstMedia, secondMedia, videoMedia));

        artisanProductService.setThumbnail("artisan@test.com", 101, 202);

        assertThat(firstMedia.getIsThumbnail()).isFalse();
        assertThat(secondMedia.getIsThumbnail()).isTrue();
        assertThat(videoMedia.getIsThumbnail()).isFalse();
        verify(productMediaRepository, times(3)).save(any(ProductMedia.class));
    }

    @Test
    void setThumbnail_WhenSelectedMediaIsVideo_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        ProductMedia videoMedia = media(product, 203, "https://cdn.test/clip.mp4", "VIDEO", false);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByMediaIdAndProduct(203, product))
                .thenReturn(Optional.of(videoMedia));

        assertThatThrownBy(() -> artisanProductService.setThumbnail("artisan@test.com", 101, 203))
                .isInstanceOf(RuntimeException.class);

        verify(productMediaRepository, never()).findByProductOrderByDisplayOrderAscMediaIdAsc(product);
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void updateMediaOrder_WhenInputListsAreAligned_ShouldUpdateOrderAndMetadata() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        ProductMedia firstMedia = media(product, 201, "https://cdn.test/first.jpg", "IMAGE", true);
        ProductMedia secondMedia = media(product, 202, "https://cdn.test/second.jpg", "IMAGE", false);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByMediaIdAndProduct(201, product))
                .thenReturn(Optional.of(firstMedia));
        when(productMediaRepository.findByMediaIdAndProduct(202, product))
                .thenReturn(Optional.of(secondMedia));
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of(firstMedia, secondMedia));

        artisanProductService.updateMediaOrder(
                "artisan@test.com",
                101,
                List.of(201, 202),
                List.of(2, 1),
                List.of("BACK", "FRONT"),
                List.of("Back caption", "Front caption")
        );

        assertThat(firstMedia.getDisplayOrder()).isEqualTo(2);
        assertThat(firstMedia.getSlotType()).isEqualTo("BACK");
        assertThat(firstMedia.getCaption()).isEqualTo("Back caption");
        assertThat(secondMedia.getDisplayOrder()).isEqualTo(1);
        assertThat(secondMedia.getSlotType()).isEqualTo("FRONT");
        assertThat(secondMedia.getCaption()).isEqualTo("Front caption");
        verify(productMediaRepository, times(2)).save(any(ProductMedia.class));
    }

    @Test
    void updateMediaOrder_WhenMediaIdsAndDisplayOrdersMismatch_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.updateMediaOrder(
                "artisan@test.com",
                101,
                List.of(201, 202),
                List.of(1),
                List.of("FRONT", "BACK"),
                List.of("Front", "Back")
        )).isInstanceOf(RuntimeException.class);

        verify(productMediaRepository, never()).findByMediaIdAndProduct(any(Integer.class), any(Product.class));
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void deleteMedia_WhenDeletedMediaIsNotThumbnail_ShouldDeleteMediaWithoutReassigningThumbnail() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        ProductMedia media = media(product, 202, "https://cdn.test/second.jpg", "IMAGE", false);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByMediaIdAndProduct(202, product))
                .thenReturn(Optional.of(media));

        artisanProductService.deleteMedia("artisan@test.com", 101, 202);

        verify(mediaStorageService).deleteProductMedia("https://cdn.test/second.jpg");
        verify(productMediaRepository).delete(media);
        verify(productMediaRepository, never()).findByProductOrderByDisplayOrderAscMediaIdAsc(product);
        verify(productMediaRepository, never()).save(any(ProductMedia.class));
    }

    @Test
    void deleteMedia_WhenDeletedMediaIsThumbnail_ShouldAssignFirstRemainingImageAsThumbnail() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = editableDraftProduct(101, artisanUser);
        ProductMedia deletedThumbnail = media(product, 201, "https://cdn.test/first.jpg", "IMAGE", true);
        ProductMedia remainingVideo = media(product, 202, "https://cdn.test/clip.mp4", "VIDEO", false);
        ProductMedia remainingImage = media(product, 203, "https://cdn.test/second.jpg", "IMAGE", false);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByMediaIdAndProduct(201, product))
                .thenReturn(Optional.of(deletedThumbnail));
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of(remainingVideo, remainingImage));

        artisanProductService.deleteMedia("artisan@test.com", 101, 201);

        assertThat(remainingImage.getIsThumbnail()).isTrue();
        assertThat(remainingVideo.getIsThumbnail()).isFalse();
        verify(mediaStorageService).deleteProductMedia("https://cdn.test/first.jpg");
        verify(productMediaRepository).delete(deletedThumbnail);
        verify(productMediaRepository).save(remainingImage);
    }

    @Test
    void publish_WhenDraftProductIsReady_ShouldMakeProductAvailableAndVisible() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = publishReadyProduct(101, artisanUser, "DRAFT", false, "Standard");
        ProductMedia thumbnail = media(product, 201, "https://cdn.test/front.jpg", "IMAGE", true);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of(thumbnail));

        artisanProductService.publish("artisan@test.com", 101);

        assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
        assertThat(product.getIsVisible()).isTrue();
        assertThat(product.getIsPublicPrice()).isTrue();
        verify(productRepository).save(product);
    }

    @Test
    void publish_WhenProductIsNotDraft_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = publishReadyProduct(101, artisanUser, "AVAILABLE", true, "Standard");

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.publish("artisan@test.com", 101))
                .isInstanceOf(RuntimeException.class);

        verify(productMediaRepository, never()).findByProductOrderByDisplayOrderAscMediaIdAsc(any(Product.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void publish_WhenRequiredInformationMissing_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = publishReadyProduct(101, artisanUser, "DRAFT", false, "Standard");
        product.setProductName(" ");

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.publish("artisan@test.com", 101))
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void publish_WhenProductHasNoMedia_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = publishReadyProduct(101, artisanUser, "DRAFT", false, "Standard");

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of());

        assertThatThrownBy(() -> artisanProductService.publish("artisan@test.com", 101))
                .isInstanceOf(RuntimeException.class);

        assertThat(product.getProductStatus()).isEqualTo("DRAFT");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void hideProduct_WhenAvailableProductIsVisible_ShouldHideAndSaveProduct() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = publishReadyProduct(101, artisanUser, "AVAILABLE", true, "Standard");

        mockOwnedProductLookup(artisanUser, product);

        artisanProductService.hideProduct("artisan@test.com", 101);

        assertThat(product.getIsVisible()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void showProduct_WhenProductIsReserved_ShouldThrowException() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = publishReadyProduct(101, artisanUser, "RESERVED", false, "Standard");

        mockOwnedProductLookup(artisanUser, product);

        assertThatThrownBy(() -> artisanProductService.showProduct("artisan@test.com", 101))
                .isInstanceOf(RuntimeException.class);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void showProduct_WhenAvailableProductIsHidden_ShouldShowAndRecalculatePublicPrice() {
        User artisanUser = artisan(10, "artisan@test.com");
        Product product = publishReadyProduct(101, artisanUser, "AVAILABLE", false, "Elite");
        product.setIsPublicPrice(true);
        ProductMedia thumbnail = media(product, 201, "https://cdn.test/front.jpg", "IMAGE", true);

        mockOwnedProductLookup(artisanUser, product);
        when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                .thenReturn(List.of(thumbnail));

        artisanProductService.showProduct("artisan@test.com", 101);

        assertThat(product.getIsVisible()).isTrue();
        assertThat(product.getIsPublicPrice()).isFalse();
        verify(productRepository).save(product);
    }

    private User artisan(Integer userId, String email) {
        return User.builder()
                .userId(userId)
                .email(email)
                .fullName("Artisan " + userId)
                .build();
    }

    private Product product(Integer productId, User artisanUser) {
        return Product.builder()
                .productId(productId)
                .createdBy(artisanUser)
                .productName("Bonsai " + productId)
                .productStatus("AVAILABLE")
                .build();
    }

    private Product editableDraftProduct(Integer productId, User artisanUser) {
        Product product = product(productId, artisanUser);
        product.setProductStatus("DRAFT");
        product.setIsVisible(false);
        return product;
    }

    private Product publishReadyProduct(Integer productId,
                                        User artisanUser,
                                        String productStatus,
                                        Boolean isVisible,
                                        String segmentName) {
        Product product = product(productId, artisanUser);
        product.setVariety(variety(1));
        product.setSegment(segment(2, segmentName));
        product.setProductName("Publish Ready Bonsai");
        product.setAge(8);
        product.setHeight(55.5F);
        product.setTrunkDiameter(5.5F);
        product.setStyle("Dáng trực");
        product.setPrice(new BigDecimal("2000000"));
        product.setProductStatus(productStatus);
        product.setIsVisible(isVisible);
        return product;
    }

    private void mockOwnedProductLookup(User artisanUser, Product product) {
        when(userRepository.findByEmail("artisan@test.com"))
                .thenReturn(Optional.of(artisanUser));
        when(productRepository.findByProductIdAndCreatedByUserId(product.getProductId(), artisanUser.getUserId()))
                .thenReturn(Optional.of(product));
    }

    private ArtisanProductFormDTO validForm() {
        return ArtisanProductFormDTO.builder()
                .varietyId(1)
                .segmentId(2)
                .productName("Japanese Maple")
                .description("Healthy bonsai")
                .treeStory("Imported and trained")
                .age(5)
                .height(45.5F)
                .trunkDiameter(4.2F)
                .style("Dáng trực")
                .price(new BigDecimal("1500000"))
                .build();
    }

    private Variety variety(Integer varietyId) {
        return Variety.builder()
                .varietyId(varietyId)
                .varietyName("Kim giòn")
                .category(Category.builder()
                        .categoryId(1)
                        .categoryName("Japanese Maple")
                        .build())
                .build();
    }

    private ProductSegment segment(Integer segmentId, String segmentName) {
        return ProductSegment.builder()
                .segmentId(segmentId)
                .segmentName(segmentName)
                .build();
    }

    private Tag tag(Integer tagId) {
        return Tag.builder()
                .tagId(tagId)
                .tagName("Tag " + tagId)
                .build();
    }

    private ProductTag productTag(Product product, Tag tag) {
        return ProductTag.builder()
                .product(product)
                .tag(tag)
                .build();
    }

    private ProductMedia media(Product product, String mediaUrl) {
        return ProductMedia.builder()
                .product(product)
                .mediaUrl(mediaUrl)
                .mediaType("IMAGE")
                .build();
    }

    private ProductMedia media(Product product, Integer mediaId, String mediaUrl, String mediaType, Boolean isThumbnail) {
        return ProductMedia.builder()
                .mediaId(mediaId)
                .product(product)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .isThumbnail(isThumbnail)
                .displayOrder(mediaId - 200)
                .build();
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
