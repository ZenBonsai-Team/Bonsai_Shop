package com.example.bonsai_shop.system;

import com.example.bonsai_shop.appointmentSetting.service.AppointmentSettingService;
import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.artisan.service.ArtisanAppointmentService;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BF02PremiumTreeViewingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ViewingAppointmentRepository viewingAppointmentRepository;

    @Autowired
    private AppointmentSettingRepository appointmentSettingRepository;

    @Autowired
    private ArtisanAppointmentService artisanAppointmentService;

    @MockitoBean
    private AppointmentSettingService appointmentSettingService;

    private User createCustomer() {
        Role customerRole = findRole("CUSTOMER", "ROLE_CUSTOMER");
        String email = "bf02.customer@test.com";

        User customer = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("BF02 Customer")
                        .username("bf02_customer")
                        .email(email)
                        .password("123456")
                        .phone("0900000002")
                        .status("ACTIVE")
                        .role(customerRole)
                        .build()));

        customer.setPhone("0900000002");
        customer.setStatus("ACTIVE");
        customer.setRole(customerRole);
        return userRepository.save(customer);
    }

    private RequestPostProcessor artisanUser() {
        User artisan = createArtisan();

        return user(new CustomUserDetails(
                artisan,
                List.of(new SimpleGrantedAuthority("ROLE_ARTISAN"))
        ));
    }

    private RequestPostProcessor customerUser(User customer) {
        return user(new CustomUserDetails(
                customer,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));
    }

    private User createArtisan() {
        Role artisanRole = findRole("ARTISAN", "ROLE_ARTISAN");
        String email = "bf02.artisan@test.com";

        User artisan = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("BF02 Artisan")
                        .username("bf02_artisan")
                        .email(email)
                        .password("123456")
                        .phone("0900000003")
                        .status("ACTIVE")
                        .role(artisanRole)
                        .build()));

        artisan.setPhone("0900000003");
        artisan.setStatus("ACTIVE");
        artisan.setRole(artisanRole);
        return userRepository.save(artisan);
    }

    private Role findRole(String... roleNames) {
        List<String> expectedRoleNames = List.of(roleNames);

        return roleRepository.findAll()
                .stream()
                .filter(role -> expectedRoleNames.stream()
                        .anyMatch(roleName -> roleName.equalsIgnoreCase(role.getRoleName())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Required customer role not found"));
    }

    @Test
    void createAppointment_WhenValid_ShouldCreatePendingAppointment() throws Exception {
        // Given
        User customer = createCustomer();

        LocalDate appointmentDate = LocalDate.now().plusDays(1);

        // When
        mockMvc.perform(
                        post("/appointments/create")
                                .param("appointmentDate", appointmentDate.toString())
                                .param("appointmentTime", "10:00")
                                .param("note", "BF02 System Test")
                                .with(user(customer.getEmail()).roles("CUSTOMER"))
                                .with(csrf())
                )
                // Then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"));

        ViewingAppointment appointment =
                viewingAppointmentRepository
                        .findByCustomerOrderByCreatedAtDesc(customer)
                        .getFirst();

        assertEquals("PENDING", appointment.getStatus());
    }

    @Test
    void createAppointment_WhenAppointmentTimeIsDuringPause_ShouldRejectBooking() throws Exception {
        // Given
        User customer = createCustomer();

        LocalDate appointmentDate = LocalDate.now().plusDays(1);

        // Khi thời gian đặt lịch nằm trong khoảng pause
        when(appointmentSettingService.isPausedAt(any()))
                .thenReturn(true);

        int appointmentCountBefore =
                viewingAppointmentRepository
                        .findByCustomerOrderByCreatedAtDesc(customer)
                        .size();

        // When
        mockMvc.perform(
                        post("/appointments/create")
                                .param("appointmentDate", appointmentDate.toString())
                                .param("appointmentTime", "10:00")
                                .param("note", "BF02 Pause Test")
                                .with(user(customer.getEmail()).roles("CUSTOMER"))
                                .with(csrf())
                )
                // Then - HTTP Flow
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"))
                .andExpect(flash().attributeExists("error"));

        // Then - DB State
        int appointmentCountAfter =
                viewingAppointmentRepository
                        .findByCustomerOrderByCreatedAtDesc(customer)
                        .size();

        assertEquals(appointmentCountBefore, appointmentCountAfter);
    }

    @Test
    void updateAppointment_WhenPending_ShouldManuallyApproveAppointment() throws Exception {
        // Given
        User customer = createCustomer();

        ViewingAppointment appointment = ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status("PENDING")
                .note("BF02 Manual Approve Test")
                .build();

        viewingAppointmentRepository.save(appointment);

        // When
        mockMvc.perform(
                        post("/artisan/appointments/update")
                                .param("id", appointment.getAppointmentId().toString())
                                .param("status", "APPROVED")
                                .with(artisanUser())
                                .with(csrf())
                )
                // Then - HTTP Flow
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments"))
                .andExpect(flash().attributeExists("success"));

        // Then - DB State
        ViewingAppointment updatedAppointment =
                viewingAppointmentRepository
                        .findById(appointment.getAppointmentId())
                        .orElseThrow();

        assertEquals("APPROVED", updatedAppointment.getStatus());
    }

    @Test
    void updateSetting_WhenAutoApproveEnabled_ShouldSaveSetting() throws Exception {
        // Given
        User artisan = createArtisan();

        AppointmentSetting setting = appointmentSettingRepository
                .findFirstByOrderBySettingIdAsc()
                .orElseGet(() -> appointmentSettingRepository.save(
                        AppointmentSetting.builder().build()
                ));
        setting.setAutoCompleteAfter(6);
        appointmentSettingRepository.save(setting);

        // When
        mockMvc.perform(
                        post("/artisan/appointments/settings")
                                .param("autoApprove", "true")
                                .param("autoApproveAfter", "5")
                                .param("autoComplete", "true")
                                .param("autoCompleteAfter", "30")
                                .with(user(artisan.getEmail()).roles("ARTISAN"))
                                .with(csrf())
                )
                // Then - HTTP
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments"));

        // Then - DB
        AppointmentSetting updatedSetting =
                appointmentSettingRepository
                        .findFirstByOrderBySettingIdAsc()
                        .orElseThrow();

        assertEquals(true, updatedSetting.getAutoApprove());
        assertEquals(5, updatedSetting.getAutoApproveAfter());
        assertEquals(true, updatedSetting.getAutoComplete());
        assertEquals(30, updatedSetting.getAutoCompleteAfter());
    }

    @Test
    void processAutoApprove_WhenPendingAppointmentPassedThreshold_ShouldApprove() {
        // Given
        User customer = createCustomer();

        AppointmentSetting setting = appointmentSettingRepository
                .findFirstByOrderBySettingIdAsc()
                .orElseGet(() -> appointmentSettingRepository.save(AppointmentSetting.builder()
                        .build()));

        setting.setAutoApprove(true);
        setting.setAutoApproveAfter(5);
        setting.setAutoComplete(false);
        appointmentSettingRepository.save(setting);

        ViewingAppointment appointment = ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .updatedAt(LocalDateTime.now().minusMinutes(10))
                .status("PENDING")
                .note("BF02 Auto Approve Test")
                .build();

        viewingAppointmentRepository.save(appointment);

        // When
        int approvedCount = artisanAppointmentService.processAutoApprove();

        // Then
        ViewingAppointment updatedAppointment =
                viewingAppointmentRepository
                        .findById(appointment.getAppointmentId())
                        .orElseThrow();

        assertEquals(1, approvedCount);
        assertEquals("APPROVED", updatedAppointment.getStatus());
    }

    @Test
    void cancelAppointment_WhenPending_ShouldCancelAppointment() throws Exception {
        // Given
        User customer = createCustomer();

        ViewingAppointment appointment = ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status("PENDING")
                .note("BF02 Cancel Test")
                .build();

        viewingAppointmentRepository.save(appointment);

        // When
        mockMvc.perform(
                        post("/appointments/cancel/" + appointment.getAppointmentId())
                                .with(user(customer.getEmail()).roles("CUSTOMER"))
                                .with(csrf())
                )
                // Then - HTTP Flow
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"));

        // Then - DB State
        ViewingAppointment updatedAppointment =
                viewingAppointmentRepository
                        .findById(appointment.getAppointmentId())
                        .orElseThrow();

        assertEquals("CANCELLED", updatedAppointment.getStatus());
    }

    @Test
    void cancelAppointment_WhenApproved_ShouldRejectCancellation() throws Exception {
        // Given
        User customer = createCustomer();

        ViewingAppointment appointment = ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status("APPROVED")
                .note("BF02 Approved Cancel Test")
                .build();

        viewingAppointmentRepository.save(appointment);

        // When
        mockMvc.perform(
                        post("/appointments/cancel/" + appointment.getAppointmentId())
                                .with(user(customer.getEmail()).roles("CUSTOMER"))
                                .with(csrf())
                )
                // Then - HTTP Flow
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"))
                .andExpect(flash().attributeExists("error"));

        // Then - DB State
        ViewingAppointment updatedAppointment =
                viewingAppointmentRepository
                        .findById(appointment.getAppointmentId())
                        .orElseThrow();

        assertEquals("APPROVED", updatedAppointment.getStatus());
    }

    @Test
    void processAutoComplete_WhenApprovedAppointmentPassedScheduledTime_ShouldComplete() {
        // Given
        User customer = createCustomer();

        AppointmentSetting setting = appointmentSettingRepository
                .findFirstByOrderBySettingIdAsc()
                .orElseGet(() -> appointmentSettingRepository.save(
                        AppointmentSetting.builder().build()
                ));

        setting.setAutoApprove(false);
        setting.setAutoComplete(true);
        setting.setAutoCompleteAfter(5);
        appointmentSettingRepository.save(setting);

        ViewingAppointment appointment = ViewingAppointment.builder()
                .customer(customer)
                // Appointment time đã qua threshold
                .appointmentDate(LocalDateTime.now().minusMinutes(10))
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .updatedAt(LocalDateTime.now().minusMinutes(20))
                .status("APPROVED")
                .note("BF02 Auto Complete Test")
                .build();

        viewingAppointmentRepository.save(appointment);

        // When
        int completedCount = artisanAppointmentService.processAutoComplete();

        // Then - DB State
        ViewingAppointment updatedAppointment =
                viewingAppointmentRepository
                        .findById(appointment.getAppointmentId())
                        .orElseThrow();

        assertEquals(1, completedCount);
        assertEquals("COMPLETED", updatedAppointment.getStatus());
    }

    @Test
    void getAppointments_WhenCustomerHasAppointment_ShouldDisplayAppointmentList() throws Exception {
        // Given
        User customer = createCustomer();

        LocalDate appointmentDate = LocalDate.now().plusDays(1);

        ViewingAppointment appointment = ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(appointmentDate.atTime(10, 0))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status("PENDING")
                .note("BF02 Appointment List Test")
                .build();

        viewingAppointmentRepository.save(appointment);

        // When
        mockMvc.perform(
                        get("/appointments")
                                .with(customerUser(customer))
                                .with(csrf())
                )
                // Then - HTTP Flow
                .andExpect(status().isOk())
                .andExpect(view().name("customer/view-appointment"))
                .andExpect(model().attributeExists("viewingAppointments"));
    }

    @Test
    void getAppointmentDetail_WhenCustomerOwnsAppointment_ShouldReturnDetails() throws Exception {
        // Given
        User customer = createCustomer();

        ViewingAppointment appointment = ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status("PENDING")
                .note("BF02 Detail Test")
                .build();

        viewingAppointmentRepository.save(appointment);

        // When
        mockMvc.perform(
                        get("/appointments/detail/{id}", appointment.getAppointmentId())
                                .with(customerUser(customer))
                                .with(csrf())
                )
                // Then - HTTP Flow
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(appointment.getAppointmentId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.note").value("BF02 Detail Test"));
    }
}
