package com.example.bonsai_shop.owner.dto;

import com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO;

import java.util.List;

public record OwnerArtisanRevenueDetailDTO(
        OwnerArtisanRevenueDTO summary,
        List<ArtisanDashboardSourceDTO> completedRevenueSources
) {
    public String sourcePanelId() {
        return "artisan-revenue-sources-" + summary.artisanId();
    }
}
