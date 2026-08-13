package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.appointmentSetting.reponsitory.AppointmentSettingRepository;
import com.example.bonsai_shop.artisan.repository.ArtisanAppointmentRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArtisanAppointmentControllerTest {

    private static final String CUSTOMER_EMAIL = "artisan-controller-customer@test.com";
    private static final String ARTISAN_EMAIL = "artisan-controller-artisan@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ArtisanAppointmentRepository artisanAppointmentRepository;

    @Autowired
    private AppointmentSettingRepository appointmentSettingRepository;

    private User customer;
    private User artisan;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_CUSTOMER")
                        .description("Customer")
                        .build()));

        customer = userRepository.findByEmail(CUSTOMER_EMAIL)
                .orElseGet(() -> User.builder()
                        .fullName("Artisan Controller Customer")
                        .username("artisan_controller_customer")
                        .email(CUSTOMER_EMAIL)
                        .password("password")
                        .status("ACTIVE")
                        .role(customerRole)
                        .build());

        customer.setPhone("0900000002");
        customer.setRole(customerRole);
        customer = userRepository.save(customer);

        Role artisanRole = roleRepository.findByRoleName("ROLE_ARTISAN")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_ARTISAN")
                        .description("Artisan")
                        .build()));

        artisan = userRepository.findByEmail(ARTISAN_EMAIL)
                .orElseGet(() -> User.builder()
                        .fullName("Artisan Controller User")
                        .username("artisan_controller_user")
                        .email(ARTISAN_EMAIL)
                        .password("password")
                        .status("ACTIVE")
                        .role(artisanRole)
                        .build());

        artisan.setRole(artisanRole);
        artisan = userRepository.save(artisan);

        artisanAppointmentRepository.deleteAll();
        appointmentSettingRepository.deleteAll();
    }

    @Test
    void showAppointments_WhenDateIsProvided_ShouldReturnManageScheduleView() throws Exception {
        LocalDate selectedDate = LocalDate.now().plusDays(2);
        createAppointment(selectedDate.atTime(9, 30), "PENDING", "Selected date");
        createAppointment(selectedDate.plusDays(1).atTime(10, 30), "PENDING", "Other date");

        mockMvc.perform(get("/artisan/appointments")
                        .param("date", selectedDate.toString())
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/manage-schedule"))
                .andExpect(model().attribute("selectedDate", selectedDate))
                .andExpect(model().attribute("appointments", hasSize(1)));
    }

    @Test
    void showAppointments_WhenDateIsMissing_ShouldUseToday() throws Exception {
        LocalDate today = LocalDate.now();
        createAppointment(today.atTime(9, 30), "PENDING", "Today");

        mockMvc.perform(get("/artisan/appointments")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/manage-schedule"))
                .andExpect(model().attribute("selectedDate", today))
                .andExpect(model().attribute("appointments", hasSize(1)));
    }

    @Test
    void showAppointments_WhenNoAppointments_ShouldReturnEmptyModelList() throws Exception {
        LocalDate selectedDate = LocalDate.now().plusDays(3);

        mockMvc.perform(get("/artisan/appointments")
                        .param("date", selectedDate.toString())
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/manage-schedule"))
                .andExpect(model().attribute("selectedDate", selectedDate))
                .andExpect(model().attribute("appointments", empty()));
    }

    @Test
    void getAppointmentsByDate_WhenDateIsProvided_ShouldReturnAppointmentsJson() throws Exception {
        LocalDate selectedDate = LocalDate.now().plusDays(4);
        ViewingAppointment appointment = createAppointment(
                LocalDateTime.of(selectedDate, LocalTime.of(14, 0)),
                "APPROVED",
                "Json appointment"
        );
        createAppointment(selectedDate.plusDays(1).atTime(15, 0), "PENDING", "Other date");

        mockMvc.perform(get("/artisan/appointments/data")
                        .param("date", selectedDate.toString())
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].appointmentId").value(appointment.getAppointmentId()))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].note").value("Json appointment"))
                .andExpect(jsonPath("$[0].customerEmail").value(CUSTOMER_EMAIL));
    }

    @Test
    void getAppointmentsByDate_WhenDateIsMissing_ShouldUseToday() throws Exception {
        LocalDate today = LocalDate.now();
        ViewingAppointment appointment = createAppointment(today.atTime(11, 0), "PENDING", "Today json");

        mockMvc.perform(get("/artisan/appointments/data")
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].appointmentId").value(appointment.getAppointmentId()));
    }

    @Test
    void getAppointmentsByDate_WhenNoAppointments_ShouldReturnEmptyJsonList() throws Exception {
        LocalDate selectedDate = LocalDate.now().plusDays(5);

        mockMvc.perform(get("/artisan/appointments/data")
                        .param("date", selectedDate.toString())
                        .with(artisanUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void updateAppointment_WhenPendingAppointmentAndDateProvided_ShouldApproveAndRedirectSelectedDate() throws Exception {
        LocalDate selectedDate = LocalDate.now().plusDays(6);
        ViewingAppointment appointment = createAppointment(selectedDate.atTime(9, 0), "PENDING", "Approve");

        mockMvc.perform(post("/artisan/appointments/update")
                        .param("id", appointment.getAppointmentId().toString())
                        .param("status", "APPROVED")
                        .param("date", selectedDate.toString())
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments?date=" + selectedDate))
                .andExpect(flash().attributeExists("success"));

        ViewingAppointment updatedAppointment = artisanAppointmentRepository
                .findByAppointmentId(appointment.getAppointmentId())
                .orElseThrow();
        assertEquals("APPROVED", updatedAppointment.getStatus());
    }

    @Test
    void updateAppointment_WhenServiceThrows_ShouldRedirectSelectedDateWithError() throws Exception {
        LocalDate selectedDate = LocalDate.now().plusDays(7);
        ViewingAppointment appointment = createAppointment(selectedDate.atTime(9, 0), "APPROVED", "Already approved");

        mockMvc.perform(post("/artisan/appointments/update")
                        .param("id", appointment.getAppointmentId().toString())
                        .param("status", "REJECTED")
                        .param("date", selectedDate.toString())
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments?date=" + selectedDate))
                .andExpect(flash().attributeExists("error"));

        ViewingAppointment unchangedAppointment = artisanAppointmentRepository
                .findByAppointmentId(appointment.getAppointmentId())
                .orElseThrow();
        assertEquals("APPROVED", unchangedAppointment.getStatus());
    }

    @Test
    void updateAppointment_WhenDateIsMissing_ShouldRedirectAppointmentsRoot() throws Exception {
        ViewingAppointment appointment = createAppointment(
                LocalDate.now().plusDays(8).atTime(10, 0),
                "PENDING",
                "No date"
        );

        mockMvc.perform(post("/artisan/appointments/update")
                        .param("id", appointment.getAppointmentId().toString())
                        .param("status", "REJECTED")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void markComplete_WhenApprovedPastAppointmentAndDateProvided_ShouldCompleteAndRedirectSelectedDate() throws Exception {
        LocalDate selectedDate = LocalDate.now().minusDays(1);
        ViewingAppointment appointment = createAppointment(selectedDate.atTime(9, 0), "APPROVED", "Complete");

        mockMvc.perform(post("/artisan/appointments/complete")
                        .param("id", appointment.getAppointmentId().toString())
                        .param("status", "COMPLETED")
                        .param("date", selectedDate.toString())
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments?date=" + selectedDate))
                .andExpect(flash().attributeExists("success"));

        ViewingAppointment updatedAppointment = artisanAppointmentRepository
                .findByAppointmentId(appointment.getAppointmentId())
                .orElseThrow();
        assertEquals("COMPLETED", updatedAppointment.getStatus());
    }

    @Test
    void markComplete_WhenServiceThrows_ShouldRedirectSelectedDateWithError() throws Exception {
        LocalDate selectedDate = LocalDate.now().minusDays(2);
        ViewingAppointment appointment = createAppointment(selectedDate.atTime(9, 0), "PENDING", "Not approved");

        mockMvc.perform(post("/artisan/appointments/complete")
                        .param("id", appointment.getAppointmentId().toString())
                        .param("status", "COMPLETED")
                        .param("date", selectedDate.toString())
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments?date=" + selectedDate))
                .andExpect(flash().attributeExists("error"));

        ViewingAppointment unchangedAppointment = artisanAppointmentRepository
                .findByAppointmentId(appointment.getAppointmentId())
                .orElseThrow();
        assertEquals("PENDING", unchangedAppointment.getStatus());
    }

    @Test
    void markComplete_WhenDateIsMissing_ShouldRedirectAppointmentsRoot() throws Exception {
        ViewingAppointment appointment = createAppointment(
                LocalDate.now().minusDays(3).atTime(10, 0),
                "APPROVED",
                "No date complete"
        );

        mockMvc.perform(post("/artisan/appointments/complete")
                        .param("id", appointment.getAppointmentId().toString())
                        .param("status", "ABSENT")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void updateSetting_WhenValid_ShouldUpdateSettingAndRedirect() throws Exception {
        AppointmentSetting setting = appointmentSettingRepository.save(AppointmentSetting.builder()
                .autoApprove(true)
                .autoApproveAfter(5)
                .autoComplete(true)
                .autoCompleteAfter(60)
                .build());

        LocalDateTime pauseFrom = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        LocalDateTime pauseTo = pauseFrom.plusHours(2);

        mockMvc.perform(post("/artisan/appointments/settings")
                        .param("autoApprove", "true")
                        .param("autoApproveAfter", "10")
                        .param("autoComplete", "true")
                        .param("autoCompleteAfter", "30")
                        .param("pauseFrom", pauseFrom.toString())
                        .param("pauseTo", pauseTo.toString())
                        .param("pauseReason", "Maintenance")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments"))
                .andExpect(flash().attributeExists("success"));

        AppointmentSetting updatedSetting = appointmentSettingRepository
                .findById(setting.getSettingId())
                .orElseThrow();
        assertTrue(updatedSetting.getAutoApprove());
        assertEquals(10, updatedSetting.getAutoApproveAfter());
        assertTrue(updatedSetting.getAutoComplete());
        assertEquals(30, updatedSetting.getAutoCompleteAfter());
        assertEquals(pauseFrom, updatedSetting.getPauseFrom());
        assertEquals(pauseTo, updatedSetting.getPauseTo());
        assertEquals("Maintenance", updatedSetting.getPauseReason());
        assertEquals(artisan.getUserId(), updatedSetting.getUpdatedBy().getUserId());
    }

    @Test
    void updateSetting_WhenServiceThrows_ShouldRedirectWithError() throws Exception {
        appointmentSettingRepository.save(AppointmentSetting.builder()
                .autoApprove(true)
                .autoApproveAfter(5)
                .autoComplete(true)
                .autoCompleteAfter(60)
                .build());

        mockMvc.perform(post("/artisan/appointments/settings")
                        .param("autoApprove", "true")
                        .param("autoApproveAfter", "0")
                        .param("autoComplete", "true")
                        .param("autoCompleteAfter", "30")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void updateSetting_WhenNoSettingExists_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/artisan/appointments/settings")
                        .param("autoApprove", "true")
                        .param("autoApproveAfter", "10")
                        .param("autoComplete", "true")
                        .param("autoCompleteAfter", "30")
                        .with(artisanUser())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/appointments"))
                .andExpect(flash().attributeExists("error"));
    }

    private ViewingAppointment createAppointment(LocalDateTime appointmentDate,
                                                 String status,
                                                 String note) {
        ViewingAppointment appointment = ViewingAppointment.builder()
                .customer(customer)
                .appointmentDate(appointmentDate)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(status)
                .note(note)
                .build();

        return artisanAppointmentRepository.save(appointment);
    }

    private RequestPostProcessor artisanUser() {
        return user(new CustomUserDetails(
                artisan,
                List.of(new SimpleGrantedAuthority("ROLE_ARTISAN"))
        ));
    }
}
