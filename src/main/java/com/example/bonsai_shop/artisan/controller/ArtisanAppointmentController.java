package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import com.example.bonsai_shop.artisan.service.ArtisanAppointmentService;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ArtisanAppointmentController {

    private final ArtisanAppointmentService artisanAppointmentService;
    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("artisan/appointments")
    public String viewSchedule(Model model, Principal principal) {
        User artisan = userService.findByEmail(principal.getName());

        List<ArtisanAppointmentDTO> appointments = artisanAppointmentService.findAllAppointments();
        model.addAttribute("appointments", appointments);
        model.addAttribute("appointmentSetting", artisanAppointmentService.getAppointmentSetting());

        model.addAttribute(
                "notifications",
                notificationService.getAllNotifications(artisan.getUsername())
        );

        model.addAttribute(
                "unreadCount",
                notificationService.countUnreadNotification(artisan.getUsername())
        );

        return "artisan/manage-schedule";
    }

    @PostMapping("artisan/appointments/settings")
    public String updateAppointmentSetting(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pauseFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pauseTo,
            @RequestParam(required = false) String pauseReason,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User artisan = userService.findByEmail(principal.getName());

        try {
            artisanAppointmentService.updateAppointmentSetting(pauseFrom, pauseTo,  pauseReason, artisan);
            redirectAttributes.addFlashAttribute("success", "Cấu hình lịch bận cho auto lịch hẹn đã được cập nhật.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/artisan/appointments";
    }
}
