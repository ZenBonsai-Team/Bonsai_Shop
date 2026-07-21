package com.example.bonsai_shop.moderator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/moderator/orders")
public class ModeratorOrderController {

    @GetMapping
    public String viewOrdersDashboardRedirect() {
        return "redirect:/moderator/orders/pool";
    }

    @GetMapping("/pool")
    public String viewOrdersPool(Model model) {
        model.addAttribute("role", "MODERATOR");
        model.addAttribute("activePage", "orders-pool");
        model.addAttribute("activePageLabel", "Orders Pool - Kho Đơn Hàng Chung");
        return "moderator/orders_pool";
    }

    @GetMapping("/my")
    public String viewMyOrders(Model model) {
        model.addAttribute("role", "MODERATOR");
        model.addAttribute("activePage", "my-orders");
        model.addAttribute("activePageLabel", "Đơn hàng của tôi (My Orders)");
        return "moderator/my_orders";
    }
}

