package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "artisan_profile", uniqueConstraints = {
        @UniqueConstraint(name = "uq_artisan_profile_user_id", columnNames = "UserID")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtisanProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ArtisanID")
    private Integer artisanId;

    @Column(name = "UserID", unique = true)
    private Integer userId;

    @Column(name = "FullName", nullable = false, length = 255)
    private String fullName;

    @Column(name = "Bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "YearsOfExperience")
    private Integer yearsOfExperience;

    @Column(name = "Specialty", length = 255)
    private String specialty;

    @Column(name = "CoverImageUrl", length = 500)
    private String coverImageUrl;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
