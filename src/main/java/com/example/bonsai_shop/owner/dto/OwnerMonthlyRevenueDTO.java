package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;

// DTO mot cot doanh thu thang trong bieu do dashboard Owner.
public record OwnerMonthlyRevenueDTO(
        String monthLabel,
        BigDecimal revenue,
        int chartPercent,
        String dataLabel
) {
}
