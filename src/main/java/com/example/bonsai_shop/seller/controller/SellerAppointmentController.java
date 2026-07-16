package com.example.bonsai_shop.seller.controller;


import com.example.bonsai_shop.customer.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.seller.dto.SellerAppointmentDTO;
import com.example.bonsai_shop.seller.service.SellerAppointmentService;
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
public class SellerAppointmentController {

       private final SellerAppointmentService sellerAppointmentService;
       private final UserService userService;


       @GetMapping("seller/schedule")
    public String viewSchedule(Model model, Principal principal) {
           User seller = userService.findByEmail(principal.getName());

           List<SellerAppointmentDTO> appointments = sellerAppointmentService.findAllAppointmentsBySeller(seller);
           model.addAttribute("appointments", appointments);

           return "seller/manage-schedule";
       }
      @PostMapping("seller/schedule/update/{appointmentId}/status")
      public String updateAppointmentStatus(
               @PathVariable Integer appointmentId
              , @RequestParam String status
              , Authentication authentication
              , RedirectAttributes redirectAttributes
           ) {

           String email = authentication.getName();
          User seller = userService.findByEmail(email);
           try {
               sellerAppointmentService.updateAppointmentStatus(appointmentId,status,seller);
               redirectAttributes.addFlashAttribute(
                       "success",
                       "Appointment status updated successfully."
               );
           } catch (Exception e) {
               redirectAttributes.addFlashAttribute("error",
                       e.getMessage());
           }
           return "redirect:/seller/schedule";
      }
}
