package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.appointmentSetting.service.AppointmentSettingService;
import com.example.bonsai_shop.customer.dto.AppointmentDetailDTO;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.notification.service.NotificationService;
import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViewingAppointmentService {

    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AppointmentSettingService appointmentSettingService;

    @Transactional
    public void createViewingAppointment(ViewingAppointment viewingAppointment) {
        userService.checkProfileEmailAndPhone(viewingAppointment.getCustomer());

        if(appointmentSettingService.isPausedAt(viewingAppointment.getAppointmentDate())){
            AppointmentSetting setting = appointmentSettingService.getAppointmentSetting();

            throw new RuntimeException("Nhà vườn đang tạm ngừng nhận lịch trong khoảng thời gian này.\n"
                                       + "Lý do : " + setting.getPauseReason());
        }

        LocalTime appointmentTime = viewingAppointment.getAppointmentDate().toLocalTime();

        if (viewingAppointment.getAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Không thể đặt lịch trong thời gian đã qua.");
        }

        if (appointmentTime.isBefore(LocalTime.of(8, 0))
                || appointmentTime.isAfter(LocalTime.of(17, 0))) {
            throw new RuntimeException("Chỉ được đặt lịch trong khung giờ từ 08:00 đến 17:00.");
        }

        if(viewingAppointmentRepository.existsByCustomerAndStatusIn(viewingAppointment.getCustomer(),List.of("PENDING","APPROVED"))){
            throw new RuntimeException(
                    "Bạn đã có một lịch hẹn đang diễn ra. Vui lòng hoàn thành lịch hẹn hiện tại trước khi đặt lịch mới."
            );
        }

        String note = viewingAppointment.getNote();
        if(note.length() > 500){
            throw new RuntimeException("Lời nhắn không được vượt quá 500 ký tự.");
        }

        viewingAppointmentRepository.save(viewingAppointment);
    }

    public List<ViewingAppointment> findByCustomer(User customer) {
        return viewingAppointmentRepository.findByCustomerOrderByCreatedAtDesc(customer);
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

        if (appointmentDate.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Không thể đặt lịch trong thời gian đã qua.");
        }

        LocalTime time = appointmentDate.toLocalTime();

        if (time.isBefore(LocalTime.of(8, 0))
                || time.isAfter(LocalTime.of(17, 0))) {
            throw new RuntimeException(
                    "Chỉ được cập nhật lịch trong giờ hành chính (08:00 - 17:00)"
            );
        }

        if(note.length() > 500){
            throw new RuntimeException("Lời nhắn không được vượt quá 500 ký tự.");
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
    }
}
