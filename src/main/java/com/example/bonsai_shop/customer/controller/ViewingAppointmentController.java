package com.example.bonsai_shop.customer.controller;


import com.example.bonsai_shop.customer.dto.AppoimentDetailDTO;
import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.customer.service.ViewingAppointmentService;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.product.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
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
        // Lấy khách hàng
        User user = userService.findByEmail(principal.getName());

        // Lấy sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        // Tạo ngày giờ hẹn
        LocalDateTime finalAppointmentDate = LocalDateTime.of(
                appointmentDate,
                LocalTime.parse(appointmentTime)
        );

        // Tạo lịch hẹn
        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setCustomer(user);
        appointment.setProduct(product);
        appointment.setAppointmentDate(finalAppointmentDate);
        appointment.setNote(note);
        appointment.setStatus("PENDING");

        try {

            viewingAppointmentService.createViewingAppointment(appointment);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "🎉 Đặt lịch xem thành công! Chúng tôi sẽ sớm liên hệ với bạn."
            );

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            // Lưu lại dữ liệu nếu muốn hiển thị lại form
            redirectAttributes.addFlashAttribute("productId", productId);
            redirectAttributes.addFlashAttribute("productTitle", product.getProductName());
            redirectAttributes.addFlashAttribute("appointmentDate", appointmentDate);
            redirectAttributes.addFlashAttribute("appointmentTime", appointmentTime);
            redirectAttributes.addFlashAttribute("note", note);
        }

        return "redirect:/bonsai_luxury_detail/" + productId;
    }

    @GetMapping("/appointments")
    public String myAppointment(Model model,
                                Principal principal){
    User user = userService.findByEmail(principal.getName());
    List <ViewingAppointment> viewingAppointments = viewingAppointmentService.findByCustomer(user);
    model.addAttribute("viewingAppointments", viewingAppointments);
    return "customer/view-appointment";
    }

    @GetMapping("/appointments/detail/{id}")
    @ResponseBody
    public AppoimentDetailDTO viewingAppointmentdetail(@PathVariable Integer id,
                                                       Principal principal){
        User user = userService.findByEmail(principal.getName());
       return  viewingAppointmentService.findByIdAndCustomer(id, user);

    }

    @PostMapping("/appointments/update/{id}")
        public String updateAppointment(@PathVariable Integer id,
                                        @RequestParam LocalDate appointmentDate,
                                        @RequestParam String appointmentTime,
                                        @RequestParam(required = false) String note,
                                        Principal principal){

        User user = userService.findByEmail(principal.getName());

        LocalDateTime finalAppointmentDate = LocalDateTime.of(
                appointmentDate,
                LocalTime.parse(appointmentTime)
        );
        viewingAppointmentService.updateViewingAppointment(
                id,
                user,
                finalAppointmentDate,
                note
        );

        return "redirect:/appointments";
    }

}
