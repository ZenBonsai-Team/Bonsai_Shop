package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PRODUCT_JOURNAL_EVENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductJournalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EventID")
    private Integer eventId;

    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "CreatedByID")
    private User createdBy;

    @Column(name = "EventDate", nullable = false)
    private LocalDate eventDate;

    @Column(name = "EventType", nullable = false, length = 50)
    @Builder.Default
    private String eventType = "PHOTO_UPDATE";

    @Column(name = "Title", nullable = false, length = 255)
    private String title;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "IsPublic")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "CreatedAt")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, mediaId ASC")
    @Builder.Default
    private List<ProductJournalMedia> mediaList = new ArrayList<>();

    public ProductJournalMedia getFirstMedia() {
        if (mediaList == null || mediaList.isEmpty()) {
            return null;
        }
        return mediaList.get(0);
    }
}
