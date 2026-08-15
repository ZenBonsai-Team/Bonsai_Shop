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

    // Hien thi dashboard tong quan cho Owner theo thang duoc chon.
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@RequestParam(value = "month", required = false) String month,
                            Model model) {
        // Chuyen query param month thanh YearMonth, neu sai thi mac dinh thang hien tai.
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

    // Hien thi danh sach cay da ban de Owner drill-down tu chi so tren dashboard.
    @GetMapping("/dashboard/sold-trees")
    public String soldTrees(Model model) {
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("soldTrees", ownerDashboardService.getSoldTrees());
        return "owner/sold_trees";
    }

    // Hien thi danh sach cay con trong vuon.
    @GetMapping("/dashboard/garden-trees")
    public String gardenTrees(Model model) {
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("gardenTrees", ownerDashboardService.getGardenTrees());
        return "owner/garden_trees";
    }

    // Hien thi doanh thu theo tung artisan trong thang bao cao.
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

    // Bao cao nguon tien coc bi giu lai trong thang.
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

    // Bao cao cac khoan hoan tien cho khach trong thang.
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

    // Bao cao cac phi van chuyen duoc ghi nhan trong thang.
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

    // Bao cao cac phi cau/chuyen cay duoc ghi nhan trong thang.
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

    // Gom du lieu dung chung cho cac man hinh bao cao nguon tien.
    private String financeSourceReport(Model model,
                                       YearMonth selectedMonth,
                                       String title,
                                       String description,
                                       String amountColumnLabel,
                                       BigDecimal totalAmount,
                                       List<ArtisanDashboardSourceDTO> sources) {
        // Gan cac attribute chung de template finance_sources render dung tieu de, tong tien va danh sach nguon.
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

    // Parse thang bao cao tu query param yyyy-MM, fallback ve thang hien tai neu bo trong hoac sai dinh dang.
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
