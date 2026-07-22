package com.example.bonsai_shop.customer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CartMvcController {

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("activePage", "cart");
        return "customer/cart";
    }

    @GetMapping("/checkout")
    public String viewCheckout(Model model) {
        model.addAttribute("activePage", "checkout");
        return "customer/checkout";
    }

    @GetMapping("/order/success")
    public String viewOrderSuccess(@RequestParam String orderCode, Model model) {
        model.addAttribute("activePage", "orders");
        model.addAttribute("orderCode", orderCode);
        return "customer/order_success";
    }
}
