package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;

public record OwnerArtisanRevenueDTO(
        Integer artisanId,
        String artisanName,
        String artisanEmail,
        Long treesSold,
        BigDecimal revenue
) {
    public OwnerArtisanRevenueDTO {
        artisanName = artisanName == null || artisanName.isBlank() ? "Chưa gán nghệ nhân" : artisanName;
        artisanEmail = artisanEmail == null || artisanEmail.isBlank() ? "-" : artisanEmail;
        treesSold = treesSold != null ? treesSold : 0L;
        revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }
}
