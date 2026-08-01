package com.example.bonsai_shop.moderator.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.moderator.dto.OrderActionRequestDTO;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderActionServiceTest {

    private OrderRepository orderRepository;
    private OrderHandlingRepository orderHandlingRepository;
    private OrderService orderService;
    private OrderActionService orderActionService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderHandlingRepository = mock(OrderHandlingRepository.class);
        orderService = mock(OrderService.class);
        orderActionService = new OrderActionService(orderRepository, orderHandlingRepository, orderService);
    }

    @Test
    void assignedPendingOrderCanReturnToPoolAndClosesActiveHandling() {
        User moderator = moderator(10);
        Order order = assignedOrder("BSMS-001", "PENDING", moderator);
        OrderHandling activeHandling = OrderHandling.builder()
                .order(order)
                .moderator(moderator)
                .handledAt(LocalDateTime.now().minusHours(1))
                .isActive(true)
                .build();

        when(orderRepository.findByOrderCode("BSMS-001")).thenReturn(Optional.of(order));
        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId()))
                .thenReturn(List.of(activeHandling));

        OrderActionRequestDTO request = request("return_inventory");
        Map<String, Object> result = orderActionService.executeAction("BSMS-001", request, moderator);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("newStatus")).isEqualTo("PENDING");
        assertThat(order.getAssignedTo()).isNull();
        assertThat(order.getAssignedAt()).isNull();
        assertThat(activeHandling.getIsActive()).isFalse();
        assertThat(activeHandling.getReleasedAt()).isNotNull();
        verify(orderRepository).save(order);
        verify(orderHandlingRepository).save(activeHandling);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING_PAYMENT", "PAID", "COMPLETED", "CANCELLED"})
    void nonPendingOrdersCannotReturnToPool(String status) {
        User moderator = moderator(10);
        Order order = assignedOrder("BSMS-002", status, moderator);

        when(orderRepository.findByOrderCode("BSMS-002")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderActionService.executeAction("BSMS-002", request("return_inventory"), moderator))
                .isInstanceOf(IllegalStateException.class);

        assertThat(order.getAssignedTo()).isEqualTo(moderator);
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderHandlingRepository, never()).save(any(OrderHandling.class));
    }

    @Test
    void rejectRequiresReasonBeforeCallingBackendWorkflow() {
        User moderator = moderator(12);
        Order order = assignedOrder("BSMS-REJECT", "PENDING", moderator);

        when(orderRepository.findByOrderCode("BSMS-REJECT")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderActionService.executeAction("BSMS-REJECT", request("reject"), moderator))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderService, never()).rejectOrder(any(), any(), any());
    }

    @Test
    void approvedUnassignedOrderCannotBeClaimedManually() {
        User moderator = moderator(11);
        Order order = Order.builder()
                .orderId(101)
                .orderCode("BSMS-003")
                .orderStatus("PENDING_PAYMENT")
                .build();

        when(orderRepository.findByOrderCode("BSMS-003")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderActionService.executeAction("BSMS-003", request("claim"), moderator))
                .isInstanceOf(IllegalStateException.class);

        assertThat(order.getAssignedTo()).isNull();
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderHandlingRepository, never()).save(any(OrderHandling.class));
    }

    @Test
    void returnedPendingOrderCanBeClaimedByAnotherModeratorExactlyOnce() {
        User secondModerator = moderator(22);
        Order returnedOrder = Order.builder()
                .orderId(102)
                .orderCode("BSMS-004")
                .orderStatus("PENDING")
                .assignedTo(null)
                .assignedAt(null)
                .build();

        when(orderRepository.findByOrderCode("BSMS-004")).thenReturn(Optional.of(returnedOrder));

        orderActionService.executeAction("BSMS-004", request("claim"), secondModerator);

        assertThat(returnedOrder.getAssignedTo()).isEqualTo(secondModerator);
        assertThat(returnedOrder.getAssignedAt()).isNotNull();

        ArgumentCaptor<OrderHandling> handlingCaptor = ArgumentCaptor.forClass(OrderHandling.class);
        verify(orderHandlingRepository).save(handlingCaptor.capture());
        assertThat(handlingCaptor.getValue().getOrder()).isEqualTo(returnedOrder);
        assertThat(handlingCaptor.getValue().getModerator()).isEqualTo(secondModerator);
        assertThat(handlingCaptor.getValue().getIsActive()).isTrue();
    }

    @Test
    void depositedCompletionCreatesRemainingPaymentThroughOrderService() {
        User moderator = moderator(30);
        Order order = assignedOrder("BSMS-005", "DEPOSITED", moderator);

        when(orderRepository.findByOrderCode("BSMS-005")).thenReturn(Optional.of(order));
        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId()))
                .thenReturn(List.of());
        OrderActionRequestDTO request = request("complete");
        request.setReason("Collected remaining cash");

        Map<String, Object> result = orderActionService.executeAction("BSMS-005", request, moderator);

        assertThat(result.get("newStatus")).isEqualTo("COMPLETED");
        verify(orderService).confirmRemainingPayment("BSMS-005", "Collected remaining cash", moderator);
        verify(orderService, never()).completePaidOrder("BSMS-005", moderator);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void customerNoShowActionRequiresDepositedAssignedOrder() {
        User moderator = moderator(31);
        Order order = assignedOrder("BSMS-006", "DEPOSITED", moderator);
        OrderActionRequestDTO request = request("customer_no_show");
        request.setReason("Customer refused delivery");

        when(orderRepository.findByOrderCode("BSMS-006")).thenReturn(Optional.of(order));
        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId()))
                .thenReturn(List.of());

        Map<String, Object> result = orderActionService.executeAction("BSMS-006", request, moderator);

        assertThat(result.get("newStatus")).isEqualTo("CANCELLED");
        verify(orderService).markDepositedOrderCustomerNoShow("BSMS-006", "Customer refused delivery", moderator);
    }

    @Test
    void faultRefundActionCallsDedicatedManualRefundWorkflow() {
        User moderator = moderator(32);
        Order order = assignedOrder("BSMS-007", "PAID", moderator);
        OrderActionRequestDTO request = request("record_fault_refund");
        request.setFaultParty("NURSERY");
        request.setRefundAmount(new BigDecimal("500000"));
        request.setReason("Tree damaged before handover");
        request.setEvidenceNote("Photo evidence");
        request.setExternalReference("REF-001");
        request.setCustomerKeepsTree(false);
        request.setProductResolution("RETURNED_AND_RESELLABLE");

        when(orderRepository.findByOrderCode("BSMS-007")).thenReturn(Optional.of(order));
        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(order.getOrderId()))
                .thenReturn(List.of());

        Map<String, Object> result = orderActionService.executeAction("BSMS-007", request, moderator);

        assertThat(result.get("newStatus")).isEqualTo("CANCELLED");
        verify(orderService).recordFaultRefundAndCancel(
                "BSMS-007",
                "NURSERY",
                new BigDecimal("500000"),
                "Tree damaged before handover",
                "Photo evidence",
                "REF-001",
                false,
                "RETURNED_AND_RESELLABLE",
                moderator
        );
    }

    private OrderActionRequestDTO request(String action) {
        OrderActionRequestDTO request = new OrderActionRequestDTO();
        request.setAction(action);
        return request;
    }

    private Order assignedOrder(String orderCode, String status, User moderator) {
        return Order.builder()
                .orderId(100)
                .orderCode(orderCode)
                .orderStatus(status)
                .assignedTo(moderator)
                .assignedAt(LocalDateTime.now().minusMinutes(30))
                .build();
    }

    private User moderator(Integer userId) {
        return User.builder()
                .userId(userId)
                .username("moderator-" + userId)
                .build();
    }
}
