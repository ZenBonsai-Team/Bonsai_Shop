package com.example.bonsai_shop.owner.dto;

import java.math.BigDecimal;
import java.util.List;

public record OwnerDashboardDTO(
        BigDecimal totalRevenue,
        BigDecimal revenueThisMonth,
        long treesInGarden,
        long treesSold,
        List<OwnerMonthlyRevenueDTO> monthlyRevenue,
        List<OwnerTopViewedTreeDTO> topViewedTrees
) {
}
