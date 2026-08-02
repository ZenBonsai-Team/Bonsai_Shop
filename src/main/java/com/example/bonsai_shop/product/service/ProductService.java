package com.example.bonsai_shop.product.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.ProductTag;
import com.example.bonsai_shop.entity.Tag;
import com.example.bonsai_shop.product.dto.ProductCardDTO;
import com.example.bonsai_shop.product.dto.ProductMediaDTO;
import com.example.bonsai_shop.product.repository.ProductMediaRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSpecifications;
import com.example.bonsai_shop.product.repository.ProductTagRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductTagRepository productTagRepository;

    public Page<ProductCardDTO> getMarketplaceProducts(Pageable pageable) {
        return productRepository.findMarketplaceProducts(pageable);
    }

    public Page<Product> getAllActiveProducts(Pageable pageable) {
        return productRepository.findAllActiveProducts(pageable);
    }

    public Page<Product> getAvailableProductsOnly(Pageable pageable) {
        return productRepository.findAvailableProductsOnly(pageable);
    }

    public Page<Product> getFilteredProducts(
            String keyword,
            String status,
            Boolean availableOnly,
            String segment,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<String> ages,
            List<String> species,
            List<String> styles,
            List<String> priceRanges,
            Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.filterProducts(
                        keyword, status, availableOnly, segment, category, minPrice, maxPrice, ages, species, styles, priceRanges
                ),
                pageable
        );
    }

    public Page<ProductCardDTO> getPremiumProducts(Pageable pageable) {
        return productRepository.findPremiumProducts(pageable);
    }

    public ProductCardDTO getPremiumProductsById(Integer productId) {
        return productRepository.findPremiumProductById(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductMediaDTO> getPremiumProductMedia(Integer productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null
                || product.getSegment() == null
                || product.getSegment().getSegmentId() == null
                || product.getSegment().getSegmentId() != 3
                || "DRAFT".equalsIgnoreCase(product.getProductStatus())
                || Boolean.FALSE.equals(product.getIsVisible())) {
            return List.of();
        }

        return productMediaRepository.findByProductOrderByDisplayOrderAscMediaIdAsc(product).stream()
                .map(this::toProductMediaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Tag> getProductTags(Product product) {
        if (product == null) {
            return List.of();
        }
        return productTagRepository.findByProduct(product).stream()
                .map(ProductTag::getTag)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countManagedProductsByArtisan(Integer artisanUserId) {
        if (artisanUserId == null) {
            return 0;
        }
        return productRepository.countByCreatedByUserId(artisanUserId);
    }

    @Transactional(readOnly = true)
    public Product getProductById(Integer id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null && Boolean.FALSE.equals(product.getIsVisible())) {
            return null;
        }
        if (product != null) {
            if (product.getProductMedias() != null) {
                product.getProductMedias().size();
            }
            if (product.getReviews() != null) {
                product.getReviews().size();
            }
        }
        return product;
    }

    @Transactional
    public boolean incrementViewCountForCustomer(Integer productId, Authentication authentication) {
        if (productId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isCustomer = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CUSTOMER"::equals);

        if (isCustomer) {
            return productRepository.incrementViewCount(productId) > 0;
        }
        return false;
    }

    public List<Product> getTop5MostViewed() {
        return productRepository
                .findTop5ByProductStatusAndIsVisibleTrueOrderByViewCountDesc("AVAILABLE");
    }

    @Transactional(readOnly = true)
    public List<Product> getRelatedProducts(Product product, int limit) {
        if (product == null || product.getVariety() == null) {
            return List.of();
        }
        Integer varietyId = product.getVariety().getVarietyId();
        Integer currentProductId = product.getProductId();
        
        final List<Product> relatedByVariety = productRepository.findAll().stream()
                .filter(p -> p.getVariety() != null 
                        && varietyId.equals(p.getVariety().getVarietyId())
                        && !currentProductId.equals(p.getProductId())
                        && !Boolean.FALSE.equals(p.getIsVisible())
                        && !"DRAFT".equalsIgnoreCase(p.getProductStatus()))
                .limit(limit)
                .toList();
                
        List<Product> related = new java.util.ArrayList<>(relatedByVariety);
                
        if (related.size() < limit && product.getVariety().getCategory() != null) {
            Integer categoryId = product.getVariety().getCategory().getCategoryId();
            final List<Product> currentRelated = List.copyOf(related);
            List<Product> additional = productRepository.findAll().stream()
                    .filter(p -> p.getVariety() != null 
                            && p.getVariety().getCategory() != null
                            && categoryId.equals(p.getVariety().getCategory().getCategoryId())
                            && !currentProductId.equals(p.getProductId())
                            && !Boolean.FALSE.equals(p.getIsVisible())
                            && !"DRAFT".equalsIgnoreCase(p.getProductStatus())
                            && !currentRelated.contains(p))
                    .limit(limit - related.size())
                    .toList();
            
            related.addAll(additional);
        }
        
        if (related.size() < limit) {
            final List<Product> currentRelated = List.copyOf(related);
            List<Product> fallback = productRepository.findAll().stream()
                    .filter(p -> !currentProductId.equals(p.getProductId())
                            && !Boolean.FALSE.equals(p.getIsVisible())
                            && !"DRAFT".equalsIgnoreCase(p.getProductStatus())
                            && !currentRelated.contains(p))
                    .limit(limit - related.size())
                    .toList();
            related.addAll(fallback);
        }
        
        related.forEach(p -> {
            if (p.getProductMedias() != null) {
                p.getProductMedias().size();
            }
        });
        
        return related;
    }


    private ProductMediaDTO toProductMediaDTO(ProductMedia media) {
        return new ProductMediaDTO(
                media.getMediaId(),
                media.getMediaUrl(),
                media.getMediaType(),
                media.getSlotType(),
                media.getCaption(),
                media.getIsThumbnail(),
                media.getDisplayOrder()
        );
    }
}
