package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.customer.dto.AppointmentDetailDTO;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.notification.service.NotificationService;
import com.example.bonsai_shop.viewappointment.repository.ViewingAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViewingAppointmentService {

    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public void createViewingAppointment(ViewingAppointment viewingAppointment) {
        userService.checkProfileEmailAndPhone(viewingAppointment.getCustomer());

        viewingAppointmentRepository.save(viewingAppointment);

        notificationService.createNotification(
                viewingAppointment.getCustomer(),
                "Lịch thăm vườn của bạn đã được tạo thành công vào lúc " + viewingAppointment.getAppointmentDate()
        );
    }

    public List<ViewingAppointment> findByCustomer(User customer) {
        return viewingAppointmentRepository.findByCustomer(customer);
    }

    public AppointmentDetailDTO findByIdAndCustomer(Integer appointmentId, User customer) {
        ViewingAppointment appointment = viewingAppointmentRepository.findByAppointmentIdAndCustomer(appointmentId, customer)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn!"));
        return new AppointmentDetailDTO(
                appointment.getAppointmentId(),
                appointment.getAppointmentDate(),
                appointment.getStatus(),
                appointment.getNote()
        );
    }

    @Transactional
    public void updateViewingAppointment(Integer id, User customer, LocalDateTime appointmentDate, String note) {
        ViewingAppointment appointment = viewingAppointmentRepository
                .findByAppointmentIdAndCustomer(id, customer)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if (!appointment.getStatus().equals("PENDING")) {
            throw new RuntimeException("Không thể chỉnh sửa lịch sau khi đã xác nhận");
        }

        appointment.setAppointmentDate(appointmentDate);
        appointment.setNote(note);
        appointment.setUpdatedAt(LocalDateTime.now());

        viewingAppointmentRepository.save(appointment);

        notificationService.createNotification(
                customer,
                "Lịch thăm vườn của bạn đã được thay đổi thành công vào lúc "
                        + appointment.getAppointmentDate()
                        + " Ngày thay đổi: "
                        + appointment.getUpdatedAt()
        );
    }

    @Transactional
    public void cancelViewAppointment(Integer id, User user) {
        ViewingAppointment appointment = viewingAppointmentRepository
                .findByAppointmentIdAndCustomer(id, user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new RuntimeException("Lịch hẹn đã được hủy.");
        }

        if (!appointment.getStatus().equals("PENDING")) {
            throw new RuntimeException("Không thể hủy lịch sau khi đã xác nhận");
        }

        appointment.setStatus("CANCELLED");
        appointment.setUpdatedAt(LocalDateTime.now());
        viewingAppointmentRepository.save(appointment);

        notificationService.createNotification(
                user,
                "Lịch thăm vườn của bạn đã hủy thành công vào lúc " + appointment.getUpdatedAt()
        );
    }
}
