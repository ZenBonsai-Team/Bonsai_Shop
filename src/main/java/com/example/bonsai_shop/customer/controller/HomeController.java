package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model,
                       @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails != null) {
            String email = userDetails.getUsername();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            model.addAttribute("username", user.getUsername());
            model.addAttribute("email", user.getEmail());
        }
        model.addAttribute("activePage", "home");

        return "home";
    }
}