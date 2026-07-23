package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "PRODUCT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productId;

    @ManyToOne
    @JoinColumn(name = "CreatedByID")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "ArtisanID")
    private ArtisanProfile artisan;

    @ManyToOne
    @JoinColumn(name = "VarietyID", nullable = false)
    private Variety variety;

    @ManyToOne
    @JoinColumn(name = "SegmentID", nullable = false)
    private ProductSegment segment;

    @Column(name = "ProductCode", nullable = false, unique = true, length = 100)
    private String productCode;

    @Column(name = "ProductName", nullable = false, length = 255)
    private String productName;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "Age")
    private Integer age;

    @Column(name = "Height")
    private Float height;

    @Column(name = "TrunkDiameter")
    private Float trunkDiameter;

    @Column(name = "Style", length = 255)
    private String style;

    @Column(name = "Price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "IsPublicPrice")
    private Boolean isPublicPrice = true;

    @Column(name = "ProductStatus", length = 50)
    private String productStatus = "AVAILABLE";

    @Column(name = "ViewCount")
    private Integer viewCount = 0;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductTag> productTags;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductMedia> productMedias;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Wishlist> wishlists;

    public ProductMedia getPrimaryMedia() {
        if (productMedias == null || productMedias.isEmpty()) {
            return null;
        }
        return productMedias.stream()
                .filter(media -> Boolean.TRUE.equals(media.getIsThumbnail()))
                .min(mediaDisplayComparator())
                .orElseGet(() -> productMedias.stream()
                        .min(mediaDisplayComparator())
                        .orElse(null));
    }

    public String getPrimaryMediaUrl() {
        ProductMedia primaryMedia = getPrimaryMedia();
        return primaryMedia != null ? primaryMedia.getMediaUrl() : null;
    }

    public String getPrimaryMediaType() {
        ProductMedia primaryMedia = getPrimaryMedia();
        return primaryMedia != null ? primaryMedia.getMediaType() : null;
    }

    public String getFirstImageUrl() {
        if (productMedias == null || productMedias.isEmpty()) {
            return null;
        }
        return productMedias.stream()
                .filter(media -> "IMAGE".equals(media.getMediaType()))
                .filter(media -> Boolean.TRUE.equals(media.getIsThumbnail()))
                .min(mediaDisplayComparator())
                .or(() -> productMedias.stream()
                        .filter(media -> "IMAGE".equals(media.getMediaType()))
                        .min(mediaDisplayComparator()))
                .map(ProductMedia::getMediaUrl)
                .orElse(null);
    }

    private Comparator<ProductMedia> mediaDisplayComparator() {
        return Comparator
                .comparing(ProductMedia::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProductMedia::getMediaId, Comparator.nullsLast(Integer::compareTo));
    }

    public boolean isMidSegment() {
        if (segment == null || segment.getSegmentName() == null) {
            return false;
        }
        String segmentName = segment.getSegmentName().toLowerCase();
        return segmentName.contains("mid")
                || segmentName.contains("middle")
                || segmentName.contains("standard")
                || segmentName.contains("trung");
    }

    public Double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}
