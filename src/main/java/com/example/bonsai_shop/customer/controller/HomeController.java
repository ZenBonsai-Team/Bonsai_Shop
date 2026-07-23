package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
            @AuthenticationPrincipal Object principal) {

        String email = null;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            email = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
        }

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                model.addAttribute("username", user.getUsername());
                model.addAttribute("email", user.getEmail());
            }
        }
        model.addAttribute("activePage", "home");

        return "home";
    }

    @GetMapping("/contact")
    public String contact(@RequestParam(value = "tab", defaultValue = "contact") String tab,
            Model model,
            @AuthenticationPrincipal Object principal) {
        String email = null;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            email = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
        }

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                model.addAttribute("currentUser", user);
            }
        }
        
        model.addAttribute("activeTab", tab);
        model.addAttribute("activePage", "contact");
        return "customer/contact";
    }

    @GetMapping("/about")
    public String about() {
        return "redirect:/contact?tab=about";
    }

    @GetMapping("/terms")
    public String terms() {
        return "redirect:/contact?tab=terms";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "redirect:/contact?tab=privacy";
    }
}