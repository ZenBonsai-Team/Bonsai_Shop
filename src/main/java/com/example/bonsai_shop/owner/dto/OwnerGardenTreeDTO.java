package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerGardenTreeDTO(
        Integer productId,
        String productCode,
        String productName,
        String varietyName,
        String artisanName,
        String productStatus,
        BigDecimal price,
        int viewCount,
        LocalDateTime createdAt
) {
}
