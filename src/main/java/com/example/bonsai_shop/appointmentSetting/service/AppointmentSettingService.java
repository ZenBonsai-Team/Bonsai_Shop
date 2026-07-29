package com.example.bonsai_shop.appointmentSetting.service;

import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.entity.AppointmentSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentSettingService {

        private final AppointmentSettingRepository appointmentSettingRepository;

        public AppointmentSetting getAppointmentSetting() {
            return appointmentSettingRepository.findFirstByOrderBySettingIdAsc()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình lịch hẹn."));
        }

        public boolean isPausedAt(LocalDateTime appointmentDate) {
            AppointmentSetting setting = getAppointmentSetting();

            if (appointmentDate == null) {
                return false;
            }

            if (setting.getPauseFrom() == null || setting.getPauseTo() == null) {
                return false;
            }

            return !appointmentDate.isBefore(setting.getPauseFrom())
                    && !appointmentDate.isAfter(setting.getPauseTo());
        }
    }

