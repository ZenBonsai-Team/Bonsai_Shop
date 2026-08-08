package com.example.bonsai_shop.integration.moderator;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.integration.support.BaseControllerIntegrationTest;
import com.example.bonsai_shop.moderator.dto.OrderActionRequestDTO;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public class ModeratorOrderControllerIntegrationTest extends BaseControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User createTestUser(String email, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(roleName).description(roleName).build()));
        User user = new User();
        user.setEmail(email);
        user.setFullName("Test " + roleName);
        user.setPhone("0987654321");
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

    @DisplayName("TC-IT-MOD-01: Moderator lấy danh sách My Orders JSON API")
    @Test
    @WithMockUser(username = "mod.json@example.com", roles = {"MODERATOR"})
    void testGetMyOrdersJsonApi() throws Exception {
        createTestUser("mod.json@example.com", "MODERATOR");

        mockMvc.perform(MockMvcRequestBuilders.get("/moderator/orders/api/my-orders"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.orders").exists());
    }

    @DisplayName("TC-IT-MOD-02: Facade Action Router - Thực thi action claim hợp lệ")
    @Test
    @WithMockUser(username = "mod.action@example.com", roles = {"MODERATOR"})
    void testExecuteOrderActionClaimSuccess() throws Exception {
        createTestUser("mod.action@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-ACTION-02");
        order.setOrderStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        OrderActionRequestDTO requestDTO = new OrderActionRequestDTO();
        requestDTO.setAction("claim");

        mockMvc.perform(MockMvcRequestBuilders.post("/moderator/orders/api/action/ORD-ACTION-02").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @DisplayName("TC-IT-MOD-03: Action Router - Loại action không hợp lệ (Bad Request)")
    @Test
    @WithMockUser(username = "mod.bad@example.com", roles = {"MODERATOR"})
    void testExecuteOrderActionInvalidType() throws Exception {
        createTestUser("mod.bad@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-ACTION-03");
        order.setOrderStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        OrderActionRequestDTO requestDTO = new OrderActionRequestDTO();
        requestDTO.setAction("invalid_action_type");

        mockMvc.perform(MockMvcRequestBuilders.post("/moderator/orders/api/action/ORD-ACTION-03").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @DisplayName("TC-IT-MOD-04: Action Router - Moderator B cố unclaim đơn của Moderator A")
    @Test
    @WithMockUser(username = "modB.action@example.com", roles = {"MODERATOR"})
    void testExecuteOrderActionOwnershipViolation() throws Exception {
        User modA = createTestUser("modA.action@example.com", "MODERATOR");
        createTestUser("modB.action@example.com", "MODERATOR");

        Order order = new Order();
        order.setOrderCode("ORD-ACTION-04");
        order.setOrderStatus("PENDING");
        order.setAssignedTo(modA);
        order.setAssignedAt(LocalDateTime.now());
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        OrderActionRequestDTO requestDTO = new OrderActionRequestDTO();
        requestDTO.setAction("unclaim");

        mockMvc.perform(MockMvcRequestBuilders.post("/moderator/orders/api/action/ORD-ACTION-04").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @DisplayName("TC-IT-MOD-05: Customer gọi API Moderator Action (403 Forbidden)")
    @Test
    @WithMockUser(username = "cust.action@example.com", roles = {"CUSTOMER"})
    void testExecuteOrderActionCustomerRoleForbidden() throws Exception {
        OrderActionRequestDTO requestDTO = new OrderActionRequestDTO();
        requestDTO.setAction("claim");

        mockMvc.perform(MockMvcRequestBuilders.post("/moderator/orders/api/action/ORD-ACTION-05").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @DisplayName("TC-IT-MOD-06: Parameterized Test rendering giao diện HTML Moderator")
    @ParameterizedTest
    @ValueSource(strings = {"/moderator/orders/pool", "/moderator/orders/my"})
    void testModeratorViewsRendering(String url) throws Exception {
        User modUser = createTestUser("mod.view@example.com", "MODERATOR");
        CustomUserDetails customUserDetails = new CustomUserDetails(modUser, List.of(new SimpleGrantedAuthority("ROLE_MODERATOR")));

        mockMvc.perform(MockMvcRequestBuilders.get(url).with(user(customUserDetails)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
