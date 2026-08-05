package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "live_lead")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LiveLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LeadID")
    private Integer leadId;

    @ManyToOne
    @JoinColumn(name = "SessionID", nullable = false)
    private LiveSession liveSession;

    @ManyToOne
    @JoinColumn(name = "ProductID")
    private Product product;

    @Column(name = "ViewerName", nullable = false, length = 150)
    private String viewerName;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "RawComment", nullable = false, columnDefinition = "TEXT")
    private String rawComment;

    /** CHOT_DON | TU_VAN | GOI_LAI */
    @Column(name = "IntentType", nullable = false, length = 50)
    private String intentType;

    /** PENDING | CONTACTED | DONE */
    @Column(name = "LeadStatus", nullable = false, length = 50)
    private String leadStatus = "PENDING";

    @Column(name = "Notes", length = 500)
    private String notes;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
