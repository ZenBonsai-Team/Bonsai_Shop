package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.owner.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("dashboard", ownerDashboardService.getDashboard());
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
    public String artisanRevenue(Model model) {
        model.addAttribute("role", "OWNER");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("reportMonth", ownerDashboardService.getCurrentMonthLabel());
        model.addAttribute("artisanRevenue", ownerDashboardService.getCurrentMonthRevenueByArtisan());
        return "owner/artisan_revenue";
    }
}
