package com.example.bonsai_shop.owner.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/owner/order-handling-history")
@PreAuthorize("hasRole('OWNER')")
public class OwnerOrderHandlingHistoryController {

    @GetMapping
    public String history(@RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "ALL") String status,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("type", "ONLINE");
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        if (search != null && !search.isBlank()) {
            redirectAttributes.addAttribute("search", search.trim());
        }
        return "redirect:/owner/order-history";
    }

    @GetMapping("/orders/{orderCode}")
    public String orderDetail(@PathVariable String orderCode) {
        return "redirect:/owner/order-history/orders/{orderCode}";
    }
}
