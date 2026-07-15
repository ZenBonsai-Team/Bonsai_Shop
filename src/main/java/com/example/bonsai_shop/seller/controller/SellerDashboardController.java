package com.example.bonsai_shop.seller.controller;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.OrderDetailRepository;
import com.example.bonsai_shop.seller.service.SellerProductService;
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
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final SellerProductService sellerProductService;
    private final OrderDetailRepository orderDetailRepository;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        boolean isAdminOrMod = userDetails.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority())
                        || "ROLE_MODERATOR".equals(auth.getAuthority())
                        || "ROLE_ORDER_MODERATOR".equals(auth.getAuthority()));

        List<Product> products;
        long monthlySoldItems;
        BigDecimal monthlyRevenue;

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        if (isAdminOrMod) {
            products = sellerProductService.getAllProducts();
            monthlySoldItems = orderDetailRepository.countMonthlySoldItems(monthStart, nextMonthStart);
            monthlyRevenue = orderDetailRepository.sumMonthlyRevenue(monthStart, nextMonthStart);
        } else {
            products = sellerProductService.getMyProducts(userDetails.getUsername());
            User seller = sellerProductService.getSeller(userDetails.getUsername());
            monthlySoldItems = orderDetailRepository.countMonthlySoldItemsBySeller(
                    seller.getUserId(),
                    monthStart,
                    nextMonthStart
            );
            monthlyRevenue = orderDetailRepository.sumMonthlyRevenueBySeller(
                    seller.getUserId(),
                    monthStart,
                    nextMonthStart
            );
        }

        User sellerOrAdmin = sellerProductService.getSeller(userDetails.getUsername());

        model.addAttribute("totalProducts", products.size());
        model.addAttribute("publishedProducts", products.stream()
                .filter(product -> "AVAILABLE".equals(product.getProductStatus()))
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
                .filter(product -> "HIDDEN".equals(product.getProductStatus()))
                .count());
        model.addAttribute("missingDescriptionProducts", products.stream()
                .filter(product -> product.getDescription() == null || product.getDescription().isBlank())
                .count());
        model.addAttribute("hiddenPriceProducts", products.stream()
                .filter(product -> !Boolean.TRUE.equals(product.getIsPublicPrice()))
                .count());
        model.addAttribute("seller", sellerOrAdmin);
        return "seller/dashboard";
    }
}
