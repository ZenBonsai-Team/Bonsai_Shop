package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerSoldTreeDTO(
        Integer productId,
        String productCode,
        String productName,
        String varietyName,
        String artisanName,
        String orderCode,
        String customerName,
        BigDecimal soldAmount,
        LocalDateTime soldAt
) {
}
