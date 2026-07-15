package com.example.bonsai_shop.moderator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for handling Order Moderator pages
 * Under: src/main/java/com/example/bonsai_shop/moderator/controller/ModeratorOrderController.java
 */
@Controller
@RequestMapping("/moderator/orders")
public class ModeratorOrderController {

    /**
     * Renders the Order Moderator's Order Management Dashboard
     * 
     * @param model MVC Model to send parameters to Thymeleaf
     * @return Path to Thymeleaf template (templates/moderator/orders.html)
     */
    @GetMapping
    public String viewOrdersDashboard(Model model) {
        // Pass page identifiers to parameterize the Reusable Master Layout & Sidebar
        model.addAttribute("role", "MODERATOR");
        model.addAttribute("activePage", "orders");
        model.addAttribute("activePageLabel", "Quản lý đơn hàng (Moderator)");
        
        return "moderator/orders"; // maps to src/main/resources/templates/moderator/orders.html
    }
}
