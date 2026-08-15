package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;
import java.util.List;

// DTO tong hop tat ca chi so chinh tren dashboard Owner.
public record OwnerDashboardDTO(
        BigDecimal totalRevenue,
        BigDecimal revenueThisMonth,
        long treesInGarden,
        long treesSold,
        List<OwnerMonthlyRevenueDTO> monthlyRevenue,
        List<OwnerTopViewedTreeDTO> topViewedTrees
) {
}
