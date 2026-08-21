package com.example.bonsai_shop.artisan1.service;

import com.example.bonsai_shop.artisan.dto.ArtisanProductFormDTO;
import com.example.bonsai_shop.artisan.service.ArtisanMediaStorageService;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.Category;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.ProductSegment;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtisanProductServiceTest {

        private ProductRepository productRepository;
        private ProductMediaRepository productMediaRepository;
        private ProductSegmentRepository productSegmentRepository;
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
                ProductTagRepository productTagRepository = mock(ProductTagRepository.class);
                TagRepository tagRepository = mock(TagRepository.class);
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
                                mediaStorageService);
        }

        @Test
        void createProduct_WhenPriceIsMissing_ShouldRejectProduct() {
                ArtisanProductFormDTO form = validForm();
                form.setPrice(null);
                mockValidCreateDependencies();

                assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                                .isInstanceOf(RuntimeException.class);

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProduct_WhenPriceIsZeroOrNegative_ShouldRejectProduct() {
                ArtisanProductFormDTO form = validForm();
                form.setPrice(BigDecimal.ZERO);
                mockValidCreateDependencies();

                assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("0");

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProduct_WhenPriceContainsDecimal_ShouldRejectProduct() {
                ArtisanProductFormDTO form = validForm();
                form.setPrice(new BigDecimal("1500000.5"));
                mockValidCreateDependencies();

                assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                                .isInstanceOf(RuntimeException.class);

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProduct_WhenAgeExceedsMaximum_ShouldRejectProduct() {
                ArtisanProductFormDTO form = validForm();
                form.setAge(1001);
                mockValidCreateDependencies();

                assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("1000");

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProduct_WhenHeightExceedsMaximum_ShouldRejectProduct() {
                ArtisanProductFormDTO form = validForm();
                form.setHeight(1001F);
                mockValidCreateDependencies();

                assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("1000 cm");

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProduct_WhenTrunkDiameterExceedsMaximum_ShouldRejectProduct() {
                ArtisanProductFormDTO form = validForm();
                form.setTrunkDiameter(501F);
                mockValidCreateDependencies();

                assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("500 cm");

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProduct_WhenStyleIsEmpty_ShouldRejectProduct() {
                ArtisanProductFormDTO form = validForm();
                form.setStyle("   ");
                mockValidCreateDependencies();

                assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                                .isInstanceOf(RuntimeException.class);

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void createProduct_WhenStyleExceedsMaximumLength_ShouldRejectProduct() {
                ArtisanProductFormDTO form = validForm();
                form.setStyle("a".repeat(101));
                mockValidCreateDependencies();

                assertThatThrownBy(() -> artisanProductService.createProduct("artisan@test.com", form))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("100");

                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void updateProduct_WhenAvailableProductIsVisible_ShouldRejectUpdate() {
                User artisanUser = artisan(10, "artisan@test.com");
                Product product = product(101, artisanUser, "AVAILABLE", true);
                ArtisanProductFormDTO form = validForm();

                mockOwnedProductLookup(artisanUser, product);

                assertThatThrownBy(() -> artisanProductService.updateProduct("artisan@test.com", 101, form))
                                .isInstanceOf(RuntimeException.class);

                assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
                assertThat(product.getIsVisible()).isTrue();
                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void updateProduct_WhenProductIsReserved_ShouldRejectUpdate() {
                User artisanUser = artisan(10, "artisan@test.com");
                Product product = product(101, artisanUser, "RESERVED", false);
                ArtisanProductFormDTO form = validForm();

                mockOwnedProductLookup(artisanUser, product);

                assertThatThrownBy(() -> artisanProductService.updateProduct("artisan@test.com", 101, form))
                                .isInstanceOf(RuntimeException.class);

                assertThat(product.getProductStatus()).isEqualTo("RESERVED");
                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void deleteProduct_WhenProductIsNotDraft_ShouldRejectDelete() {
                User artisanUser = artisan(10, "artisan@test.com");
                Product product = product(101, artisanUser, "AVAILABLE", false);

                mockOwnedProductLookup(artisanUser, product);

                assertThatThrownBy(() -> artisanProductService.deleteProduct("artisan@test.com", 101))
                                .isInstanceOf(RuntimeException.class);

                assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
                verify(productMediaRepository, never())
                                .findByProductOrderByDisplayOrderAscMediaIdAsc(any(Product.class));
                verify(mediaStorageService, never()).deleteProductMedia(any());
                verify(productRepository, never()).delete(any(Product.class));
        }

        @Test
        void addMediaBatch_WhenVideoSelectedAsThumbnail_ShouldRejectBatchThumbnail() {
                User artisanUser = artisan(10, "artisan@test.com");
                Product product = product(101, artisanUser, "DRAFT", false);
                MultipartFile imageFile = file("front.jpg", "image/jpeg", 1024);
                MultipartFile videoFile = file("clip.mp4", "video/mp4", 1024);

                mockOwnedProductLookup(artisanUser, product);
                when(productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product))
                                .thenReturn(List.of());

                assertThatThrownBy(() -> artisanProductService.addMediaBatch(
                                "artisan@test.com",
                                101,
                                List.of(imageFile, videoFile),
                                List.of("IMAGE", "VIDEO"),
                                Arrays.asList("FRONT", null),
                                List.of("Front", "Clip"),
                                1)).isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Video");

                verify(mediaStorageService, never()).storeProductMedia(any(MultipartFile.class));
                verify(productMediaRepository, never()).save(any(ProductMedia.class));
        }

        @Test
        void updateProduct_WhenChangingToEliteSegment_ShouldHidePublicPrice() {
                User artisanUser = artisan(10, "artisan@test.com");
                Product product = product(101, artisanUser, "DRAFT", false);
                product.setIsPublicPrice(true);
                Variety variety = variety(1);
                ProductSegment eliteSegment = segment(2, "Elite");
                ArtisanProductFormDTO form = validForm();

                mockOwnedProductLookup(artisanUser, product);
                when(varietyRepository.findById(1)).thenReturn(Optional.of(variety));
                when(productSegmentRepository.findById(2)).thenReturn(Optional.of(eliteSegment));
                when(productRepository.save(product)).thenReturn(product);

                artisanProductService.updateProduct("artisan@test.com", 101, form);

                assertThat(product.getSegment()).isEqualTo(eliteSegment);
                assertThat(product.getIsPublicPrice()).isFalse();
                verify(productRepository).save(product);
        }

        private void mockValidCreateDependencies() {
                when(userRepository.findByEmail("artisan@test.com"))
                                .thenReturn(Optional.of(artisan(10, "artisan@test.com")));
                when(varietyRepository.findById(1)).thenReturn(Optional.of(variety(1)));
                when(productSegmentRepository.findById(2)).thenReturn(Optional.of(segment(2, "Standard")));
        }

        private void mockOwnedProductLookup(User artisanUser, Product product) {
                when(userRepository.findByEmail("artisan@test.com"))
                                .thenReturn(Optional.of(artisanUser));
                when(productRepository.findByProductIdAndCreatedByUserId(product.getProductId(),
                                artisanUser.getUserId()))
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
                                .style("Formal Upright")
                                .price(new BigDecimal("1500000"))
                                .productStatus("DRAFT")
                                .build();
        }

        private Product product(Integer productId, User artisanUser, String productStatus, Boolean isVisible) {
                return Product.builder()
                                .productId(productId)
                                .createdBy(artisanUser)
                                .productName("Bonsai " + productId)
                                .productStatus(productStatus)
                                .isVisible(isVisible)
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

        private User artisan(Integer userId, String email) {
                return User.builder()
                                .userId(userId)
                                .email(email)
                                .fullName("Artisan " + userId)
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
}
