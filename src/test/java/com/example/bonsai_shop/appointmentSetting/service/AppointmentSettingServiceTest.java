package com.example.bonsai_shop.appointmentSetting.service;

import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.entity.AppointmentSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentSettingServiceTest {
    @Mock
    private AppointmentSettingRepository appointmentSettingRepository;

    @InjectMocks
    private AppointmentSettingService appointmentSettingService;

    private AppointmentSetting setting;

    @BeforeEach
    void setUp() {
        setting = new AppointmentSetting();

        setting.setSettingId(1);
        setting.setPauseFrom(LocalDateTime.of(2026, 8, 10, 8, 0));
        setting.setPauseTo(LocalDateTime.of(2026, 8, 10, 17, 0));
        setting.setPauseReason("Bảo trì hệ thống");
    }


    @Test
    void getAppointmentSetting_WhenExists_ShouldReturnSetting() {

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        AppointmentSetting result = appointmentSettingService.getAppointmentSetting();

        assertEquals(setting, result);

        verify(appointmentSettingRepository)
                .findFirstByOrderBySettingIdAsc();

        verify(appointmentSettingRepository, never())
                .save(any(AppointmentSetting.class));
    }

    @Test
    void getAppointmentSetting_WhenNotExists_ShouldCreateNewSetting() {

        AppointmentSetting newSetting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.empty());

        when(appointmentSettingRepository.save(any(AppointmentSetting.class)))
                .thenReturn(newSetting);

        AppointmentSetting result = appointmentSettingService.getAppointmentSetting();

        assertEquals(newSetting, result);

        verify(appointmentSettingRepository)
                .findFirstByOrderBySettingIdAsc();

        verify(appointmentSettingRepository)
                .save(any(AppointmentSetting.class));
    }

    @Test
    void isPausedAt_WhenAppointmentDateIsNull_ShouldReturnFalse() {

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        boolean result = appointmentSettingService.isPausedAt(null);

        assertFalse(result);
    }

    @Test
    void isPausedAt_WhenPauseTimeIsNull_ShouldReturnFalse() {

        setting.setPauseFrom(null);
        setting.setPauseTo(null);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        boolean result = appointmentSettingService.isPausedAt(
                LocalDateTime.of(2026, 8, 10, 10, 0)
        );

        assertFalse(result);
    }

    @Test
    void isPausedAt_WhenAppointmentWithinPausePeriod_ShouldReturnTrue() {

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        boolean result = appointmentSettingService.isPausedAt(
                LocalDateTime.of(2026, 8, 10, 10, 0)
        );

        assertTrue(result);
    }

    @Test
    void isPausedAt_WhenAppointmentOutsidePausePeriod_ShouldReturnFalse() {

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        boolean result = appointmentSettingService.isPausedAt(
                LocalDateTime.of(2026, 8, 10, 18, 0)
        );

        assertFalse(result);
    }
}
