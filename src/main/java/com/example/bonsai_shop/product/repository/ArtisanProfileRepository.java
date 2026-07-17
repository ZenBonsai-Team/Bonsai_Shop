package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.ArtisanProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtisanProfileRepository extends JpaRepository<ArtisanProfile, Integer> {
    Optional<ArtisanProfile> findByUserId(Integer userId);
}
