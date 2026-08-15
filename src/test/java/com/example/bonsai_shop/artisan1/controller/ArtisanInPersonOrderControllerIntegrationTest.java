package com.example.bonsai_shop.artisan1.controller;

import com.example.bonsai_shop.artisan.controller.ArtisanInPersonOrderController;
import com.example.bonsai_shop.artisan.service.ArtisanInPersonOrderService;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.service.OrderExpirationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ArtisanInPersonOrderControllerIntegrationTest {

    private ArtisanInPersonOrderService inPersonOrderService;
    private OrderExpirationService orderExpirationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        inPersonOrderService = mock(ArtisanInPersonOrderService.class);
        orderExpirationService = mock(OrderExpirationService.class);
        ArtisanInPersonOrderController controller = new ArtisanInPersonOrderController(inPersonOrderService, orderExpirationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("artisan@test.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ARTISAN")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void index_WhenArtisanRequestsManagementPage_ShouldDisplayProductsAndOrders() throws Exception {
        List<Product> availableProducts = List.of(product(101), product(102));
        Page<Order> orders = new PageImpl<>(List.of(order(1, "PENDING_PAYMENT")));

        when(inPersonOrderService.getAvailableProducts("artisan@test.com"))
                .thenReturn(availableProducts);
        when(inPersonOrderService.getInPersonOrders("artisan@test.com", "ALL", 0, 10))
                .thenReturn(orders);

        mockMvc.perform(get("/artisan/in-person-order"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/in-person-order"))
                .andExpect(model().attribute("availableProducts", availableProducts))
                .andExpect(model().attribute("orders", orders))
                .andExpect(model().attribute("selectedStatus", "ALL"))
                .andExpect(model().attribute("pendingPaymentStatus", "PENDING_PAYMENT"))
                .andExpect(model().attribute("completedStatus", "COMPLETED"))
                .andExpect(model().attribute("cancelledStatus", "CANCELLED"));

        verify(inPersonOrderService).getAvailableProducts("artisan@test.com");
        verify(inPersonOrderService).getInPersonOrders("artisan@test.com", "ALL", 0, 10);
        verify(orderExpirationService).cancelExpiredInPersonOrders();
    }

    @Test
    void index_WhenStatusFilterProvided_ShouldReturnOrdersForThatStatus() throws Exception {
        Page<Order> pendingOrders = new PageImpl<>(List.of(order(1, "PENDING_PAYMENT")));

        when(inPersonOrderService.getAvailableProducts("artisan@test.com"))
                .thenReturn(List.of(product(101)));
        when(inPersonOrderService.getInPersonOrders("artisan@test.com", "PENDING_PAYMENT", 0, 10))
                .thenReturn(pendingOrders);

        mockMvc.perform(get("/artisan/in-person-order")
                        .param("status", "PENDING_PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/in-person-order"))
                .andExpect(model().attribute("selectedStatus", "PENDING_PAYMENT"))
                .andExpect(model().attribute("orders", pendingOrders));

        verify(inPersonOrderService).getInPersonOrders("artisan@test.com", "PENDING_PAYMENT", 0, 10);
    }

    @Test
    void index_WhenPageRequested_ShouldRequestThatPageFromService() throws Exception {
        Page<Order> secondPage = new PageImpl<>(List.of(order(11, "COMPLETED")));

        when(inPersonOrderService.getAvailableProducts("artisan@test.com"))
                .thenReturn(List.of(product(101)));
        when(inPersonOrderService.getInPersonOrders("artisan@test.com", "ALL", 1, 10))
                .thenReturn(secondPage);

        mockMvc.perform(get("/artisan/in-person-order")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("artisan/in-person-order"))
                .andExpect(model().attribute("orders", secondPage));

        verify(inPersonOrderService).getInPersonOrders("artisan@test.com", "ALL", 1, 10);
    }

    @Test
    void create_WhenRequestIsValid_ShouldRedirectWithSuccess() throws Exception {
        Order order = order(1, "PENDING_PAYMENT");
        whenCreateInPersonOrderSucceeds(order);

        mockMvc.perform(validCreatePost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("success"));

        verify(inPersonOrderService).createInPersonOrder(
                "artisan@test.com",
                101,
                "Walk-in Customer",
                "0900000000",
                "FPT HCM",
                "CASH",
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                "customer@test.com",
                "Pay at store"
        );
    }

    @Test
    void create_WhenProductUnavailable_ShouldRedirectWithError() throws Exception {
        whenCreateInPersonOrderFails("Product is unavailable");

        mockMvc.perform(validCreatePost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attribute("error", "Product is unavailable"));
    }

    @Test
    void create_WhenProductBelongsToAnotherArtisan_ShouldRedirectWithError() throws Exception {
        whenCreateInPersonOrderFails("Product does not belong to current artisan");

        mockMvc.perform(validCreatePost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attribute("error", "Product does not belong to current artisan"));
    }

    @Test
    void create_WhenRequiredCustomerInformationInvalid_ShouldRedirectWithError() throws Exception {
        when(inPersonOrderService.createInPersonOrder(
                eq("artisan@test.com"),
                eq(101),
                eq(""),
                eq(""),
                eq(""),
                eq("CASH"),
                eq(BigDecimal.ZERO),
                eq(BigDecimal.ZERO),
                eq(""),
                any()
        )).thenThrow(new RuntimeException("Customer information is required"));

        mockMvc.perform(post("/artisan/in-person-order")
                        .param("productId", "101")
                        .param("customerName", "")
                        .param("customerPhone", "")
                        .param("shippingAddress", "")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "0")
                        .param("shippingFee", "0")
                        .param("customerEmail", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attribute("error", "Customer information is required"));
    }

    @Test
    void create_WhenFeeIsNegative_ShouldRedirectWithError() throws Exception {
        when(inPersonOrderService.createInPersonOrder(
                eq("artisan@test.com"),
                eq(101),
                eq("Walk-in Customer"),
                eq("0900000000"),
                eq("FPT HCM"),
                eq("CASH"),
                eq(new BigDecimal("-1")),
                eq(BigDecimal.ZERO),
                eq("customer@test.com"),
                any()
        )).thenThrow(new RuntimeException("Fee must not be negative"));

        mockMvc.perform(post("/artisan/in-person-order")
                        .param("productId", "101")
                        .param("customerName", "Walk-in Customer")
                        .param("customerPhone", "0900000000")
                        .param("shippingAddress", "FPT HCM")
                        .param("paymentMethod", "CASH")
                        .param("craneFee", "-1")
                        .param("shippingFee", "0")
                        .param("customerEmail", "customer@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attribute("error", "Fee must not be negative"));
    }

    @Test
    void create_WhenPaymentMethodUnsupported_ShouldRedirectWithError() throws Exception {
        when(inPersonOrderService.createInPersonOrder(
                eq("artisan@test.com"),
                eq(101),
                eq("Walk-in Customer"),
                eq("0900000000"),
                eq("FPT HCM"),
                eq("BANK_TRANSFER"),
                eq(BigDecimal.ZERO),
                eq(BigDecimal.ZERO),
                eq("customer@test.com"),
                any()
        )).thenThrow(new RuntimeException("Unsupported payment method"));

        mockMvc.perform(post("/artisan/in-person-order")
                        .param("productId", "101")
                        .param("customerName", "Walk-in Customer")
                        .param("customerPhone", "0900000000")
                        .param("shippingAddress", "FPT HCM")
                        .param("paymentMethod", "BANK_TRANSFER")
                        .param("craneFee", "0")
                        .param("shippingFee", "0")
                        .param("customerEmail", "customer@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order/"))
                .andExpect(flash().attribute("error", "Unsupported payment method"));
    }

    @Test
    void update_WhenPendingOrderIsEditable_ShouldRedirectWithSuccess() throws Exception {
        Order order = order(1, "PENDING_PAYMENT");
        whenUpdateInPersonOrderSucceeds(order);

        mockMvc.perform(validUpdatePost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("success"));

        verify(inPersonOrderService).updateInPersonOrder(
                "artisan@test.com",
                1,
                "Updated Customer",
                "0911111111",
                "Updated Address",
                "VNPAY",
                new BigDecimal("200000"),
                new BigDecimal("75000"),
                "updated@test.com",
                "Updated notes"
        );
    }

    @Test
    void update_WhenOrderIsNonEditable_ShouldRedirectWithError() throws Exception {
        whenUpdateInPersonOrderFails("Order is not editable");

        mockMvc.perform(validUpdatePost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attribute("error", "Order is not editable"));
    }

    @Test
    void update_WhenOrderBelongsToAnotherArtisan_ShouldRedirectWithError() throws Exception {
        whenUpdateInPersonOrderFails("Order does not belong to current artisan");

        mockMvc.perform(validUpdatePost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attribute("error", "Order does not belong to current artisan"));
    }

    @Test
    void confirmPayment_WhenPendingOrderIsValid_ShouldRedirectWithSuccess() throws Exception {
        Order order = order(1, "COMPLETED");
        when(inPersonOrderService.confirmPayment("artisan@test.com", 1))
                .thenReturn(order);

        mockMvc.perform(post("/artisan/in-person-order/1/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("success"));

        verify(inPersonOrderService).confirmPayment("artisan@test.com", 1);
    }

    @Test
    void confirmPayment_WhenOrderStateIsInvalid_ShouldRedirectWithError() throws Exception {
        when(inPersonOrderService.confirmPayment("artisan@test.com", 1))
                .thenThrow(new RuntimeException("Order cannot be confirmed"));

        mockMvc.perform(post("/artisan/in-person-order/1/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attribute("error", "Order cannot be confirmed"));
    }

    @Test
    void confirmPayment_WhenOrderBelongsToAnotherArtisan_ShouldRedirectWithError() throws Exception {
        when(inPersonOrderService.confirmPayment("artisan@test.com", 1))
                .thenThrow(new RuntimeException("Order does not belong to current artisan"));

        mockMvc.perform(post("/artisan/in-person-order/1/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attribute("error", "Order does not belong to current artisan"));
    }

    @Test
    void cancel_WhenPendingOrderIsValid_ShouldRedirectWithSuccess() throws Exception {
        Order order = order(1, "CANCELLED");
        when(inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1, "Customer changed mind"))
                .thenReturn(order);

        mockMvc.perform(post("/artisan/in-person-order/1/cancel")
                        .param("reason", "Customer changed mind"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attributeExists("success"));

        verify(inPersonOrderService).cancelInPersonOrder("artisan@test.com", 1, "Customer changed mind");
    }

    @Test
    void cancel_WhenOrderAlreadyCompleted_ShouldRedirectWithError() throws Exception {
        when(inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1, "Too late"))
                .thenThrow(new RuntimeException("Order cannot be cancelled"));

        mockMvc.perform(post("/artisan/in-person-order/1/cancel")
                        .param("reason", "Too late"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attribute("error", "Order cannot be cancelled"));
    }

    @Test
    void cancel_WhenOrderBelongsToAnotherArtisan_ShouldRedirectWithError() throws Exception {
        when(inPersonOrderService.cancelInPersonOrder("artisan@test.com", 1, "Not mine"))
                .thenThrow(new RuntimeException("Order does not belong to current artisan"));

        mockMvc.perform(post("/artisan/in-person-order/1/cancel")
                        .param("reason", "Not mine"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan/in-person-order#walkInOrdersSection"))
                .andExpect(flash().attribute("error", "Order does not belong to current artisan"));
    }

    private void whenCreateInPersonOrderSucceeds(Order order) {
        when(inPersonOrderService.createInPersonOrder(
                "artisan@test.com",
                101,
                "Walk-in Customer",
                "0900000000",
                "FPT HCM",
                "CASH",
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                "customer@test.com",
                "Pay at store"
        )).thenReturn(order);
    }

    private void whenCreateInPersonOrderFails(String message) {
        when(inPersonOrderService.createInPersonOrder(
                "artisan@test.com",
                101,
                "Walk-in Customer",
                "0900000000",
                "FPT HCM",
                "CASH",
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                "customer@test.com",
                "Pay at store"
        )).thenThrow(new RuntimeException(message));
    }

    private void whenUpdateInPersonOrderSucceeds(Order order) {
        when(inPersonOrderService.updateInPersonOrder(
                "artisan@test.com",
                1,
                "Updated Customer",
                "0911111111",
                "Updated Address",
                "VNPAY",
                new BigDecimal("200000"),
                new BigDecimal("75000"),
                "updated@test.com",
                "Updated notes"
        )).thenReturn(order);
    }

    private void whenUpdateInPersonOrderFails(String message) {
        when(inPersonOrderService.updateInPersonOrder(
                "artisan@test.com",
                1,
                "Updated Customer",
                "0911111111",
                "Updated Address",
                "VNPAY",
                new BigDecimal("200000"),
                new BigDecimal("75000"),
                "updated@test.com",
                "Updated notes"
        )).thenThrow(new RuntimeException(message));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validCreatePost() {
        return post("/artisan/in-person-order")
                .param("productId", "101")
                .param("customerName", "Walk-in Customer")
                .param("customerPhone", "0900000000")
                .param("shippingAddress", "FPT HCM")
                .param("paymentMethod", "CASH")
                .param("craneFee", "100000")
                .param("shippingFee", "50000")
                .param("customerEmail", "customer@test.com")
                .param("notes", "Pay at store");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validUpdatePost() {
        return post("/artisan/in-person-order/1/update")
                .param("customerName", "Updated Customer")
                .param("customerPhone", "0911111111")
                .param("shippingAddress", "Updated Address")
                .param("paymentMethod", "VNPAY")
                .param("craneFee", "200000")
                .param("shippingFee", "75000")
                .param("customerEmail", "updated@test.com")
                .param("notes", "Updated notes");
    }

    private Product product(Integer productId) {
        return Product.builder()
                .productId(productId)
                .productName("Bonsai " + productId)
                .price(new BigDecimal("1500000"))
                .createdBy(User.builder().email("artisan@test.com").build())
                .build();
    }

    private Order order(Integer orderId, String status) {
        return Order.builder()
                .orderId(orderId)
                .orderCode("BSMS-100001")
                .orderStatus(status)
                .orderType("IN_PERSON")
                .build();
    }
}
