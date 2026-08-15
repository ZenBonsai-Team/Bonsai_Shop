package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;

// DTO hien thi cay co luot xem cao tren dashboard Owner.
public record OwnerTopViewedTreeDTO(
        Integer productId,
        String productCode,
        String productName,
        String varietyName,
        String productStatus,
        BigDecimal price,
        int viewCount,
        String primaryImageUrl
) {
}
