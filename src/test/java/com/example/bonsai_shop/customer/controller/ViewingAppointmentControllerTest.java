package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.appointmentSetting.service.AppointmentSettingService;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.AppointmentSetting;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.ViewingAppointment;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ViewingAppointmentControllerTest {

    private static final String TEST_EMAIL = "sam@test.com";
    private static final String OTHER_EMAIL = "other@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ViewingAppointmentRepository viewingAppointmentRepository;

    @MockitoBean
    private AppointmentSettingService appointmentSettingService;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_CUSTOMER")
                        .description("Customer")
                        .build()));

        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseGet(() -> User.builder()
                        .fullName("Sam Test")
                        .username("sam_test")
                        .email(TEST_EMAIL)
                        .password("password")
                        .status("ACTIVE")
                        .role(customerRole)
                        .build());

        user.setPhone("0900000000");
        user.setRole(customerRole);
        userRepository.save(user);

        viewingAppointmentRepository.deleteAll(
                viewingAppointmentRepository.findByCustomerOrderByCreatedAtDesc(user)
        );

        when(appointmentSettingService.isPausedAt(any())).thenReturn(false);
    }
















    // Test kiem tra khach hang tao lich hen hop le thi he thong luu lich voi trang thai PENDING.
    @Test
    void createAppointment_WhenValid_ShouldCreateAppointment() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        LocalDate appointmentDate = LocalDate.now().plusDays(1);

        mockMvc.perform(
                        post("/appointments/create")
                                .param("appointmentDate", appointmentDate.toString())
                                .param("appointmentTime", "10:00")
                                .param("note", "Test Integration")
                                .with(user(TEST_EMAIL).roles("CUSTOMER"))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        List<ViewingAppointment> appointments =
                viewingAppointmentRepository.findByCustomerOrderByCreatedAtDesc(user);

        assertFalse(appointments.isEmpty());

        ViewingAppointment appointment = appointments.getFirst();

        assertEquals(appointmentDate, appointment.getAppointmentDate().toLocalDate());
        assertEquals("10:00", appointment.getAppointmentDate().toLocalTime().toString());
        assertEquals("Test Integration", appointment.getNote());
        assertEquals("PENDING", appointment.getStatus());
        assertEquals(user.getUserId(), appointment.getCustomer().getUserId());
    }

    // Test kiem tra tai khoan nhan su khong duoc phep tao lich hen xem bonsai cho khach hang.
    @ParameterizedTest
    @ValueSource(strings = {
            "OWNER",
            "ARTISAN",
            "MODERATOR",
            "CONTENT_MODERATOR"
    })
    void createAppointment_WhenNonCustomerRole_ShouldReject(String roleName) throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        String fullRoleName = "ROLE_" + roleName;

        Role role = roleRepository.findByRoleName(fullRoleName)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName(fullRoleName)
                                .description(roleName)
                                .build()
                ));

        user.setRole(role);
        userRepository.save(user);

        LocalDate appointmentDate = LocalDate.now().plusDays(1);

        mockMvc.perform(
                        post("/appointments/create")
                                .param("appointmentDate", appointmentDate.toString())
                                .param("appointmentTime", "10:00")
                                .param("note", "Test " + roleName)
                                .with(user(TEST_EMAIL).roles(roleName))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        List<ViewingAppointment> appointments =
                viewingAppointmentRepository.findByCustomerOrderByCreatedAtDesc(user);

        assertTrue(appointments.isEmpty());
    }

    // Test kiem tra tao lich bi service tu choi thi controller chuyen huong kem thong bao loi.
    @Test
    void createAppointment_WhenServiceThrowsException_ShouldRedirectWithError() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        when(appointmentSettingService.isPausedAt(any())).thenReturn(true);
        when(appointmentSettingService.getAppointmentSetting()).thenReturn(
                AppointmentSetting.builder()
                        .pauseReason("Lịch hẹn đã tồn tại.")
                        .build()
        );

        LocalDate appointmentDate = LocalDate.now().plusDays(1);

        mockMvc.perform(
                        post("/appointments/create")
                                .param("appointmentDate", appointmentDate.toString())
                                .param("appointmentTime", "10:00")
                                .param("note", "Test Error")
                                .with(user(TEST_EMAIL).roles("CUSTOMER"))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attributeExists("error"));

        List<ViewingAppointment> appointments =
                viewingAppointmentRepository.findByCustomerOrderByCreatedAtDesc(user);

        assertTrue(appointments.isEmpty());
    }

    // Test kiem tra khach hang co lich hen thi API tra ve danh sach lich hen cua khach do.
    @Test
    void myAppointment_WhenCustomerHasAppointments_ShouldReturnViewWithAppointments() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        viewingAppointmentRepository.save(ViewingAppointment.builder()
                .customer(user)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .note("Existing appointment")
                .status("PENDING")
                .build());

        mockMvc.perform(
                        get("/appointments/list")
                                .with(user(new CustomUserDetails(
                                        user,
                                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                )))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].note").value("Existing appointment"));
    }

    // Test kiem tra khach hang chua co lich hen thi API tra ve danh sach rong.
    @Test
    void myAppointment_WhenCustomerHasNoAppointments_ShouldReturnViewWithEmptyAppointments() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        mockMvc.perform(
                        get("/appointments/list")
                                .with(user(new CustomUserDetails(
                                        user,
                                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                )))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // Test kiem tra khong tim thay nguoi dung hien tai thi he thong phat sinh loi khi xem danh sach lich hen.
    @Test
    void myAppointment_WhenUserNotFound_ShouldThrowException() {
        assertThrows(
                ServletException.class,
                () -> mockMvc.perform(
                        get("/appointments/list")
                                .with(user("missing@test.com").roles("CUSTOMER"))
                )
        );
    }

    // Test kiem tra khach hang xem chi tiet lich hen cua minh thi API tra ve thong tin chi tiet.
    @Test
    void viewingAppointmentDetail_WhenOwnedAppointmentExists_ShouldReturnDetail() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        LocalDateTime appointmentDate = LocalDateTime.now().plusDays(1).withNano(0);
        ViewingAppointment appointment = viewingAppointmentRepository.save(ViewingAppointment.builder()
                .customer(user)
                .appointmentDate(appointmentDate)
                .note("Detail note")
                .status("PENDING")
                .build());

        mockMvc.perform(
                        get("/appointments/detail/{id}", appointment.getAppointmentId())
                                .with(user(new CustomUserDetails(
                                        user,
                                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                )))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(appointment.getAppointmentId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.note").value("Detail note"));
    }

    // Test kiem tra khach hang khong duoc xem chi tiet lich hen cua khach hang khac.
    @Test
    void viewingAppointmentDetail_WhenAppointmentBelongsToAnotherCustomer_ShouldThrowException() {
        User currentUser = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseThrow();
        User anotherUser = User.builder()
                .fullName("Other Test")
                .username("other_test")
                .email(OTHER_EMAIL)
                .password("password")
                .phone("0900000001")
                .status("ACTIVE")
                .role(customerRole)
                .build();
        userRepository.save(anotherUser);

        ViewingAppointment appointment = viewingAppointmentRepository.save(ViewingAppointment.builder()
                .customer(anotherUser)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .note("Other customer appointment")
                .status("PENDING")
                .build());

        assertThrows(
                ServletException.class,
                () -> mockMvc.perform(
                        get("/appointments/detail/{id}", appointment.getAppointmentId())
                                .with(user(new CustomUserDetails(
                                        currentUser,
                                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                )))
                )
        );
    }

    // Test kiem tra xem chi tiet lich hen khong ton tai thi he thong phat sinh loi.
    @Test
    void viewingAppointmentDetail_WhenAppointmentIdNotFound_ShouldThrowException() {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        assertThrows(
                ServletException.class,
                () -> mockMvc.perform(
                        get("/appointments/detail/{id}", 999999)
                                .with(user(new CustomUserDetails(
                                        user,
                                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                )))
                )
        );
    }

    // Test kiem tra khach hang cap nhat lich hen PENDING hop le thi he thong luu thay doi va bao thanh cong.
    @Test
    void updateAppointment_WhenValid_ShouldUpdateAppointmentAndRedirectWithSuccess() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        ViewingAppointment appointment = viewingAppointmentRepository.save(ViewingAppointment.builder()
                .customer(user)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .note("Old note")
                .status("PENDING")
                .build());

        LocalDate updatedDate = LocalDate.now().plusDays(2);

        mockMvc.perform(
                        post("/appointments/update/{id}", appointment.getAppointmentId())
                                .param("appointmentDate", updatedDate.toString())
                                .param("appointmentTime", "11:00")
                                .param("note", "Updated note")
                                .with(user(TEST_EMAIL).roles("CUSTOMER"))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attributeExists("success"));

        ViewingAppointment updatedAppointment = viewingAppointmentRepository
                .findById(appointment.getAppointmentId())
                .orElseThrow();

        assertEquals(updatedDate, updatedAppointment.getAppointmentDate().toLocalDate());
        assertEquals("11:00", updatedAppointment.getAppointmentDate().toLocalTime().toString());
        assertEquals("Updated note", updatedAppointment.getNote());
    }

    // Test kiem tra cap nhat lich hen bi service tu choi thi controller chuyen huong kem thong bao loi.
    @Test
    void updateAppointment_WhenServiceThrowsException_ShouldRedirectWithError() throws Exception {
        LocalDate updatedDate = LocalDate.now().plusDays(2);

        mockMvc.perform(
                        post("/appointments/update/{id}", 999999)
                                .param("appointmentDate", updatedDate.toString())
                                .param("appointmentTime", "11:00")
                                .param("note", "Updated note")
                                .with(user(TEST_EMAIL).roles("CUSTOMER"))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attributeExists("error"));
    }

    // Test kiem tra dinh dang gio cap nhat khong hop le thi request phat sinh loi truoc khi xu ly nghiep vu.
    @Test
    void updateAppointment_WhenTimeFormatInvalid_ShouldThrowBeforeCatch() {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        ViewingAppointment appointment = viewingAppointmentRepository.save(ViewingAppointment.builder()
                .customer(user)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .note("Old note")
                .status("PENDING")
                .build());

        LocalDate updatedDate = LocalDate.now().plusDays(2);

        assertThrows(
                ServletException.class,
                () -> mockMvc.perform(
                        post("/appointments/update/{id}", appointment.getAppointmentId())
                                .param("appointmentDate", updatedDate.toString())
                                .param("appointmentTime", "invalid-time")
                                .param("note", "Updated note")
                                .with(user(TEST_EMAIL).roles("CUSTOMER"))
                                .with(csrf())
                )
        );
    }

    // Test kiem tra khach hang huy lich hen PENDING hop le thi trang thai chuyen sang CANCELLED.
    @Test
    void cancelAppointment_WhenValid_ShouldCancelAppointmentAndRedirectWithSuccess() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow();

        ViewingAppointment appointment = viewingAppointmentRepository.save(ViewingAppointment.builder()
                .customer(user)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .note("Cancel note")
                .status("PENDING")
                .build());

        mockMvc.perform(
                        post("/appointments/cancel/{id}", appointment.getAppointmentId())
                                .with(user(TEST_EMAIL).roles("CUSTOMER"))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attributeExists("success"));

        ViewingAppointment cancelledAppointment = viewingAppointmentRepository
                .findById(appointment.getAppointmentId())
                .orElseThrow();

        assertEquals("CANCELLED", cancelledAppointment.getStatus());
    }

    // Test kiem tra huy lich hen bi service tu choi thi controller chuyen huong kem thong bao loi.
    @Test
    void cancelAppointment_WhenServiceThrowsException_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(
                        post("/appointments/cancel/{id}", 999999)
                                .with(user(TEST_EMAIL).roles("CUSTOMER"))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attributeExists("error"));
    }

    // Test kiem tra khong tim thay nguoi dung hien tai thi he thong phat sinh loi khi huy lich hen.
    @Test
    void cancelAppointment_WhenUserNotFound_ShouldThrowBeforeCatch() {
        assertThrows(
                ServletException.class,
                () -> mockMvc.perform(
                        post("/appointments/cancel/{id}", 999999)
                                .with(user("missing@test.com").roles("CUSTOMER"))
                                .with(csrf())
                )
        );
    }

}
