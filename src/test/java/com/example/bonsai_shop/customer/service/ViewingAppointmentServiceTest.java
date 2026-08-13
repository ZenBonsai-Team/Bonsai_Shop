
package com.example.bonsai_shop.customer.service;
import com.example.bonsai_shop.appointmentSetting.service.AppointmentSettingService;
import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import com.example.bonsai_shop.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewingAppointmentServiceTest {

    @Mock
    private ViewingAppointmentRepository viewingAppointmentRepository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AppointmentSettingService appointmentSettingService;

    @InjectMocks
    private ViewingAppointmentService viewingAppointmentService;

    private User customer;
    private ViewingAppointment appointment;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setUserId(1);

        appointment = new ViewingAppointment();
        appointment.setCustomer(customer);
        appointment.setAppointmentDate(
                LocalDateTime.now()
                        .plusDays(1)
                        .withHour(10)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
        );
        appointment.setStatus("PENDING");
        appointment.setNote("Test");

        customer.setPhone(null);
        customer.setEmail("abc@gmail.com");

    }

    @Test
    void createViewingAppointment_WhenValid_ShouldSaveAndNotify() {
        when(appointmentSettingService.isPausedAt(appointment.getAppointmentDate())).thenReturn(false);
        when(viewingAppointmentRepository.existsByCustomerAndStatusIn(customer, List.of("PENDING", "APPROVED")))
                .thenReturn(false);

        viewingAppointmentService.createViewingAppointment(appointment);

        verify(userService).checkProfileEmailAndPhone(customer);
        verify(viewingAppointmentRepository).save(appointment);
        verify(notificationService).createNotification(
                eq(customer),
                argThat(message -> message.contains(appointment.getAppointmentDate().toString()))
        );
    }

    @Test
    void createViewingAppointment_WhenAppointmentTimeOutsideBusinessHours_ShouldThrow() {
        appointment.setAppointmentDate(
                LocalDateTime.of(2026, 8, 10, 18, 0)
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.createViewingAppointment(appointment)
        );

        assertEquals(
                "Chỉ được đặt lịch trong khung giờ từ 08:00 đến 17:00.",
                ex.getMessage()
        );

        verify(viewingAppointmentRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void createViewingAppointment_WhenSchedulePaused_ShouldThrowAndNotSave() {
        AppointmentSetting setting = new AppointmentSetting();
        setting.setPauseReason("Bao tri");

        when(appointmentSettingService.isPausedAt(appointment.getAppointmentDate())).thenReturn(true);
        when(appointmentSettingService.getAppointmentSetting()).thenReturn(setting);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.createViewingAppointment(appointment)
        );

        assertTrue(exception.getMessage().contains("Bao tri"));
        verify(viewingAppointmentRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), anyString());
    }

    @Test
    void createViewingAppointment_WhenCustomerHasActiveAppointment_ShouldThrowAndNotSave() {
        when(appointmentSettingService.isPausedAt(appointment.getAppointmentDate())).thenReturn(false);
        when(viewingAppointmentRepository.existsByCustomerAndStatusIn(customer, List.of("PENDING", "APPROVED")))
                .thenReturn(true);

        assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.createViewingAppointment(appointment)
        );

        verify(viewingAppointmentRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), anyString());
    }

    @Test
    void createViewingAppointment_WhenCustomerNotHasProfile_ShouldThrowAndNotSave() {
        doThrow(new RuntimeException("Vui lòng cập nhật số điện thoại trước khi đặt lịch"))
                .when(userService)
                .checkProfileEmailAndPhone(customer);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.createViewingAppointment(appointment)
        );

        assertEquals(
                "Vui lòng cập nhật số điện thoại trước khi đặt lịch",
                ex.getMessage()
        );
        verify(userService).checkProfileEmailAndPhone(customer);

        verifyNoInteractions(appointmentSettingService);

        verify(viewingAppointmentRepository, never()).save(any());

        verifyNoInteractions(notificationService);
    }

    @Test
    void findByCustomer_ShouldReturnCustomerAppointments() {
        when(viewingAppointmentRepository.findByCustomerOrderByCreatedAtDesc(customer))
                .thenReturn(List.of(appointment));

        List<ViewingAppointment> result = viewingAppointmentService.findByCustomer(customer);

        assertEquals(List.of(appointment), result);
        verify(viewingAppointmentRepository).findByCustomerOrderByCreatedAtDesc(customer);
    }

    @Test
    void findByCustomer_NoAppointment() {
        when (viewingAppointmentRepository.findByCustomerOrderByCreatedAtDesc(customer)).thenReturn(Collections.emptyList());
        List<ViewingAppointment> result = viewingAppointmentService.findByCustomer(customer);

        assertTrue(result.isEmpty());
        verify(viewingAppointmentRepository).findByCustomerOrderByCreatedAtDesc(customer);
    }

    @Test
    void findByIdAndCustomer_WhenFound_ShouldReturnDetailDto() {
        appointment.setAppointmentId(10);
        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.of(appointment));

        var result = viewingAppointmentService.findByIdAndCustomer(10, customer);

        assertEquals(10, result.getAppointmentId());
        assertEquals(appointment.getAppointmentDate(), result.getAppointmentDate());
        assertEquals("PENDING", result.getStatus());
        assertEquals("Test", result.getNote());
    }

    @Test
    void findByIdAndCustomer_WhenNotFound_ShouldThrow() {

        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.findByIdAndCustomer(10, customer)
        );

        assertEquals("Không tìm thấy lịch hẹn!", ex.getMessage());

        verify(viewingAppointmentRepository)
                .findByAppointmentIdAndCustomer(10, customer);
    }

    @Test
    void updateViewingAppointment_WhenPending_ShouldUpdateSaveAndNotify() {

        LocalDateTime newDate =
                LocalDateTime.of(2026, 8, 10, 10, 0);

        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.of(appointment));

        viewingAppointmentService.updateViewingAppointment(
                10,
                customer,
                newDate,
                "Updated note"
        );

        assertEquals(newDate, appointment.getAppointmentDate());
        assertEquals("Updated note", appointment.getNote());
        assertNotNull(appointment.getUpdatedAt());

        verify(viewingAppointmentRepository).save(appointment);

        verify(notificationService).createNotification(
                eq(customer),
                argThat(message -> message.contains(newDate.toString()))
        );
    }

    @Test
    void updateViewingAppointment_WhenNotPending_ShouldThrowAndNotSave() {
        appointment.setStatus("APPROVED");
        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.of(appointment));

        assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.updateViewingAppointment(10, customer, LocalDateTime.now().plusDays(2), "Updated note")
        );

        verify(viewingAppointmentRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), anyString());
    }

    @Test
    void updateViewingAppointment_WhenNotFound_ShouldThrow() {

        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.updateViewingAppointment(
                        10,
                        customer,
                        LocalDateTime.now().plusDays(2),
                        "Updated note"
                )
        );

        assertEquals("Không tìm thấy lịch hẹn", ex.getMessage());

        verify(viewingAppointmentRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void updateViewingAppointment_WhenOutsideBusinessHours_ShouldThrowAndNotSave() {

        LocalDateTime invalidDate =
                LocalDateTime.of(2026, 8, 10, 23, 0);

        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.updateViewingAppointment(
                        10,
                        customer,
                        invalidDate,
                        "Updated note"
                )
        );

        assertEquals(
                "Chỉ được cập nhật lịch trong giờ hành chính (08:00 - 17:00)",
                ex.getMessage()
        );

        verify(viewingAppointmentRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), anyString());
    }

    @Test
    void cancelViewAppointment_WhenPending_ShouldCancelSaveAndNotify() {
        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.of(appointment));

        viewingAppointmentService.cancelViewAppointment(10, customer);

        assertEquals("CANCELLED", appointment.getStatus());
        assertNotNull(appointment.getUpdatedAt());
        verify(viewingAppointmentRepository).save(appointment);
        verify(notificationService).createNotification(
                eq(customer),
                argThat(message -> message.contains(appointment.getUpdatedAt().toString()))
        );
    }

    @Test
    void cancelViewAppointment_WhenAlreadyCancelled_ShouldThrowAndNotSave() {
        appointment.setStatus("CANCELLED");
        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.of(appointment));

        assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.cancelViewAppointment(10, customer)
        );

        verify(viewingAppointmentRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), anyString());
    }

    @Test
    void cancelViewAppointment_WhenNotFound_ShouldThrow() {

        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.cancelViewAppointment(10, customer)
        );

        assertEquals("Không tìm thấy lịch hẹn", ex.getMessage());

        verify(viewingAppointmentRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancelViewAppointment_WhenApproved_ShouldThrowAndNotSave() {

        //appointment.setStatus("APPROVE");
        //appointment.setStatus("COMPLETED");
        //appointment.setStatus("ABSENT");
        appointment.setStatus("REJECT");

        when(viewingAppointmentRepository.findByAppointmentIdAndCustomer(10, customer))
                .thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> viewingAppointmentService.cancelViewAppointment(10, customer)
        );

        assertEquals(
                "Không thể hủy lịch sau khi đã xác nhận",
                ex.getMessage()
        );

        verify(viewingAppointmentRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), anyString());
    }
}