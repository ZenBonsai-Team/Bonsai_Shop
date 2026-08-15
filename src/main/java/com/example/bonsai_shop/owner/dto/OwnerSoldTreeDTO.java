package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO hien thi mot cay da ban tren bao cao Owner.
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
