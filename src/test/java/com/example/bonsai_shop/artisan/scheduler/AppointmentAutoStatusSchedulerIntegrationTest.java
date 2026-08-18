package com.example.bonsai_shop.artisan.scheduler;

import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class AppointmentAutoStatusSchedulerIntegrationTest {

    @Autowired
    private AppointmentAutoStatusScheduler appointmentAutoStatusScheduler;

    @Autowired
    private AppointmentSettingRepository appointmentSettingRepository;

    @Autowired
    private ViewingAppointmentRepository viewingAppointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    private User customer;

    @BeforeEach
    void setUpSchedulerTestData() {
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_CUSTOMER")
                        .description("Customer")
                        .build()));

        customer = userRepository.save(User.builder()
                .fullName("Scheduler Customer")
                .username("scheduler_customer_" + System.nanoTime())
                .email("scheduler_customer_" + System.nanoTime() + "@test.com")
                .password("password")
                .phone("0900000000")
                .status("ACTIVE")
                .role(customerRole)
                .build());
    }

    @Test
    @DisplayName("TC-SCH-JOB03-001 - JOB-03 autoApprove changes eligible pending appointment to approved")
    void tcSchJob03001_autoApprove_WhenPendingAppointmentPassesThreshold_ShouldApprove(CapturedOutput output) {
        configureAppointmentSetting();

        ViewingAppointment appointment = viewingAppointmentRepository.save(ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now().minusMinutes(35))
                .updatedAt(LocalDateTime.now().minusMinutes(35))
                .note("TC-SCH-JOB03-001")
                .status("PENDING")
                .build());

        appointmentAutoStatusScheduler.autoApprove();
        entityManager.flush();
        entityManager.clear();

        ViewingAppointment updatedAppointment = viewingAppointmentRepository
                .findById(appointment.getAppointmentId())
                .orElseThrow();

        assertEquals("APPROVED", updatedAppointment.getStatus());
        assertTrue(output.getAll().contains("appointments have been auto approved"));
    }

    @Test
    @DisplayName("TC-SCH-JOB04-001 - JOB-04 scheduledAutoComplete changes eligible approved appointment to completed")
    void tcSchJob04001_scheduledAutoComplete_WhenApprovedAppointmentPassesThreshold_ShouldComplete(CapturedOutput output) {
        configureAppointmentSetting();

        ViewingAppointment appointment = viewingAppointmentRepository.save(ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(LocalDateTime.now().minusMinutes(70))
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusMinutes(70))
                .note("TC-SCH-JOB04-001")
                .status("APPROVED")
                .build());

        appointmentAutoStatusScheduler.scheduledAutoComplete();
        entityManager.flush();
        entityManager.clear();

        ViewingAppointment updatedAppointment = viewingAppointmentRepository
                .findById(appointment.getAppointmentId())
                .orElseThrow();

        assertEquals("COMPLETED", updatedAppointment.getStatus());
        assertTrue(output.getAll().contains("appointments have been auto completed"));
    }

    private void configureAppointmentSetting() {
        AppointmentSetting setting = appointmentSettingRepository.findFirstByOrderBySettingIdAsc()
                .orElseGet(AppointmentSetting::new);
        setting.setAutoApprove(true);
        setting.setAutoApproveAfter(30);
        setting.setAutoComplete(true);
        setting.setAutoCompleteAfter(60);
        appointmentSettingRepository.save(setting);
        entityManager.flush();
    }
}
