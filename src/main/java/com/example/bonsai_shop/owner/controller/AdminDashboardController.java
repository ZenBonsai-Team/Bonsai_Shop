package com.example.bonsai_shop.owner.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('OWNER')")
public class AdminDashboardController {

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("role", "OWNER");
        model.addAttribute("activeMenu", "admin-dashboard");
        return "owner/dashboard";
    }
}
