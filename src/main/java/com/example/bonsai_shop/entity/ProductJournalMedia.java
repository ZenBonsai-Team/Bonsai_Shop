package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PRODUCT_JOURNAL_MEDIA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductJournalMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MediaID")
    private Integer mediaId;

    @ManyToOne
    @JoinColumn(name = "EventID", nullable = false)
    private ProductJournalEvent event;

    @Column(name = "MediaURL", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "MediaType", nullable = false, length = 50)
    @Builder.Default
    private String mediaType = "IMAGE";

    @Column(name = "Caption", length = 255)
    private String caption;

    @Column(name = "DisplayOrder")
    @Builder.Default
    private Integer displayOrder = 0;
}
