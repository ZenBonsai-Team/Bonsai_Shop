package com.example.bonsai_shop.artisan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/artisan")
@RequiredArgsConstructor
public class ArtisanHomeController {

    @GetMapping
    public String redirectToProducts() {
        return "redirect:/artisan/products";
    }
}
