package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.notification.service.NotificationService;
import com.example.bonsai_shop.viewappointment.repository.ViewingAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtisanAppointmentService {

    // Demo: 5 phút (Production: 60 phút)
    private static final int AUTO_DECIDE_PENDING_MINUTES = 5;
    private static final int AUTO_COMPLETE_MINUTES = 5;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final LocalTime BUSINESS_START_TIME = LocalTime.of(8, 0);
    private static final LocalTime BUSINESS_END_TIME = LocalTime.of(17, 0);

    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final NotificationService notificationService;
    private final AppointmentSettingRepository appointmentSettingRepository;

    public List<ArtisanAppointmentDTO> findAllAppointments() {
        return viewingAppointmentRepository.findAllAppointmentSummaries();
    }

    @Transactional
    public int processAutomaticAppointmentStatusUpdates() {
        LocalDateTime now = LocalDateTime.now();

        int updatedCount = autoDecidePendingAppointments(now);
        updatedCount += autoCompleteApprovedAppointments(now);

        return updatedCount;
    }

    public AppointmentSetting getAppointmentSetting() {
       return appointmentSettingRepository.findFirstByOrderBySettingIdAsc()
               .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình lịch hẹn."));
    }

    public void updateAppointmentSetting(
            LocalDateTime pauseFrom,
            LocalDateTime pauseTo,
            String pauseReason,
            User updatedBy
    ) {
        AppointmentSetting setting = getAppointmentSetting();

        if (pauseFrom != null && pauseTo != null && pauseFrom.isAfter(pauseTo)) {
            throw new RuntimeException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }

        validatePauseDateTime(pauseFrom);
        validatePauseDateTime(pauseTo);

        String normalizedPauseReason = normalizePauseReason(pauseReason);
        if (pauseFrom != null && pauseTo != null && normalizedPauseReason == null) {
            throw new RuntimeException("Vui lòng nhập lý do tạm ngừng.");
        }

        if (pauseFrom != null && pauseTo != null) {
            if (pauseFrom.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Thời gian tạm nghỉ phải sau thời điểm hiện tại.");
            }

            if (pauseFrom.isAfter(pauseTo)) {
                throw new RuntimeException("Thời gian bắt đầu phải trước thời gian kết thúc.");
            }
        }


        setting.setPauseFrom(pauseFrom);
        setting.setPauseTo(pauseTo);
        setting.setPauseReason(normalizedPauseReason);
        setting.setAutoComplete(true);
        setting.setUpdatedBy(updatedBy);
        setting.setUpdatedAt(LocalDateTime.now());

        appointmentSettingRepository.save(setting);
    }

    private int autoDecidePendingAppointments(LocalDateTime now) {
//        LocalDateTime start  = now.toLocalDate().atStartOfDay();
//        LocalDateTime end = start.plusDays(1);
//        List<ViewingAppointment> appointments = viewingAppointmentRepository
//                 .findByStatusAndAppointmentDateGreaterThanEqualAndAppointmentDateLessThan(STATUS_PENDING, start, end);


        LocalDateTime deadline = now.minusMinutes(AUTO_DECIDE_PENDING_MINUTES);
        List<ViewingAppointment> appointments = viewingAppointmentRepository.findByStatusAndCreatedAtLessThanEqual(STATUS_PENDING, deadline);

        int updatedCount = 0;
        for (ViewingAppointment appointment : appointments) {

                applyAutomaticStatus(appointment,STATUS_APPROVED);
            updatedCount++;
        }

        return updatedCount;
    }

    private int autoCompleteApprovedAppointments(LocalDateTime now) {
        LocalDateTime completeTime = now.minusMinutes(AUTO_COMPLETE_MINUTES);

        List<ViewingAppointment> appointments = viewingAppointmentRepository
                .findByStatusAndAppointmentDateLessThanEqual(STATUS_APPROVED, completeTime);

        int updatedCount = 0;
        for (ViewingAppointment appointment : appointments) {
            applyAutomaticStatus(appointment, STATUS_COMPLETED);
            updatedCount++;
        }

        return updatedCount;
    }

    private void applyAutomaticStatus(ViewingAppointment appointment, String nextStatus) {
       appointment.setStatus(nextStatus);
       appointment.setUpdatedAt(LocalDateTime.now());
       viewingAppointmentRepository.save(appointment);
        if (STATUS_APPROVED.equals(nextStatus)) {
            notificationService.createNotification(
                    appointment.getCustomer(),
                    "Lịch hẹn tham quan vườn của bạn đã được chấp nhận."
            );
        }
    }

    private String normalizePauseReason(String pauseReason) {
        if (pauseReason == null || pauseReason.isBlank()) {
            return null;
        }
        return pauseReason.trim();
    }

    private void validatePauseDateTime(LocalDateTime pauseDateTime) {
        if (pauseDateTime == null) {
            return;
        }
        if (pauseDateTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thoi gian ban khong duoc truoc thoi diem hien tai.");
        }

        LocalTime pauseTime = pauseDateTime.toLocalTime();
        if (pauseTime.isBefore(BUSINESS_START_TIME) || pauseTime.isAfter(BUSINESS_END_TIME)) {
            throw new RuntimeException("Thoi gian ban phai nam trong gio hanh chinh 08:00 - 17:00.");
        }
    }
}
