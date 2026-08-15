package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.owner.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@RequestParam(value = "month", required = false) String month,
                            Model model) {
        YearMonth selectedMonth = parseSelectedMonth(month);
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("dashboard", ownerDashboardService.getDashboard(selectedMonth));
        model.addAttribute("reportPeriodLabel", ownerDashboardService.getReportPeriodLabel(selectedMonth));
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("monthlyRevenue", ownerDashboardService.getMonthlyCompletedProductRevenue(selectedMonth));
        model.addAttribute("monthlyShippingFee", ownerDashboardService.getMonthlyShippingFee(selectedMonth));
        model.addAttribute("monthlyCraneFee", ownerDashboardService.getMonthlyCraneFee(selectedMonth));
        model.addAttribute("monthlyForfeitedDepositIncome", ownerDashboardService.getMonthlyForfeitedDepositIncome(selectedMonth));
        model.addAttribute("monthlyRefundAmount", ownerDashboardService.getMonthlyRefundAmount(selectedMonth));
        model.addAttribute("completedRevenueSources", ownerDashboardService.getCompletedRevenueSources(selectedMonth));
        model.addAttribute("forfeitedDepositSources", ownerDashboardService.getForfeitedDepositSources(selectedMonth));
        model.addAttribute("refundSources", ownerDashboardService.getRefundSources(selectedMonth));
        model.addAttribute("shippingFeeSources", ownerDashboardService.getShippingFeeSources(selectedMonth));
        model.addAttribute("craneFeeSources", ownerDashboardService.getCraneFeeSources(selectedMonth));
        return "owner/dashboard";
    }

    @GetMapping("/dashboard/sold-trees")
    public String soldTrees(Model model) {
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("soldTrees", ownerDashboardService.getSoldTrees());
        return "owner/sold_trees";
    }

    @GetMapping("/dashboard/garden-trees")
    public String gardenTrees(Model model) {
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("gardenTrees", ownerDashboardService.getGardenTrees());
        return "owner/garden_trees";
    }

    @GetMapping("/dashboard/artisan-revenue")
    public String artisanRevenue(@RequestParam(value = "month", required = false) String month,
                                 Model model) {
        YearMonth selectedMonth = parseSelectedMonth(month);
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("reportMonth", ownerDashboardService.getMonthLabel(selectedMonth));
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("artisanRevenue", ownerDashboardService.getRevenueByArtisan(selectedMonth));
        return "owner/artisan_revenue";
    }

    private YearMonth parseSelectedMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ignored) {
            return YearMonth.now();
        }
    }
}
