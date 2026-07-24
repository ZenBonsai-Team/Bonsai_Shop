package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.product.service.OrderService;

import com.example.bonsai_shop.config.SecurityUtils;

@Controller
@RequiredArgsConstructor
public class CartMvcController {

    private final UserService userService;
    private final OrderService orderService;

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("activePage", "cart");
        return "customer/cart";
    }

    @GetMapping("/checkout")
    public String viewCheckout(@AuthenticationPrincipal Object principal, Model model) {
        model.addAttribute("activePage", "checkout");
        String email = SecurityUtils.extractEmail(principal);
        if (email != null) {
            User user = userService.getCurrentUserProfile(email);
            model.addAttribute("user", user);
        }
        return "customer/checkout";
    }

    @GetMapping("/order/success")
    public String viewOrderSuccess(@RequestParam String orderCode, Model model) {
        model.addAttribute("activePage", "orders");
        model.addAttribute("orderCode", orderCode);
        return "customer/order_success";
    }

    @GetMapping({"/order/lookup", "/lookup", "/orders/lookup"})
    public String viewOrderLookup(@RequestParam(required = false) String orderCode, Model model) {
        model.addAttribute("activePage", "orderLookup");
        boolean searched = false;
        Order order = null;
        String searchCode = "";

        if (orderCode != null && !orderCode.trim().isEmpty()) {
            searchCode = orderCode.trim();
            searched = true;
            order = orderService.getOrderByCodeWithDetails(searchCode);
        }

        model.addAttribute("searched", searched);
        model.addAttribute("order", order);
        model.addAttribute("searchCode", searchCode);
        return "customer/order_lookup";
    }
}
