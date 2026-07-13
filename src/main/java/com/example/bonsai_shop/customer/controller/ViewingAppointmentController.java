package com.example.bonsai_shop.customer.controller;


import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.customer.service.ViewingAppointmentService;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.product.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequiredArgsConstructor
public class ViewingAppointmentController {

    private final UserService userService;
    private final ProductRepository productRepository;
    private final ViewingAppointmentService viewingAppointmentService;

    @PostMapping("/appointments/create")
    public String createAppointment(
            @RequestParam Integer productId,
            @RequestParam LocalDate appointmentDate,
            @RequestParam String appointmentTime,
            @RequestParam(required = false) String note,
            Principal principal,
            RedirectAttributes redirectAttributes){
        // Lấy ID khách hàng đang login
        User user = userService.findByEmail(principal.getName());
        System.out.println(user.getUserId());

        //Lấy ID cây
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new  RuntimeException("Product not found"));

        if ("RESERVED".equals(product.getProductStatus())) {
            throw new RuntimeException("Sản phẩm đã có lịch hẹn!");
        }

        if ("SOLD".equals(product.getProductStatus())) {
            throw new RuntimeException("Sản phẩm đã được bán!");
        }

        LocalDateTime finalAppointmentDate =
                LocalDateTime.of(
                        appointmentDate,
                        LocalTime.parse(appointmentTime)
                );

        //Tạo Lịch Hẹn
        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setCustomer(user);
        appointment.setProduct(product);
        appointment.setAppointmentDate(finalAppointmentDate);
        appointment.setNote(note);
        appointment.setStatus("PENDING");

        //lưu appointment
        try{
            viewingAppointmentService.createViewingAppointment(appointment);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đặt lịch thành công!");
        }catch (RuntimeException e){
            redirectAttributes.addFlashAttribute("error",e.getMessage());

            redirectAttributes.addFlashAttribute("productId", productId);
            redirectAttributes.addFlashAttribute("productTitle", product.getProductName());
            redirectAttributes.addFlashAttribute("appointmentDate", appointmentDate);
            redirectAttributes.addFlashAttribute("appointmentTime", appointmentTime);
            redirectAttributes.addFlashAttribute("note", note);
        }

        return "redirect:/bonsai-luxury";
    }
}
