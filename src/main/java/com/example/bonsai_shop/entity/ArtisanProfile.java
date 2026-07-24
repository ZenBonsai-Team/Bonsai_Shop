package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;

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
}
