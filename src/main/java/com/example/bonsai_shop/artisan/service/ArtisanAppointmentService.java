package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.notification.service.NotificationService;
import com.example.bonsai_shop.viewappointment.repository.ViewingAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtisanAppointmentService {

    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final NotificationService notificationService;

    public List<ArtisanAppointmentDTO> findAllAppointmentsByArtisan(User artisan) {
        return viewingAppointmentRepository.findAllAppointmentsByArtisan();
    }

    public void updateAppointmentStatus(Integer appointmentId, String status, String message, User artisan) {
        ViewingAppointment appointment =
                viewingAppointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new RuntimeException("Không có lịch."));

        String currentStatus = appointment.getStatus();
        String nextStatus = status == null ? "" : status.trim().toUpperCase();


        if ("PENDING".equalsIgnoreCase(currentStatus)) {
            validatePendingTransition(nextStatus, message);
        } else if ("APPROVED".equalsIgnoreCase(currentStatus)) {
            validateApprovedTransition(nextStatus, appointment.getAppointmentDate());
        } else {
            throw new RuntimeException("Lịch hẹn đã được xử lý.");
        }

        appointment.setStatus(nextStatus);
        appointment.setUpdatedAt(LocalDateTime.now());
        viewingAppointmentRepository.save(appointment);
        notifyCustomerIfNeeded(appointment, nextStatus, message);
    }

    public void checkAppointment(Integer appointmentId, User artisan) {
        ViewingAppointment appointment =
                viewingAppointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new RuntimeException("Không có lịch."));

        if (!"APPROVED".equalsIgnoreCase(appointment.getStatus())) {
            throw new RuntimeException("Không thể hoàn thành lịch khi đang ở trạng thái " + appointment.getStatus());
        }
        LocalDateTime appointmentTime =appointment.getAppointmentDate();
        if (LocalDateTime.now().isBefore(appointmentTime)) {
            throw new RuntimeException(
                    "Chưa đến thời gian diễn ra lịch hẹn nên không thể hoàn thành."
            );
        }
        appointment.setStatus("COMPLETED");
        appointment.setUpdatedAt(LocalDateTime.now());
        viewingAppointmentRepository.save(appointment);
    }

    public void markAppointmentOverdue(Integer appointmentId, User artisan) {
        ViewingAppointment appointment =
                viewingAppointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new RuntimeException("Không có lịch."));

        if (!"PENDING".equalsIgnoreCase(appointment.getStatus())) {
            throw new RuntimeException("Chỉ lịch PENDING mới có thể chuyển quá hạn.");
        }

        LocalDateTime deadline = appointment.getAppointmentDate().toLocalDate().atStartOfDay();
        if (LocalDateTime.now().isBefore(deadline)) {
            throw new RuntimeException("Lịch này chưa tới hạn quá hạn.");
        }

        appointment.setStatus("OVERDUE");
        appointment.setUpdatedAt(LocalDateTime.now());
        viewingAppointmentRepository.save(appointment);
    }

    private void validatePendingTransition(String nextStatus, String message) {
        if (!"APPROVED".equals(nextStatus) && !"REJECTED".equals(nextStatus)) {
            throw new RuntimeException("Trạng thái không hợp lệ.");
        }

        if ("REJECTED".equals(nextStatus) && (message == null || message.trim().length() < 5)) {
            throw new RuntimeException("Vui lòng nhập lý do từ chối tối thiểu 5 ký tự.");
        }
    }

    private void validateApprovedTransition(String nextStatus, LocalDateTime appointmentTime) {
        if (!"COMPLETED".equals(nextStatus)) {
            throw new RuntimeException("Lịch đã duyệt chỉ có thể chuyển sang COMPLETED.");
        }
        if (LocalDateTime.now().isBefore(appointmentTime)) {
            throw new RuntimeException(
                    "Chưa đến thời gian hẹn. Không thể hoàn thành lịch hẹn."
            );
        }
    }

    private void notifyCustomerIfNeeded(ViewingAppointment appointment, String nextStatus, String message) {
        if ("APPROVED".equals(nextStatus)) {
            notificationService.createNotification(
                    appointment.getCustomer(),
                    "Lịch hẹn xem cây của bạn đã được chấp nhận."
            );
        } else if ("REJECTED".equals(nextStatus)) {
            notificationService.createNotification(
                    appointment.getCustomer(),
                    "Lịch hẹn xem cây của bạn đã bị từ chối.\nLý do: " + message.trim()
            );
        }
    }
}
