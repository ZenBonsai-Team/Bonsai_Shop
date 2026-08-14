package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;

public record OwnerMonthlyRevenueDTO(
        String monthLabel,
        BigDecimal revenue,
        int chartPercent,
        String dataLabel
) {
}
