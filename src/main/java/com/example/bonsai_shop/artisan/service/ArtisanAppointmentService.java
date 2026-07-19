package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.notification.service.NotificationService;
import com.example.bonsai_shop.viewappointment.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;

import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class ArtisanAppointmentService {

    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final NotificationService notificationService;

    public List<ArtisanAppointmentDTO> findAllAppointmentsByArtisan(User artisan) {
        return viewingAppointmentRepository.findAllAppointmentsByArtisan(artisan);
    }

    public void updateAppointmentStatus(Integer appointmentId,String status,String message ,User artisan) {
        ViewingAppointment appointment =
                viewingAppointmentRepository.findByAppointmentIdAndArtisan(appointmentId,artisan)
                        .orElseThrow(() ->
                                new RuntimeException("Không có lịch "));
        if (!"PENDING".equalsIgnoreCase(appointment.getStatus())) {
            throw new RuntimeException("Lịch hẹn đã được xử lý.");
        }

        if (!status.equalsIgnoreCase("APPROVED")
                && !status.equalsIgnoreCase("REJECTED")) {
            throw new RuntimeException("Trạng thái không hợp lệ.");
        }

        appointment.setStatus(status);
        viewingAppointmentRepository.save(appointment);

        if ("APPROVED".equalsIgnoreCase(status)) {
            notificationService.createNotification(
                    appointment.getCustomer(),
                    "Lịch hẹn xem cây của bạn đã được chấp nhận."
            );
        } else {
            notificationService.createNotification(
                    appointment.getCustomer(),
                    "Lịch hẹn xem cây của bạn đã bị từ chối.\nLý do: " + message
            );
        }

    }



}
