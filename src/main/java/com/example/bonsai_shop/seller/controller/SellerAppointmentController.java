package com.example.bonsai_shop.seller.controller;


import org.springframework.ui.Model;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.seller.dto.SellerAppointmentDTO;
import com.example.bonsai_shop.seller.service.SellerAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

}
