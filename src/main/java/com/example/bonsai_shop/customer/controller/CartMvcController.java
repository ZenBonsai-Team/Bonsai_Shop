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

@Controller
@RequiredArgsConstructor
public class CartMvcController {

    private final UserService userService;

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("activePage", "cart");
        return "customer/cart";
    }

    @GetMapping("/checkout")
    public String viewCheckout(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("activePage", "checkout");
        if (userDetails != null) {
            User user = userService.getCurrentUserProfile(userDetails.getUsername());
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
}
