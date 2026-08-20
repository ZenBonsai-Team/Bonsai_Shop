package com.example.bonsai_shop.artisan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtisanMediaItemDTO {
    private String mediaUrl;
    private String mediaType;
    private String slotType;
    private String caption;
    private Boolean isThumbnail;
    private Integer displayOrder;
}
