package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;

// DTO tong hop doanh thu theo artisan trong mot thang.
public record OwnerArtisanRevenueDTO(
        Integer artisanId,
        String artisanName,
        String artisanEmail,
        Long treesSold,
        BigDecimal revenue
) {
    // Compact constructor chuan hoa null de view khong phai xu ly gia tri rong.
    public OwnerArtisanRevenueDTO {
        artisanName = artisanName == null || artisanName.isBlank() ? "Chưa gán nghệ nhân" : artisanName;
        artisanEmail = artisanEmail == null || artisanEmail.isBlank() ? "-" : artisanEmail;
        treesSold = treesSold != null ? treesSold : 0L;
        revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }
}
