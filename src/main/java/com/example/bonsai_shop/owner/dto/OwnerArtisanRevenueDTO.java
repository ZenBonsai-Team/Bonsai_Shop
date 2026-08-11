package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;

public record OwnerArtisanRevenueDTO(
        Integer artisanId,
        String artisanName,
        String artisanEmail,
        long treesSold,
        BigDecimal revenue
) {
}
