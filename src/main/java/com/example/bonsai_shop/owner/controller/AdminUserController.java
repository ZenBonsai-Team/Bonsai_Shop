package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.owner.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/owner/users")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AccountService accountService;

    @GetMapping
    public String listAccounts(Model model) {
        model.addAttribute("users", accountService.findAll());
        return "owner/user_list";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("roles", accountService.findAssignableRoles());
        return "owner/users_create";
    }

    @PostMapping("/create")
    public String createAccount(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String password,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) Integer roleId,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.createAccount(fullName, email, password, phone, roleId);
            redirectAttributes.addFlashAttribute("success",
                    "Tao tai khoan thanh cong cho " + email + "!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("fullName", fullName);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phone", phone);
            redirectAttributes.addFlashAttribute("roleId", roleId);
            return "redirect:/owner/users/create";
        }
        return "redirect:/owner/users";
    }

    @PostMapping("/toggle-status")
    public String toggleStatus(@RequestParam Integer userId,
                               RedirectAttributes redirectAttributes) {
        try {
            boolean locked = accountService.toggleAccountStatus(userId);
            redirectAttributes.addFlashAttribute("success",
                    locked ? "Da khoa tai khoan!" : "Da mo khoa tai khoan!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/users";
    }

    @PostMapping("/lock")
    public String lockAccount(@RequestParam Integer userId,
                              RedirectAttributes redirectAttributes) {
        try {
            accountService.lockAccount(userId);
            redirectAttributes.addFlashAttribute("success", "Da khoa tai khoan!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/users";
    }

    @PostMapping("/unlock")
    public String unlockAccount(@RequestParam Integer userId,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.unlockAccount(userId);
            redirectAttributes.addFlashAttribute("success", "Da mo khoa tai khoan!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/users";
    }
}
