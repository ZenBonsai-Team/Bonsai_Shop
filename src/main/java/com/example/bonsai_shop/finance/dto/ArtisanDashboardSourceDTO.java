package com.example.bonsai_shop.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ArtisanDashboardSourceDTO(
        String orderCode,
        String customerName,
        LocalDateTime sourceDate,
        String orderStatus,
        String productCode,
        String productName,
        BigDecimal productAmount,
        BigDecimal depositAmount,
        BigDecimal refundAmount,
        BigDecimal shippingFee,
        BigDecimal craneFee,
        BigDecimal sourceAmount
) {
}
