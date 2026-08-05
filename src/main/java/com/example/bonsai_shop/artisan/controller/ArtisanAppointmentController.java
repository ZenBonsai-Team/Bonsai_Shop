package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import com.example.bonsai_shop.artisan.service.ArtisanAppointmentService;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/artisan/appointments")
public class ArtisanAppointmentController {


     private final ArtisanAppointmentService artisanAppointmentService;
    private final UserService userService;

    @GetMapping({"", "/"})
    public String showAppointments(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            Model model) {

        if (date == null) {
            date = LocalDate.now();
        }

        model.addAttribute(
                "appointments",
                artisanAppointmentService.findAllByAppointmentDateBetween(date)
        );

        model.addAttribute("selectedDate", date);

        return "artisan/manage-schedule";
    }

    @ResponseBody
    @GetMapping("/data")
    public List<ArtisanAppointmentDTO> getAppointmentsByDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        if (date == null) {
            date = LocalDate.now();
        }

        return artisanAppointmentService.findAllByAppointmentDateBetween(date);
    }

    @PostMapping("/update")
    public String updateAppointment(
            @RequestParam Integer id,
            @RequestParam String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            RedirectAttributes redirectAttributes) {
        try {
            artisanAppointmentService.handUpdateStatus(id, status);
            redirectAttributes.addFlashAttribute("success",
                    "Cập nhật trạng thái thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return redirectToSelectedDate(date);
    }

    @PostMapping("/complete")
    public String markComplete(
            @RequestParam Integer id,
            @RequestParam String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            RedirectAttributes redirectAttributes) {
        try {
            artisanAppointmentService.handMarkComplete(id, status);
            redirectAttributes.addFlashAttribute("success",
                    "Cập nhật trạng thái thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return redirectToSelectedDate(date);
    }

    private String redirectToSelectedDate(LocalDate date) {
        if (date == null) {
            return "redirect:/artisan/appointments";
        }

        return "redirect:/artisan/appointments?date=" + date;
    }
    @PostMapping("/settings")
    public String updateSetting(

            @RequestParam Boolean autoApprove,

            @RequestParam(required = false)
             Integer autoApproveAfter,

            @RequestParam Boolean autoComplete,

            @RequestParam(required = false)
            Integer autoCompleteAfter,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime pauseFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime pauseTo,

            @RequestParam(required = false)
            String pauseReason,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {
            User user = userService.findByEmail(authentication.getName());
            artisanAppointmentService.updateSetting(autoApprove,autoApproveAfter,autoComplete,autoCompleteAfter,pauseFrom,pauseTo,pauseReason,user);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Cập nhật cấu hình thành công."
            );
        } catch (Exception e) {
          redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/artisan/appointments";
    }

}
