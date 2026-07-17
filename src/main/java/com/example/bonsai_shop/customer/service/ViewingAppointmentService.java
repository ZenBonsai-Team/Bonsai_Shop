package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bonsai_shop.customer.dto.AppoimentDetailDTO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViewingAppointmentService {

    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final ProductRepository productRepository;
    private final UserService  userService ;

    @Transactional
    public void createViewingAppointment(ViewingAppointment viewingAppointment) {

        if (viewingAppointmentRepository.existsActiveAppointment(
                viewingAppointment.getAppointmentDate())){
            throw new RuntimeException("Khung giờ này đã có lịch hẹn!");
        }
        userService.checkProfileEmailAndPhone(viewingAppointment.getCustomer());

        Product product = viewingAppointment.getProduct();
        if ("RESERVED".equals(product.getProductStatus())) {
            throw new RuntimeException("Sản phẩm đã có lịch hẹn!");
        }

        if ("SOLD".equals(product.getProductStatus())) {
            throw new RuntimeException("Sản phẩm đã được bán!");
        }

        viewingAppointmentRepository.save(viewingAppointment);

        product.setProductStatus("RESERVED");
        productRepository.save(product);
    }

    public List<ViewingAppointment> findByCustomer(User customer){
        return viewingAppointmentRepository.findByCustomer(customer);
    }

    public AppoimentDetailDTO findByIdAndCustomer(Integer appointmentId, User customer){
           ViewingAppointment appointment = viewingAppointmentRepository.findByAppointmentIdAndCustomer(appointmentId,customer)
                   .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn!"));
           return new AppoimentDetailDTO(
                   appointment.getAppointmentId(),
                   appointment.getProduct().getProductName(),
                   appointment.getProduct().getProductCode(),
                   appointment.getAppointmentDate(),
                   appointment.getStatus(),
                   appointment.getNote()
           );
    }

    public void updateViewingAppointment(Integer id, User customer, LocalDateTime appointmentDate, String note) {
        ViewingAppointment appointment = viewingAppointmentRepository
                .findByAppointmentIdAndCustomer(id,customer)
                .orElseThrow(()->new RuntimeException("Không tìm thấy lịch hẹn"));

        if (viewingAppointmentRepository.existsViewingAppointmentByAppointmentDateAndAppointmentIdNot(appointmentDate, id)) {
            throw new RuntimeException("Khung giờ này đã được đặt");
        }
        if(!appointment.getStatus().equals("PENDING")){
            throw new RuntimeException("Không thể chỉnh sửa lịch sau khi đã xác nhận");
        }

        appointment.setAppointmentDate(appointmentDate);
        appointment.setNote(note);
        appointment.setUpdatedAt(LocalDateTime.now());

        viewingAppointmentRepository.save(appointment);
    }

    @Transactional
    public void cancelViewAppointment(Integer id, User user){
        ViewingAppointment appointment = viewingAppointmentRepository
                .findByAppointmentIdAndCustomer(id, user)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new RuntimeException("Lịch hẹn đã được hủy.");
        }

        if(!appointment.getStatus().equals("PENDING")){
            throw new RuntimeException("Không thể hủy lịch sau khi đã xác nhận");
        }

        appointment.setStatus("CANCELLED");
        Product product = appointment.getProduct();
        product.setProductStatus("AVAILABLE");
        productRepository.save(product);
        viewingAppointmentRepository.save(appointment);
    }
}
