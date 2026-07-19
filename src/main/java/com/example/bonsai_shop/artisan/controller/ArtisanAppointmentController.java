package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.notification.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import com.example.bonsai_shop.artisan.service.ArtisanAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
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

        List<ArtisanAppointmentDTO> appointments = artisanAppointmentService.findAllAppointmentsByArtisan(artisan);
        model.addAttribute("appointments", appointments);

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
    @PostMapping("artisan/appointments/update/{appointmentId}/status")
    public String updateAppointmentStatus(
            @PathVariable Integer appointmentId
            , @RequestParam String status
            , @RequestParam(required = false) String message
            , Authentication authentication
            , RedirectAttributes redirectAttributes
    ) {

        String email = authentication.getName();
        User artisan = userService.findByEmail(email);
        try {
            artisanAppointmentService.updateAppointmentStatus(appointmentId,status,message,artisan);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Appointment status updated successfully."
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    e.getMessage());
        }
        return "redirect:/artisan/appointments";
    }

    @PostMapping("artisan/appointments/check/{appointmentId}")
    public String checkAppointmentStatus(
             @PathVariable Integer appointmentId
            ,Authentication authentication
            ,RedirectAttributes redirectAttributes
    ){
           String email = authentication.getName();
           User artisan = userService.findByEmail(email);

           try{
               artisanAppointmentService.checkAppointment(
                       appointmentId,
                       artisan
               );

               redirectAttributes.addFlashAttribute(
                       "success",
                       "Appointment completed successfully."
               );


           }catch(Exception e){
               redirectAttributes.addFlashAttribute("error",e.getMessage());
           }
           return "redirect:/artisan/appointments";
    }

}
