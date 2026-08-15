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

    // Hien thi danh sach tat ca tai khoan de Owner quan ly trang thai va thong tin co ban.
    @GetMapping
    public String listAccounts(Model model) {
        model.addAttribute("users", accountService.findAll());
        return "owner/user_list";
    }

    // Mo form tao tai khoan nhan vien va nap cac role duoc phep gan.
    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("roles", accountService.findAssignableRoles());
        return "owner/users_create";
    }

    // Xu ly tao tai khoan moi tu form Owner, neu validate loi thi giu lai du lieu da nhap.
    @PostMapping("/create")
    public String createAccount(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String password,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) Integer roleId,
                                RedirectAttributes redirectAttributes) {
        try {
            // Goi service de chuan hoa, validate va luu tai khoan moi.
            accountService.createAccount(fullName, email, password, phone, roleId);
            redirectAttributes.addFlashAttribute("success",
                    "Tao tai khoan thanh cong cho " + email + "!");
        } catch (RuntimeException e) {
            // Dua thong bao loi va cac gia tri form ve redirect de nguoi dung sua lai.
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("fullName", fullName);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phone", phone);
            redirectAttributes.addFlashAttribute("roleId", roleId);
            return "redirect:/owner/users/create";
        }
        return "redirect:/owner/users";
    }

    // Doi qua lai giua ACTIVE va LOCKED cho tai khoan duoc chon.
    @PostMapping("/toggle-status")
    public String toggleStatus(@RequestParam Integer userId,
                               RedirectAttributes redirectAttributes) {
        try {
            // Service tra ve true khi hanh dong vua thuc hien la khoa tai khoan.
            boolean locked = accountService.toggleAccountStatus(userId);
            redirectAttributes.addFlashAttribute("success",
                    locked ? "Da khoa tai khoan!" : "Da mo khoa tai khoan!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/users";
    }

    // Khoa rieng mot tai khoan, dung cho nut khoa ro rang tren man hinh danh sach.
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

    // Mo khoa rieng mot tai khoan dang bi LOCKED.
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
