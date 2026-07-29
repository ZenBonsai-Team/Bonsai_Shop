package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.OrderDetailRepository;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/artisan")
@RequiredArgsConstructor
public class ArtisanDashboardController {

    private final ArtisanProductService artisanProductService;
    private final OrderDetailRepository orderDetailRepository;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Product> products;
        long monthlySoldItems;
        BigDecimal monthlyRevenue;

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        products = artisanProductService.getMyProducts(userDetails.getUsername());
        User artisanUser = artisanProductService.getArtisanUser(userDetails.getUsername());
        monthlySoldItems = orderDetailRepository.countMonthlySoldItemsByArtisan(
                artisanUser.getUserId(),
                monthStart,
                nextMonthStart
        );
        monthlyRevenue = orderDetailRepository.sumMonthlyRevenueByArtisan(
                artisanUser.getUserId(),
                monthStart,
                nextMonthStart
        );

        User artisanOrAdmin = artisanProductService.getArtisanUser(userDetails.getUsername());

        model.addAttribute("totalProducts", products.size());
        model.addAttribute("publishedProducts", products.stream()
                .filter(product -> "AVAILABLE".equals(product.getProductStatus()) && !Boolean.FALSE.equals(product.getIsVisible()))
                .count());
        model.addAttribute("soldProducts", products.stream()
                .filter(product -> "SOLD".equals(product.getProductStatus()))
                .count());

        BigDecimal averageOrderValue = monthlySoldItems == 0
                ? BigDecimal.ZERO
                : monthlyRevenue.divide(BigDecimal.valueOf(monthlySoldItems), 0, RoundingMode.HALF_UP);
        model.addAttribute("monthlySoldItems", monthlySoldItems);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("averageOrderValue", averageOrderValue);
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
