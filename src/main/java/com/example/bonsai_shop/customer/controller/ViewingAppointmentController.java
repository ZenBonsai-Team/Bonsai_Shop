package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.dto.AppointmentDetailDTO;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.customer.service.ViewingAppointmentService;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewingAppointmentController {

    private final UserService userService;
    private final ViewingAppointmentService viewingAppointmentService;

    @PostMapping("/appointments/create")
    public String createAppointment(
            @RequestParam LocalDate appointmentDate,
            @RequestParam String appointmentTime,
            @RequestParam(required = false) String note,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(principal.getName());

        if (user != null && user.getRole() != null) {
            String roleName = user.getRole().getRoleName();
            if ("ROLE_OWNER".equals(roleName) || "ROLE_ARTISAN".equals(roleName)
                    || "ROLE_MODERATOR".equals(roleName) || "ROLE_CONTENT_MODERATOR".equals(roleName)
                    ) {
                redirectAttributes.addFlashAttribute("error", "Tài khoản quản trị / nhà vườn / kiểm duyệt viên không được phép đặt lịch thăm vườn!");
                return "redirect:/home";
            }
        }

        LocalDateTime finalAppointmentDate = LocalDateTime.of(
                appointmentDate,
                LocalTime.parse(appointmentTime)
        );

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setCustomer(user);
        appointment.setAppointmentDate(finalAppointmentDate);
        appointment.setNote(note);
        appointment.setStatus("PENDING");

        try {
            viewingAppointmentService.createViewingAppointment(appointment);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đặt lịch thăm vườn thành công! Chúng tôi sẽ sớm liên hệ với bạn."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());

        }

        return "redirect:/home";
    }
    
    @GetMapping("/appointments/list")
    @ResponseBody
    public List<AppointmentDetailDTO> myAppointmentList(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        return viewingAppointmentService.findByCustomer(user)
                .stream()
                .map(appointment -> new AppointmentDetailDTO(
                        appointment.getAppointmentId(),
                        appointment.getAppointmentDate(),
                        appointment.getStatus(),
                        appointment.getNote()
                ))
                .toList();
    }

    @GetMapping("/appointments/detail/{id}")
    @ResponseBody
    public AppointmentDetailDTO viewingAppointmentDetail(@PathVariable Integer id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        return viewingAppointmentService.findByIdAndCustomer(id, user);
    }

    @PostMapping("/appointments/update/{id}")
    public String updateAppointment(@PathVariable Integer id,
                                    @RequestParam LocalDate appointmentDate,
                                    @RequestParam String appointmentTime,
                                    @RequestParam(required = false) String note,
                                    RedirectAttributes redirectAttributes,
                                    Principal principal) {
        User user = userService.findByEmail(principal.getName());

        LocalDateTime finalAppointmentDate = LocalDateTime.of(
                appointmentDate,
                LocalTime.parse(appointmentTime)
        );
        try {
            viewingAppointmentService.updateViewingAppointment(
                    id,
                    user,
                    finalAppointmentDate,
                    note
            );
            redirectAttributes.addFlashAttribute("success", "Cập nhật lịch hẹn thành công.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home";
    }

    @PostMapping("/appointments/cancel/{id}")
    public String cancelAppointment(@PathVariable Integer id,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(principal.getName());
        try {
            viewingAppointmentService.cancelViewAppointment(id, user);
            redirectAttributes.addFlashAttribute("success", "Đã hủy lịch hẹn thành công.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home";
    }
}
