package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO;
import com.example.bonsai_shop.owner.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

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
        model.addAttribute("monthlyRevenue", ownerDashboardService.getMonthlyRevenueByArtisanTotal(selectedMonth));
        model.addAttribute("monthlyShippingFee", ownerDashboardService.getMonthlyShippingFee(selectedMonth));
        model.addAttribute("monthlyCraneFee", ownerDashboardService.getMonthlyCraneFee(selectedMonth));
        model.addAttribute("monthlyForfeitedDepositIncome", ownerDashboardService.getMonthlyForfeitedDepositIncome(selectedMonth));
        model.addAttribute("monthlyRefundAmount", ownerDashboardService.getMonthlyRefundAmount(selectedMonth));
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
        model.addAttribute("artisanRevenueDetails", ownerDashboardService.getRevenueDetailsByArtisan(selectedMonth));
        return "owner/artisan_revenue";
    }

    @GetMapping("/dashboard/forfeited-deposits")
    public String forfeitedDeposits(@RequestParam(value = "month", required = false) String month,
                                    Model model) {
        YearMonth selectedMonth = parseSelectedMonth(month);
        return financeSourceReport(
                model,
                selectedMonth,
                "Cọc khách bỏ",
                "Các đơn có khoản cọc khách bỏ trong tháng " + ownerDashboardService.getMonthLabel(selectedMonth) + ".",
                "Cọc giữ lại",
                ownerDashboardService.getMonthlyForfeitedDepositIncome(selectedMonth),
                ownerDashboardService.getForfeitedDepositSources(selectedMonth)
        );
    }

    @GetMapping("/dashboard/customer-refunds")
    public String customerRefunds(@RequestParam(value = "month", required = false) String month,
                                  Model model) {
        YearMonth selectedMonth = parseSelectedMonth(month);
        return financeSourceReport(
                model,
                selectedMonth,
                "Hoàn lại khách",
                "Các đơn có khoản hoàn tiền cho khách trong tháng " + ownerDashboardService.getMonthLabel(selectedMonth) + ".",
                "Số tiền hoàn",
                ownerDashboardService.getMonthlyRefundAmount(selectedMonth),
                ownerDashboardService.getRefundSources(selectedMonth)
        );
    }

    @GetMapping("/dashboard/shipping-fees")
    public String shippingFees(@RequestParam(value = "month", required = false) String month,
                               Model model) {
        YearMonth selectedMonth = parseSelectedMonth(month);
        return financeSourceReport(
                model,
                selectedMonth,
                "Phí ship",
                "Các đơn có phí ship được ghi nhận trong tháng " + ownerDashboardService.getMonthLabel(selectedMonth) + ".",
                "Phí ship",
                ownerDashboardService.getMonthlyShippingFee(selectedMonth),
                ownerDashboardService.getShippingFeeSources(selectedMonth)
        );
    }

    @GetMapping("/dashboard/crane-fees")
    public String craneFees(@RequestParam(value = "month", required = false) String month,
                            Model model) {
        YearMonth selectedMonth = parseSelectedMonth(month);
        return financeSourceReport(
                model,
                selectedMonth,
                "Phí cẩu",
                "Các đơn có phí cẩu được ghi nhận trong tháng " + ownerDashboardService.getMonthLabel(selectedMonth) + ".",
                "Phí cẩu",
                ownerDashboardService.getMonthlyCraneFee(selectedMonth),
                ownerDashboardService.getCraneFeeSources(selectedMonth)
        );
    }

    private String financeSourceReport(Model model,
                                       YearMonth selectedMonth,
                                       String title,
                                       String description,
                                       String amountColumnLabel,
                                       BigDecimal totalAmount,
                                       List<ArtisanDashboardSourceDTO> sources) {
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("reportTitle", title);
        model.addAttribute("reportDescription", description);
        model.addAttribute("amountColumnLabel", amountColumnLabel);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("sources", sources);
        model.addAttribute("selectedMonth", selectedMonth.toString());
        return "owner/finance_sources";
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
