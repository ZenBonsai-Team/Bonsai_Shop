package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Controller
@RequestMapping("/artisan")
@RequiredArgsConstructor
public class ArtisanDashboardController {

    private final ArtisanProductService artisanProductService;
    private final FinancialLedgerRepository financialLedgerRepository;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Product> products;
        long monthlySoldItems;
        BigDecimal monthlyRevenue;

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);
        List<FinancialLedgerType> feeLedgerTypes = List.copyOf(EnumSet.of(
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                FinancialLedgerType.FULL_REFUND
        ));

        products = artisanProductService.getMyProducts(userDetails.getUsername());
        User artisanUser = artisanProductService.getArtisanUser(userDetails.getUsername());
        monthlySoldItems = financialLedgerRepository.countCompletedSoldItemsByArtisan(
                artisanUser.getUserId(),
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                nextMonthStart
        );
        BigDecimal monthlyProductRevenue = financialLedgerRepository.sumCompletedProductRevenueByArtisan(
                artisanUser.getUserId(),
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                nextMonthStart
        );
        BigDecimal monthlyShippingFee = financialLedgerRepository.sumShippingFeeByArtisanAndLedgerTypes(
                artisanUser.getUserId(),
                feeLedgerTypes,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                nextMonthStart
        );
        BigDecimal monthlyCraneFee = financialLedgerRepository.sumCraneFeeByArtisanAndLedgerTypes(
                artisanUser.getUserId(),
                feeLedgerTypes,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                nextMonthStart
        );
        BigDecimal monthlyForfeitedDepositIncome = financialLedgerRepository.sumLedgerAmountByArtisan(
                artisanUser.getUserId(),
                FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                nextMonthStart
        );
        BigDecimal monthlyRefundAmount = financialLedgerRepository.sumLedgerAmountByArtisan(
                artisanUser.getUserId(),
                FinancialLedgerType.FULL_REFUND,
                FinancialLedgerStatus.RECORDED,
                monthStart,
                nextMonthStart
        );
        monthlyRevenue = monthlyProductRevenue;

        User artisanOrAdmin = artisanProductService.getArtisanUser(userDetails.getUsername());

        model.addAttribute("totalProducts", products.size());
        model.addAttribute("publishedProducts", products.stream()
                .filter(product -> "AVAILABLE".equals(product.getProductStatus()) && !Boolean.FALSE.equals(product.getIsVisible()))
                .count());
        model.addAttribute("soldProducts", products.stream()
                .filter(product -> "SOLD".equals(product.getProductStatus()))
                .count());

        model.addAttribute("monthlySoldItems", monthlySoldItems);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("monthlyShippingFee", monthlyShippingFee);
        model.addAttribute("monthlyCraneFee", monthlyCraneFee);
        model.addAttribute("monthlyForfeitedDepositIncome", monthlyForfeitedDepositIncome);
        model.addAttribute("monthlyRefundAmount", monthlyRefundAmount);
        model.addAttribute("draftProducts", products.stream()
                .filter(product -> "DRAFT".equals(product.getProductStatus()))
                .count());
        model.addAttribute("hiddenProducts", products.stream()
                .filter(product -> Boolean.FALSE.equals(product.getIsVisible()))
                .count());
        model.addAttribute("reservedProducts", products.stream()
                .filter(product -> "RESERVED".equals(product.getProductStatus()))
                .count());
        model.addAttribute("missingDescriptionProducts", products.stream()
                .filter(product -> product.getDescription() == null || product.getDescription().isBlank())
                .count());
        model.addAttribute("hiddenPriceProducts", products.stream()
                .filter(product -> !Boolean.TRUE.equals(product.getIsPublicPrice()))
                .count());
        
        // Get up to 5 most recent products for the dynamic dashboard view
        List<Product> recentProducts = products.stream()
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("recentProducts", recentProducts);

        model.addAttribute("artisan", artisanOrAdmin);
        return "artisan/dashboard";
    }
}
