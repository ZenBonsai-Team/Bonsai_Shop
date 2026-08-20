package com.example.bonsai_shop.artisan.service;

import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.artisan.dto.ArtisanAppointmentDTO;
import com.example.bonsai_shop.artisan.repository.ArtisanAppointmentRepository;

import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtisanAppointmentServiceTest {

    @Mock
    private ArtisanAppointmentRepository artisanAppointmentRepository;

    @Mock
    private AppointmentSettingRepository appointmentSettingRepository;

    @InjectMocks
    private ArtisanAppointmentService artisanAppointmentService;


    //Find list Appointment By AppointmentDay
    // Test kiem tra service lay lich trong khoang ngay va map sang DTO dung thong tin.
    @Test
    void findAllByAppointmentDateBetween_WhenAppointmentsExist_ShouldReturnDtoList() {

        LocalDate date = LocalDate.of(2026, 8, 10);

        ViewingAppointment appointment1 = new ViewingAppointment();
        appointment1.setAppointmentId(1);
        appointment1.setAppointmentDate(LocalDateTime.of(2026, 8, 10, 9, 0));
        appointment1.setStatus("PENDING");
        appointment1.setNote("Ngày 10");

        User customer1 = new User();
        customer1.setFullName("Sam");
        customer1.setPhone("0123456789");
        customer1.setEmail("sam@test.com");
        appointment1.setCustomer(customer1);

        when(artisanAppointmentRepository.findByAppointmentDateBetween(
                date.atStartOfDay(),
                date.atStartOfDay().plusDays(1)
        )).thenReturn(List.of(appointment1));

        List<ArtisanAppointmentDTO> result =
                artisanAppointmentService.findAllByAppointmentDateBetween(date);

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getAppointmentId());
        assertEquals("Sam", result.getFirst().getCustomerName());

        verify(artisanAppointmentRepository).findByAppointmentDateBetween(
                date.atStartOfDay(),
                date.atStartOfDay().plusDays(1)
        );
    }

    // Test kiem tra service lay nhieu lich trong khoang ngay thi tra ve day du DTO.
    @Test
    void findAllByAppointmentDateBetween_WhenMultipleAppointments_ShouldReturnAllDtos() {

        LocalDate date = LocalDate.of(2026, 8, 10);

        ViewingAppointment appointment1 = new ViewingAppointment();
        appointment1.setAppointmentId(1);
        appointment1.setAppointmentDate(LocalDateTime.of(2026, 8, 10, 9, 0));
        appointment1.setStatus("PENDING");

        User customer1 = new User();
        customer1.setFullName("Sam");
        appointment1.setCustomer(customer1);

        ViewingAppointment appointment2 = new ViewingAppointment();
        appointment2.setAppointmentId(2);
        appointment2.setAppointmentDate(LocalDateTime.of(2026, 8, 10, 15, 0));
        appointment2.setStatus("APPROVED");

        User customer2 = new User();
        customer2.setFullName("Anya");
        appointment2.setCustomer(customer2);

        when(artisanAppointmentRepository.findByAppointmentDateBetween(
                date.atStartOfDay(),
                date.atStartOfDay().plusDays(1)
        )).thenReturn(List.of(appointment1, appointment2));

        List<ArtisanAppointmentDTO> result =
                artisanAppointmentService.findAllByAppointmentDateBetween(date);

        assertEquals(2, result.size());

        assertEquals("Sam", result.get(0).getCustomerName());
        assertEquals("Anya", result.get(1).getCustomerName());

        verify(artisanAppointmentRepository).findByAppointmentDateBetween(
                date.atStartOfDay(),
                date.atStartOfDay().plusDays(1)
        );
    }

    // Test kiem tra khong co lich trong khoang ngay thi service tra ve danh sach rong.
    @Test
    void findAllByAppointmentDateBetween_WhenNoAppointments_ShouldReturnEmptyList() {

        LocalDate date = LocalDate.of(2026, 8, 10);

        when(artisanAppointmentRepository.findByAppointmentDateBetween(
                date.atStartOfDay(),
                date.atStartOfDay().plusDays(1)
        )).thenReturn(Collections.emptyList());

        List<ArtisanAppointmentDTO> result =
                artisanAppointmentService.findAllByAppointmentDateBetween(date);

        assertTrue(result.isEmpty());

        verify(artisanAppointmentRepository).findByAppointmentDateBetween(
                date.atStartOfDay(),
                date.atStartOfDay().plusDays(1)
        );
    }

    //Find Appointment Detail By ID
    // Test kiem tra tim lich theo id ton tai thi service tra ve DTO chi tiet.
    @Test
    void findById_WhenAppointmentExists_ShouldReturnDto() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setAppointmentId(1);
        appointment.setAppointmentDate(LocalDateTime.of(2026, 8, 10, 9, 0));
        appointment.setStatus("PENDING");
        appointment.setNote("Test");

        User customer = new User();
        customer.setFullName("Sam");
        customer.setPhone("0123456789");
        customer.setEmail("sam@test.com");

        appointment.setCustomer(customer);

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));

        ArtisanAppointmentDTO result =
                artisanAppointmentService.findById(1);

        assertEquals(1, result.getAppointmentId());
        assertEquals("Sam", result.getCustomerName());
        assertEquals("0123456789", result.getCustomerPhone());
        assertEquals("sam@test.com", result.getCustomerEmail());
        assertEquals("PENDING", result.getStatus());
        assertEquals("Test", result.getNote());

        verify(artisanAppointmentRepository)
                .findByAppointmentId(1);
    }

    // Test kiem tra tim lich theo id khong ton tai thi service bao loi.
    @Test
    void findById_WhenAppointmentNotFound_ShouldThrowException() {

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.findById(1)
        );

        assertEquals(
                "Không tìm thấy lịch hẹn.",
                ex.getMessage()
        );

        verify(artisanAppointmentRepository)
                .findByAppointmentId(1);
    }

    //Check hand Update Status
    // Test kiem tra lich PENDING duoc artisan duyet sang APPROVED va luu lai.
    @Test
    void handUpdateStatus_WhenPendingAndApproved_ShouldUpdateAndSave() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setAppointmentId(1);
        appointment.setStatus("PENDING");
        appointment.setAppointmentDate(
                LocalDateTime.now().plusDays(1)
        );

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));
        artisanAppointmentService.handUpdateStatus(1, "approved");
        assertEquals("APPROVED", appointment.getStatus());
        verify(artisanAppointmentRepository)
                .save(appointment);
    }

    // Test kiem tra lich PENDING duoc artisan tu choi sang REJECTED va luu lai.
    @Test
    void handUpdateStatus_WhenPendingAndRejected_ShouldUpdateAndSave() {
        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setAppointmentId(1);
        appointment.setStatus("PENDING");
        appointment.setAppointmentDate(
                LocalDateTime.now().plusDays(1)
        );
        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));
        artisanAppointmentService.handUpdateStatus(1, "REJECTED");
        assertEquals("REJECTED", appointment.getStatus());

        verify(artisanAppointmentRepository)
                .save(appointment);
    }

    // Test kiem tra cap nhat trang thai lich khong ton tai thi service bao loi.
    @Test
    void handUpdateStatus_WhenNotFound_ShouldThrow() {
        when(artisanAppointmentRepository.findByAppointmentId(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> artisanAppointmentService.handUpdateStatus(1, "APPROVED"));
        assertEquals("Không tìm thấy lịch hẹn.", ex.getMessage());

        verify(artisanAppointmentRepository, never())
                .save(any());
    }

    // Test kiem tra lich da APPROVED thi khong duoc cap nhat trang thai thu cong tiep.
    @Test
    void handUpdateStatus_WhenAlreadyApproved_ShouldThrow() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setAppointmentId(1);
        appointment.setStatus("APPROVED");

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));
        RuntimeException ex = assertThrows(
                RuntimeException.class, () -> artisanAppointmentService.handUpdateStatus(1, "REJECTED"));
        assertEquals("Chỉ lịch đang chờ duyệt mới được cập nhật.", ex.getMessage());
        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra trang thai cap nhat khong hop le thi service tu choi.
    @Test
    void handUpdateStatus_WhenInvalidStatus_ShouldThrow() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setStatus("PENDING");
        appointment.setAppointmentDate(
                LocalDateTime.now().plusDays(1)
        );
        when(artisanAppointmentRepository.findByAppointmentId(1)).thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> artisanAppointmentService.handUpdateStatus(1, "DONE"));

        assertEquals("Trạng thái không hợp lệ.", ex.getMessage());

        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra lich da qua thoi gian hen thi khong duoc duyet thu cong.
    @Test
    void handUpdateStatus_WhenAppointmentExpired_ShouldThrow() {

        ViewingAppointment appointment = new ViewingAppointment();

        appointment.setAppointmentId(1);
        appointment.setStatus("PENDING");
        appointment.setAppointmentDate(
                LocalDateTime.now().minusHours(1)
        );


        when(artisanAppointmentRepository.findByAppointmentId(1)).thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> artisanAppointmentService.handUpdateStatus(1, "APPROVED"));


        assertEquals("Không thể cập nhật lịch đã quá thời gian hẹn.", ex.getMessage());

        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Check Hand mark Complete
    // Test kiem tra lich APPROVED da den han thi artisan danh dau COMPLETED thanh cong.
    @Test
    void handMarkComplete_WhenApprovedAndCompleted_ShouldUpdateAndSave() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setAppointmentId(1);
        appointment.setStatus("APPROVED");
        appointment.setAppointmentDate(
                LocalDateTime.now().minusHours(1)
        );

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));

        artisanAppointmentService.handMarkComplete(1, "COMPLETED");

        assertEquals("COMPLETED", appointment.getStatus());

        verify(artisanAppointmentRepository).save(appointment);
    }

    // Test kiem tra lich APPROVED da den han thi artisan danh dau ABSENT thanh cong.
    @Test
    void handMarkComplete_WhenApprovedAndAbsent_ShouldUpdateAndSave() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setAppointmentId(1);
        appointment.setStatus("APPROVED");
        appointment.setAppointmentDate(
                LocalDateTime.now().minusHours(1)
        );

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));

        artisanAppointmentService.handMarkComplete(1, "ABSENT");

        assertEquals("ABSENT", appointment.getStatus());

        verify(artisanAppointmentRepository).save(appointment);
    }

    // Test kiem tra danh dau hoan tat lich khong ton tai thi service bao loi.
    @Test
    void handMarkComplete_WhenNotFound_ShouldThrow() {

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.handMarkComplete(1, "COMPLETED")
        );

        assertEquals("Không tìm thấy lịch hẹn.", ex.getMessage());

        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra lich chua APPROVED thi khong duoc danh dau hoan tat.
    @Test
    void handMarkComplete_WhenNotApproved_ShouldThrow() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setStatus("PENDING");

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.handMarkComplete(1, "COMPLETED")
        );

        assertEquals(
                "Chỉ lịch được duyệt mới được cập nhật.",
                ex.getMessage()
        );

        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra lich chua den thoi gian hen thi khong duoc danh dau hoan tat.
    @Test
    void handMarkComplete_WhenAppointmentNotYet_ShouldThrow() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setStatus("APPROVED");
        appointment.setAppointmentDate(
                LocalDateTime.now().plusHours(1)
        );

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.handMarkComplete(1, "COMPLETED")
        );

        assertEquals(
                "Chưa đến thời gian lịch hẹn, không thể cập nhật.",
                ex.getMessage()
        );

        verify(artisanAppointmentRepository, never()).save(any());
    }


    // Test kiem tra trang thai hoan tat khong hop le thi service tu choi.
    @Test
    void handMarkComplete_WhenInvalidStatus_ShouldThrow() {

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setStatus("APPROVED");
        appointment.setAppointmentDate(
                LocalDateTime.now().minusHours(1)
        );

        when(artisanAppointmentRepository.findByAppointmentId(1))
                .thenReturn(Optional.of(appointment));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.handMarkComplete(1, "DONE")
        );

        assertEquals("Trạng thái không hợp lệ.", ex.getMessage());

        verify(artisanAppointmentRepository, never()).save(any());
    }

    //Check Appointment Setting
    // Test kiem tra cap nhat cau hinh khi setting khong ton tai thi service bao loi.
    @Test
    void updateSetting_WhenSettingNotFound_ShouldThrow() {

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        true,
                        30,
                        true,
                        60,
                        null,
                        null,
                        null,
                        new User()
                )
        );

        assertEquals("Không tìm thấy cấu hình.", ex.getMessage());

        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra cap nhat cau hinh hop le thi service luu lai.
    @Test
    void updateSetting_WhenValid_ShouldUpdateAndSave() {

        AppointmentSetting setting = new AppointmentSetting();
        setting.setSettingId(1);
        setting.setAutoApproveAfter(30);
        setting.setAutoCompleteAfter(60);

        User user = new User();
        user.setUserId(1);

        LocalDateTime pauseFrom = LocalDateTime.now().plusDays(1);
        LocalDateTime pauseTo = pauseFrom.plusHours(2);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        artisanAppointmentService.updateSetting(
                true,
                15,
                true,
                45,
                pauseFrom,
                pauseTo,
                "Bảo trì",
                user
        );

        assertTrue(setting.getAutoApprove());
        assertEquals(15, setting.getAutoApproveAfter());

        assertTrue(setting.getAutoComplete());
        assertEquals(45, setting.getAutoCompleteAfter());

        assertEquals(pauseFrom, setting.getPauseFrom());
        assertEquals(pauseTo, setting.getPauseTo());
        assertEquals("Bảo trì", setting.getPauseReason());

        assertEquals(user, setting.getUpdatedBy());
        assertNotNull(setting.getUpdatedAt());

        verify(appointmentSettingRepository).save(setting);
    }

    // Test kiem tra bat auto approve nhung thieu so phut thi service tu choi.
    @Test
    void updateSetting_WhenAutoApproveMinuteIsNull_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        true,
                        null,
                        false,
                        null,
                        null,
                        null,
                        null,
                        new User()
                )
        );
        assertEquals(
                "Thời gian tự động duyệt phải từ 1 đến 120 phút.",
                ex.getMessage()
        );
        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra auto approve minute bang 0 thi service tu choi.
    @Test
    void updateSetting_WhenAutoApproveMinuteIsZero_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        true,
                        0,
                        false,
                        null,
                        null,
                        null,
                        null,
                        new User()
                )
        );

        assertEquals(
                "Thời gian tự động duyệt phải từ 1 đến 120 phút.",
                ex.getMessage()
        );

        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra auto approve minute lon hon 120 thi service tu choi.
    @Test
    void updateSetting_WhenAutoApproveMinuteGreaterThan120_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        true,
                        121,
                        false,
                        null,
                        null,
                        null,
                        null,
                        new User()
                )
        );

        assertEquals(
                "Thời gian tự động duyệt phải từ 1 đến 120 phút.",
                ex.getMessage()
        );

        verify(appointmentSettingRepository, never()).save(any());
    }
    // Test kiem tra auto approve minute bang 1 thi service chap nhan va luu.
    @Test
    void updateSetting_WhenAutoApproveMinuteIsOne_ShouldSave() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        artisanAppointmentService.updateSetting(
                true,
                1,
                false,
                null,
                null,
                null,
                null,
                new User()
        );

        assertTrue(setting.getAutoApprove());
        assertEquals(1, setting.getAutoApproveAfter());

        verify(appointmentSettingRepository).save(setting);
    }
    // Test kiem tra auto approve minute bang 120 thi service chap nhan va luu.
    @Test
    void updateSetting_WhenAutoApproveMinuteIs120_ShouldSave() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        artisanAppointmentService.updateSetting(
                true,
                120,
                false,
                null,
                null,
                null,
                null,
                new User()
        );

        assertTrue(setting.getAutoApprove());
        assertEquals(120, setting.getAutoApproveAfter());

        verify(appointmentSettingRepository).save(setting);
    }

    // Test kiem tra tat auto approve thi service giu nguong cu theo logic hien tai.
    @Test
    void updateSetting_WhenAutoApproveDisabled_ShouldKeepCurrentValue() {

        AppointmentSetting setting = new AppointmentSetting();
        setting.setAutoApproveAfter(30);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        artisanAppointmentService.updateSetting(
                false,
                999,
                false,
                null,
                null,
                null,
                null,
                new User()
        );

        assertFalse(setting.getAutoApprove());
        assertEquals(30, setting.getAutoApproveAfter());

        verify(appointmentSettingRepository).save(setting);
    }

    // Test kiem tra bat auto complete nhung thieu so phut thi service tu choi.
    @Test
    void updateSetting_WhenAutoCompleteMinuteIsNull_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        true,
                        null,
                        null,
                        null,
                        null,
                        new User()
                )
        );

        assertEquals(
                "Thời gian tự động hoàn thành phải từ 1 đến 120 phút.",
                ex.getMessage()
        );

        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra auto complete minute nho hon 1 thi service tu choi.
    @Test
    void updateSetting_WhenAutoCompleteMinuteLessThanOne_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        true,
                        0,
                        null,
                        null,
                        null,
                        new User()
                )
        );

        assertEquals(
                "Thời gian tự động hoàn thành phải từ 1 đến 120 phút.",
                ex.getMessage()
        );

        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra auto complete minute lon hon 120 thi service tu choi.
    @Test
    void updateSetting_WhenAutoCompleteMinuteGreaterThan120_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        true,
                        121,
                        null,
                        null,
                        null,
                        new User()
                )
        );

        assertEquals(
                "Thời gian tự động hoàn thành phải từ 1 đến 120 phút.",
                ex.getMessage()
        );

        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra auto complete minute hop le thi service chap nhan va luu.
    @Test
    void updateSetting_WhenAutoCompleteMinuteValid_ShouldSave() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        artisanAppointmentService.updateSetting(
                false,
                null,
                true,
                30,
                null,
                null,
                null,
                new User()
        );

        assertTrue(setting.getAutoComplete());
        assertEquals(30, setting.getAutoCompleteAfter());

        verify(appointmentSettingRepository).save(setting);
    }

    // Test kiem tra tat auto complete thi service bo qua minute moi theo logic hien tai.
    @Test
    void updateSetting_WhenAutoCompleteDisabled_ShouldIgnoreMinuteValue() {

        AppointmentSetting setting = new AppointmentSetting();
        setting.setAutoCompleteAfter(15);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        artisanAppointmentService.updateSetting(
                false,
                null,
                false,
                999,
                null,
                null,
                null,
                new User()
        );

        assertFalse(setting.getAutoComplete());
        assertEquals(15, setting.getAutoCompleteAfter());

        verify(appointmentSettingRepository).save(setting);
    }

    // Test kiem tra pause period va pause reason hop le thi service luu lai.
    @Test
    void updateSetting_WhenPauseIsValid_ShouldSave() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        User user = new User();

        LocalDateTime from = LocalDateTime.now().plusDays(1);
        LocalDateTime to = from.plusHours(2);

        artisanAppointmentService.updateSetting(
                false,
                null,
                false,
                null,
                from,
                to,
                "Bảo trì",
                user
        );

        verify(appointmentSettingRepository).save(setting);

        assertEquals(from, setting.getPauseFrom());
        assertEquals(to, setting.getPauseTo());
        assertEquals("Bảo trì", setting.getPauseReason());
    }

    // Test kiem tra thieu pause end khi co pause start thi service tu choi.
    @Test
    void updateSetting_WhenPauseToIsNull_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc())
                .thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        false,
                        null,
                        LocalDateTime.now().plusDays(1),
                        null,
                        "Bảo trì",
                        new User()
                )
        );

        assertEquals(
                "Thiết lập tạm dừng phải nhập đầy đủ thời gian bắt đầu, kết thúc và lý do.",
                ex.getMessage()
        );
        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra thieu pause date khi cap nhat pause thi service tu choi.
    @Test
    void updateSetting_WhenPauseDateMissing_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();
        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        false,
                        null,
                        null,
                        null,
                        "Bảo trì",
                        new User()
                )
        );

        assertEquals("Thiết lập tạm dừng phải nhập đầy đủ thời gian bắt đầu, kết thúc và lý do.", ex.getMessage());
        verify(appointmentSettingRepository, never()).save(any());
    }
    // Test kiem tra thieu pause reason khi co pause period thi service tu choi.
    @Test
    void updateSetting_WhenPauseReasonIsNull_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();
        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        false,
                        null,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(1).plusHours(2),
                        null,
                        new User()
                )
        );

        assertEquals("Thiết lập tạm dừng phải nhập đầy đủ thời gian bắt đầu, kết thúc và lý do.", ex.getMessage());
        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra ly do tam dung vuot 500 ky tu thi service tu choi.
    @Test
    void updateSetting_WhenPauseReasonExceeds500Characters_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();
        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        false,
                        null,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(1).plusHours(2),
                        "a".repeat(501),
                        new User()
                )
        );

        assertTrue(ex.getMessage().contains("500"));
        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra pause start sau pause end thi service tu choi.
    @Test
    void updateSetting_WhenPauseFromAfterPauseTo_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();
        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));


        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        false,
                        null,
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now().plusDays(1),
                        "Bảo trì",
                        new User()
                )
        );

        assertEquals("Khoảng thời gian tạm dừng nhận lịch không hợp lệ.", ex.getMessage());


        verify(appointmentSettingRepository, never()).save(any());
    }

    // Test kiem tra pause start trong qua khu thi service tu choi.
    @Test
    void updateSetting_WhenPauseStartInPast_ShouldThrow() {

        AppointmentSetting setting = new AppointmentSetting();
        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));


        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> artisanAppointmentService.updateSetting(
                        false,
                        null,
                        false,
                        null,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(1),
                        "Bảo trì",
                        new User()
                )
        );


        assertEquals("Thời gian bắt đầu phải lớn hơn thời điểm hiện tại.", ex.getMessage());

        verify(appointmentSettingRepository, never()).save(any());
    }
    // Test kiem tra tat auto approve thi service giu auto approve minute cu.
    @Test
    void updateSetting_WhenAutoApproveDisabled_ShouldKeepOldMinute() {

        AppointmentSetting setting = new AppointmentSetting();
        setting.setAutoApproveAfter(30);
        setting.setAutoCompleteAfter(60);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));

        artisanAppointmentService.updateSetting(
                false,
                999,
                true,
                60,
                null,
                null,
                null,
                new User()
        );

        assertEquals(30, setting.getAutoApproveAfter());
        verify(appointmentSettingRepository).save(setting);
    }

    // Test kiem tra tat auto complete thi service giu auto complete minute cu.
    @Test
    void updateSetting_WhenAutoCompleteDisabled_ShouldKeepOldMinute() {

        AppointmentSetting setting = new AppointmentSetting();

        setting.setAutoApproveAfter(30);
        setting.setAutoCompleteAfter(60);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));


        artisanAppointmentService.updateSetting(
                true,
                30,
                false,
                999,
                null,
                null,
                null,
                new User()
        );


        assertEquals(60, setting.getAutoCompleteAfter());
        verify(appointmentSettingRepository).save(setting);
    }
    // Process Auto Approve
    // Test kiem tra auto approve khi thieu setting thi service bao loi.
    @Test
    void processAutoApprove_WhenSettingNotFound_ShouldThrow() {

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> artisanAppointmentService.processAutoApprove());

        assertEquals("Không tìm thấy cấu hình.", ex.getMessage());
        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra auto approve bi tat thi job tra ve 0.
    @Test
    void processAutoApprove_WhenAutoApproveDisabled_ShouldReturnZero() {

        AppointmentSetting setting = new AppointmentSetting();

        setting.setAutoApprove(false);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));
        int result = artisanAppointmentService.processAutoApprove();
        assertEquals(0, result);

        verify(artisanAppointmentRepository, never()).findByStatus(any());
        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra khong co lich PENDING thi auto approve tra ve 0.
    @Test
    void processAutoApprove_WhenNoPendingAppointment_ShouldReturnZero() {

        AppointmentSetting setting = new AppointmentSetting();

        setting.setAutoApprove(true);
        setting.setAutoApproveAfter(30);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));

        when(artisanAppointmentRepository.findByStatus("PENDING")).thenReturn(Collections.emptyList());

        int result = artisanAppointmentService.processAutoApprove();

        assertEquals(0, result);
        verify(artisanAppointmentRepository).findByStatus("PENDING");

        verify(artisanAppointmentRepository, never()).save(any());
    }
    // Test kiem tra lich PENDING qua nguong thi auto approve sang APPROVED.
    @Test
    void processAutoApprove_WhenAppointmentReachedThreshold_ShouldApprove() {

        AppointmentSetting setting = new AppointmentSetting();

        setting.setAutoApprove(true);
        setting.setAutoApproveAfter(30);


        ViewingAppointment appointment = new ViewingAppointment();

        appointment.setAppointmentId(1);
        appointment.setStatus("PENDING");
        appointment.setCreatedAt(LocalDateTime.now().minusHours(1));


        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));
        when(artisanAppointmentRepository.findByStatus("PENDING")).thenReturn(List.of(appointment));

        int result = artisanAppointmentService.processAutoApprove();

        assertEquals(1, result);
        assertEquals("APPROVED", appointment.getStatus());

        verify(artisanAppointmentRepository).save(appointment);
    }
    // Test kiem tra nhieu lich PENDING thi auto approve chi cap nhat lich du dieu kien.
    @Test
    void processAutoApprove_WhenMultipleAppointments_ShouldProcessOnlyEligibleOnes() {

        AppointmentSetting setting = new AppointmentSetting();

        setting.setAutoApprove(true);
        setting.setAutoApproveAfter(30);


        ViewingAppointment appointment1 = new ViewingAppointment();

        appointment1.setAppointmentId(1);
        appointment1.setStatus("PENDING");
        appointment1.setCreatedAt(
                LocalDateTime.now().minusHours(1)
        );

        ViewingAppointment appointment2 = new ViewingAppointment();

        appointment2.setAppointmentId(2);
        appointment2.setStatus("PENDING");
        appointment2.setCreatedAt(LocalDateTime.now().minusMinutes(5));


        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));
        when(artisanAppointmentRepository.findByStatus("PENDING")).thenReturn(List.of(appointment1, appointment2));
        int result = artisanAppointmentService.processAutoApprove();

        assertEquals(1, result);
        assertEquals("APPROVED", appointment1.getStatus());
        assertEquals("PENDING", appointment2.getStatus());

        verify(artisanAppointmentRepository).save(appointment1);
        verify(artisanAppointmentRepository, never()).save(appointment2);
    }
    //Process Auto Complete
    // Test kiem tra auto complete khi thieu setting thi service bao loi.
    @Test
    void processAutoComplete_WhenSettingNotFound_ShouldThrow() {

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> artisanAppointmentService.processAutoComplete());
        assertEquals("Không tìm thấy cấu hình.", ex.getMessage());

        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra auto complete bi tat thi job tra ve 0.
    @Test
    void processAutoComplete_WhenAutoCompleteDisabled_ShouldReturnZero() {

        AppointmentSetting setting = new AppointmentSetting();

        setting.setAutoComplete(false);
        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));

        int result = artisanAppointmentService.processAutoComplete();

        assertEquals(0, result);
        verify(artisanAppointmentRepository, never()).findByStatus(any());

        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra khong co lich APPROVED thi auto complete tra ve 0.
    @Test
    void processAutoComplete_WhenNoApprovedAppointment_ShouldReturnZero() {

        AppointmentSetting setting = new AppointmentSetting();

        setting.setAutoComplete(true);
        setting.setAutoCompleteAfter(30);

        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));

        when(artisanAppointmentRepository.findByStatus("APPROVED")).thenReturn(Collections.emptyList());

        int result = artisanAppointmentService.processAutoComplete();

        assertEquals(0, result);
        verify(artisanAppointmentRepository).findByStatus("APPROVED");

        verify(artisanAppointmentRepository, never()).save(any());
    }

    // Test kiem tra lich APPROVED qua nguong thi auto complete sang COMPLETED.
    @Test
    void processAutoComplete_WhenAppointmentReachedThreshold_ShouldComplete() {

        AppointmentSetting setting = new AppointmentSetting();
        setting.setAutoComplete(true);
        setting.setAutoCompleteAfter(30);

        ViewingAppointment appointment = new ViewingAppointment();

        appointment.setAppointmentId(1);
        appointment.setStatus("APPROVED");
        appointment.setAppointmentDate(LocalDateTime.now().minusHours(1));


        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));
        when(artisanAppointmentRepository.findByStatus("APPROVED")).thenReturn(List.of(appointment));

        int result = artisanAppointmentService.processAutoComplete();

        assertEquals(1, result);
        assertEquals("COMPLETED", appointment.getStatus());

        verify(artisanAppointmentRepository).save(appointment);
    }
    // Test kiem tra lich APPROVED chua qua nguong thi auto complete giu nguyen trang thai.
    @Test
    void processAutoComplete_WhenAppointmentNotReachedThreshold_ShouldKeepApproved() {

        AppointmentSetting setting = new AppointmentSetting();

        setting.setAutoComplete(true);
        setting.setAutoCompleteAfter(60);

        ViewingAppointment appointment = new ViewingAppointment();

        appointment.setAppointmentId(1);
        appointment.setStatus("APPROVED");
        appointment.setAppointmentDate(LocalDateTime.now().minusMinutes(10));


        when(appointmentSettingRepository.findFirstByOrderBySettingIdAsc()).thenReturn(Optional.of(setting));
        when(artisanAppointmentRepository.findByStatus("APPROVED")).thenReturn(List.of(appointment));


        int result = artisanAppointmentService.processAutoComplete();


        assertEquals(0, result);
        assertEquals("APPROVED", appointment.getStatus());


        verify(artisanAppointmentRepository, never()).save(any());
    }

}
