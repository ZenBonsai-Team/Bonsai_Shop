package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.viewappointment.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.notification.service.NotificationService;
import com.example.bonsai_shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bonsai_shop.customer.dto.AppointmentDetailDTO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViewingAppointmentService {

    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final ProductRepository productRepository;
    private final UserService  userService ;
    private final NotificationService notificationService;

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

        notificationService.createNotification(viewingAppointment.getCustomer(),
                "Lịch đặt xem cây"+ viewingAppointment.getProduct().getProductName()
                        + " đã được tạo thành công ");
        notificationService.createNotification(viewingAppointment.getProduct().getCreatedBy(),
                "Khách hàng: "+ viewingAppointment.getCustomer().getUsername()
                        + " đã đặt lịch xem cây "
                        +viewingAppointment.getProduct().getProductName()
                        + " Theo lịch vào lúc " + viewingAppointment.getAppointmentDate()
                        + "Ngày tạo: " + viewingAppointment.getCreatedAt()
        );
    }

    public List<ViewingAppointment> findByCustomer(User customer){
        return viewingAppointmentRepository.findByCustomer(customer);
    }

    public AppointmentDetailDTO findByIdAndCustomer(Integer appointmentId, User customer){
           ViewingAppointment appointment = viewingAppointmentRepository.findByAppointmentIdAndCustomer(appointmentId,customer)
                   .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn!"));
           return new AppointmentDetailDTO(
                   appointment.getAppointmentId(),
                   appointment.getProduct().getProductName(),
                   appointment.getProduct().getProductCode(),
                   appointment.getAppointmentDate(),
                   appointment.getStatus(),
                   appointment.getNote()
           );
    }

    @Transactional
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

        notificationService.createNotification(customer,
                "Lịch đặt xem cây "+ appointment.getProduct().getProductName()
                        + " đã được thay đổi thành công "
                        + " Vào lúc " + appointment.getAppointmentDate()
                        + " Ngày thay đổi: " + appointment.getUpdatedAt());
        notificationService.createNotification(appointment.getProduct().getCreatedBy(),
                "Khách hàng: "+ customer.getUsername()
                        + " đã thay đổi lịch xem cây "
                        +appointment.getProduct().getProductName()
                        + " vào lúc " + appointment.getAppointmentDate()
                        + " - Ngày thay đổi: " + appointment.getUpdatedAt());
    }

    @Transactional
    public void cancelViewAppointment(Integer id, User user){
        ViewingAppointment appointment = viewingAppointmentRepository
                .findByAppointmentIdAndCustomer(id, user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new RuntimeException("Lịch hẹn đã được hủy.");
        }

        if(!appointment.getStatus().equals("PENDING")){
            throw new RuntimeException("Không thể hủy lịch sau khi đã xác nhận");
        }

        appointment.setStatus("CANCELLED");
        appointment.setUpdatedAt(LocalDateTime.now());
        Product product = appointment.getProduct();
        viewingAppointmentRepository.save(appointment);

        notificationService.createNotification(user,
                "Lịch đặt xem cây "+ product.getProductName()
                        + " đã hủy thành công " + " vào lúc " + appointment.getUpdatedAt());

        notificationService.createNotification(product.getCreatedBy(),
                "Lịch đặt xem cây " +appointment.getProduct().getProductName()
                        + " của khách hàng " + user.getUsername()
                        + " đã hủy vào lúc " + appointment.getUpdatedAt());
    }
}
