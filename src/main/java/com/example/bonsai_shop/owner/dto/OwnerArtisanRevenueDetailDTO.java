package com.example.bonsai_shop.owner.dto;

import com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO;

import java.util.List;

// DTO gom summary doanh thu cua mot artisan va danh sach cac nguon doanh thu chi tiet.
public record OwnerArtisanRevenueDetailDTO(
        OwnerArtisanRevenueDTO summary,
        List<ArtisanDashboardSourceDTO> completedRevenueSources
) {
    // Tao id HTML rieng cho panel nguon doanh thu cua artisan tren giao dien.
    public String sourcePanelId() {
        return "artisan-revenue-sources-" + summary.artisanId();
    }
}
