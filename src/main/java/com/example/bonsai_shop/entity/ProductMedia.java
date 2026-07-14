package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PRODUCT_MEDIA")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MediaID")
    private Integer mediaId;

    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(name = "MediaURL", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "MediaType", length = 50)
    private String mediaType;

    @Column(name = "SlotType", length = 50)
    private String slotType;

    @Column(name = "Caption", length = 255)
    private String caption;

    @Column(name = "IsThumbnail")
    private Boolean isThumbnail = true;

    @Column(name = "DisplayOrder")
    private Integer displayOrder = 0;
}
