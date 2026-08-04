package com.example.bonsai_shop.customer.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class WishlistMvcController {

    @GetMapping("/wishlist")
    public String viewWishlist(Authentication authentication, Model model) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch("ROLE_CUSTOMER"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        model.addAttribute("activePage", "wishlist");
        return "customer/wishlist";
    }
}
