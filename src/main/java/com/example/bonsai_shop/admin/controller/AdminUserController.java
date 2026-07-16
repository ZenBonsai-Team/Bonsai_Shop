package com.example.bonsai_shop.admin.controller;

import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    // ===== DANH SÁCH USER =====
    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("roles", roles);
        model.addAttribute("role", "OWNER");
        model.addAttribute("activeMenu", "admin-users");
        return "admin/user_list"; // templates/admin/user_list.html
    }

    // ===== ĐỔI ROLE =====
    @PostMapping("/change-role")
    public String changeRole(@RequestParam Integer userId,
                             @RequestParam Integer roleId,
                             RedirectAttributes redirectAttributes) {
         try {
             userService.changeUserRole(userId, roleId);
             redirectAttributes.addFlashAttribute("success", "Đổi quyền thành công!");
         } catch (RuntimeException e) {
             redirectAttributes.addFlashAttribute("error", e.getMessage());
         }
         return "redirect:/admin/users";
    }

    // ===== KHÓA/MỞ KHÓA TÀI KHOẢN =====
    @PostMapping("/toggle-status")
    public String toggleStatus(@RequestParam Integer userId,
                               RedirectAttributes redirectAttributes) {
         try {
             userService.toggleUserStatus(userId);
             redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công!");
         } catch (RuntimeException e) {
             redirectAttributes.addFlashAttribute("error", e.getMessage());
         }
         return "redirect:/admin/users";
    }
}