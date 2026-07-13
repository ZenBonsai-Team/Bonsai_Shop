package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bonsai_shop.customer.dto.AppoimentDetailDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ViewingAppointmentService {

    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void createViewingAppointment(ViewingAppointment viewingAppointment) {
        if (viewingAppointmentRepository
                .existsViewingAppointmentByAppointmentDate(viewingAppointment.getAppointmentDate())){
            throw new RuntimeException("Khung giờ này đã có lịch hẹn!");
        }
        viewingAppointmentRepository.save(viewingAppointment);

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

}
