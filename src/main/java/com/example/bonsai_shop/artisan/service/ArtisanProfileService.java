package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.artisan.dto.ArtisanProfileFormDTO;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.entity.ArtisanProfile;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.ArtisanProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ArtisanProfileService {

    private final ArtisanProfileRepository artisanProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ArtisanProfile createDefaultProfileIfArtisan(User user) {
        if (user == null || user.getUserId() == null || !isArtisanRole(user.getRole())) {
            return null;
        }

        return artisanProfileRepository.findByUserId(user.getUserId())
                .orElseGet(() -> artisanProfileRepository.save(ArtisanProfile.builder()
                        .userId(user.getUserId())
                        .fullName(defaultFullName(user))
                        .bio("")
                        .yearsOfExperience(0)
                        .specialty("")
                        .build()));
    }

    @Transactional
    public ArtisanProfile getOrCreateProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy artisan!"));

        ArtisanProfile artisanProfile = createDefaultProfileIfArtisan(user);
        if (artisanProfile == null) {
            throw new RuntimeException("Tài khoản không phải artisan!");
        }

        return artisanProfile;
    }

    @Transactional
    public ArtisanProfile updateProfile(String email, ArtisanProfileFormDTO form) {
        ArtisanProfile artisanProfile = getOrCreateProfile(email);
        artisanProfile.setFullName(requireText(form.getFullName(), "Vui lòng nhập tên nghệ nhân."));
        artisanProfile.setBio(blankToEmpty(form.getBio()));
        artisanProfile.setYearsOfExperience(form.getYearsOfExperience() == null ? 0 : form.getYearsOfExperience());
        artisanProfile.setSpecialty(blankToEmpty(form.getSpecialty()));
        return artisanProfileRepository.save(artisanProfile);
    }

    public ArtisanProfileFormDTO toFormDTO(ArtisanProfile artisanProfile) {
        return ArtisanProfileFormDTO.builder()
                .fullName(artisanProfile.getFullName())
                .bio(artisanProfile.getBio())
                .yearsOfExperience(artisanProfile.getYearsOfExperience())
                .specialty(artisanProfile.getSpecialty())
                .build();
    }

    private boolean isArtisanRole(Role role) {
        if (role == null || role.getRoleName() == null) {
            return false;
        }

        String normalized = role.getRoleName().trim().toUpperCase(Locale.ROOT);
        return "ARTISAN".equals(normalized) || "ROLE_ARTISAN".equals(normalized);
    }

    private String defaultFullName(User user) {
        return requireText(user.getFullName(), "Tên nghệ nhân");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
