package com.example.bonsai_shop.moderator.service;

import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.exception.OrderNotFoundException;
import com.example.bonsai_shop.moderator.dto.OrderActionRequestDTO;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    // =========================================================================
    // Group 1: Common Validation & Resolution
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-001: executeAction - request null hoặc action null/rỗng")
    void executeAction_nullOrBlankAction_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(10).build();

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> orderActionService.executeAction("BSMS-100", null, moderator));
        assertEquals("Hành động không hợp lệ.", ex1.getMessage());

        OrderActionRequestDTO reqBlank = new OrderActionRequestDTO();
        reqBlank.setAction("   ");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> orderActionService.executeAction("BSMS-100", reqBlank, moderator));
        assertEquals("Hành động không hợp lệ.", ex2.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-002: executeAction - orderCode không tìm thấy trong CSDL")
    void executeAction_orderNotFound_throwsOrderNotFoundException() {
        User moderator = User.builder().userId(10).build();
        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("claim");

        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class,
                () -> orderActionService.executeAction("BSMS-999", req, moderator));
        assertEquals("Không tìm thấy đơn hàng: BSMS-999", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-003: executeAction - action không hợp lệ (unknown_action)")
    void executeAction_unknownAction_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("unknown_action");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderActionService.executeAction("BSMS-100", req, moderator));
        assertEquals("Hành động không hợp lệ: unknown_action", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-004: executeAction - action cancel bị cấm")
    void executeAction_cancelActionForbidden_throwsIllegalStateException() {
        User moderator = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("cancel");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, moderator));
        assertEquals("Hành động huỷ không còn hợp lệ trên trang chi tiết đơn hàng.", ex.getMessage());
    }

    // =========================================================================
    // Group 2: Action "claim"
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-005: executeAction - claim khi đơn đã có người nhận")
    void executeAction_claimAlreadyAssigned_throwsIllegalStateException() {
        User moderator1 = User.builder().userId(10).build();
        User moderator2 = User.builder().userId(11).build();

        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-100")
                .orderStatus("PENDING")
                .assignedTo(moderator1)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("claim");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, moderator2));
        assertEquals("Đơn hàng này đã có người nhận.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-006: executeAction - claim khi trạng thái đơn không phải PENDING")
    void executeAction_claimNonPendingOrder_throwsIllegalStateException() {
        User moderator = User.builder().userId(10).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-100")
                .orderStatus("DEPOSITED")
                .assignedTo(null)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("claim");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, moderator));
        assertEquals("Không thể tiếp nhận đơn hàng vì trạng thái đơn hàng không còn phù hợp.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-007: executeAction - claim thành công")
    void executeAction_claimSuccess() {
        User moderator = User.builder().userId(10).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-100")
                .orderStatus("PENDING")
                .assignedTo(null)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("claim");

        Map<String, Object> result = orderActionService.executeAction("BSMS-100", req, moderator);

        assertTrue((Boolean) result.get("success"));
        assertEquals("claim", result.get("action"));
        assertEquals("PENDING", result.get("newStatus"));
        assertEquals(moderator, order.getAssignedTo());
        assertNotNull(order.getAssignedAt());

        verify(orderHandlingRepository).save(any(OrderHandling.class));
        verify(orderRepository).save(order);
    }

    // =========================================================================
    // Group 3: Action "approve"
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-008: executeAction - approve bởi moderator không phụ trách")
    void executeAction_approveNotAssignedToMe_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        User mod11 = User.builder().userId(11).build();

        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("approve");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod11));
        assertEquals("Bạn không phụ trách đơn này.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-009: executeAction - approve khi status != PENDING")
    void executeAction_approveNonPending_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("DEPOSITED").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("approve");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Chỉ có thể duyệt đơn hàng đang chờ kiểm duyệt.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-010: executeAction - approve thất bại khi verifyOrder trả về false")
    void executeAction_approveVerifyOrderFailed_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));
        when(orderService.verifyOrder(eq("BSMS-100"), any(), any(), any(), eq(mod10))).thenReturn(false);

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("approve");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Không thể duyệt đơn hàng.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-011: executeAction - approve thành công")
    void executeAction_approveSuccess() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));
        when(orderService.verifyOrder(eq("BSMS-100"), any(), any(), any(), eq(mod10))).thenReturn(true);

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("approve");
        req.setCraneFee(new BigDecimal("200000"));

        Map<String, Object> result = orderActionService.executeAction("BSMS-100", req, mod10);

        assertTrue((Boolean) result.get("success"));
        assertEquals("approve", result.get("action"));
        assertEquals("PENDING_PAYMENT", result.get("newStatus"));
        verify(orderService).verifyOrder("BSMS-100", new BigDecimal("200000"), null, null, mod10);
    }

    // =========================================================================
    // Group 4: Action "reject"
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-012: executeAction - reject bởi moderator không phụ trách")
    void executeAction_rejectNotAssignedToMe_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        User mod11 = User.builder().userId(11).build();

        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("reject");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod11));
        assertEquals("Bạn không phụ trách đơn này.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-013: executeAction - reject thiếu lý do")
    void executeAction_rejectMissingReason_throwsIllegalArgumentException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("reject");
        req.setReason("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Lý do từ chối là bắt buộc.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-014: executeAction - reject thất bại khi rejectOrder trả về false")
    void executeAction_rejectFailed_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));
        when(orderService.rejectOrder("BSMS-100", "Hết hàng", mod10)).thenReturn(false);

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("reject");
        req.setReason("Hết hàng");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Không thể từ chối đơn hàng.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-015: executeAction - reject thành công")
    void executeAction_rejectSuccess() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));
        when(orderService.rejectOrder("BSMS-100", "Hết hàng", mod10)).thenReturn(true);

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("reject");
        req.setReason("Hết hàng");

        Map<String, Object> result = orderActionService.executeAction("BSMS-100", req, mod10);

        assertTrue((Boolean) result.get("success"));
        assertEquals("reject", result.get("action"));
        assertEquals("CANCELLED", result.get("newStatus"));
        verify(orderService).rejectOrder("BSMS-100", "Hết hàng", mod10);
    }

    // =========================================================================
    // Group 5: Action "return_inventory" / "unclaim"
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-016: executeAction - return_inventory khi status != PENDING")
    void executeAction_returnInventoryNonPending_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("DEPOSITED").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("return_inventory");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Không thể trả lại kho chung sau khi đơn hàng đã được duyệt.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-017: executeAction - return_inventory / unclaim thành công")
    void executeAction_returnInventorySuccess() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .assignedAt(LocalDateTime.now()).build();
        OrderHandling activeHandling = OrderHandling.builder().orderHandlingId(1).order(order).moderator(mod10)
                .isActive(true).build();

        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));
        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(100)).thenReturn(List.of(activeHandling));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("unclaim");

        Map<String, Object> result = orderActionService.executeAction("BSMS-100", req, mod10);

        assertTrue((Boolean) result.get("success"));
        assertEquals("return_inventory", result.get("action"));
        assertNull(order.getAssignedTo());
        assertNull(order.getAssignedAt());

        assertFalse(activeHandling.getIsActive());
        assertNotNull(activeHandling.getReleasedAt());

        verify(orderHandlingRepository).save(activeHandling);
        verify(orderRepository).save(order);
    }

    // =========================================================================
    // Group 6: Action "complete"
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-018: executeAction - complete khi status không phải PAID hoặc DEPOSITED")
    void executeAction_completeInvalidStatus_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("complete");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Chỉ hoàn thành đơn khi khách đã thanh toán.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-019: executeAction - complete cho đơn DEPOSITED (confirmRemainingPayment)")
    void executeAction_completeDepositedOrder() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("DEPOSITED").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("complete");
        req.setReason("Khách trả tiền còn lại");

        Map<String, Object> result = orderActionService.executeAction("BSMS-100", req, mod10);

        assertTrue((Boolean) result.get("success"));
        assertEquals("complete", result.get("action"));
        assertEquals("COMPLETED", result.get("newStatus"));

        verify(orderService).confirmRemainingPayment("BSMS-100", "Khách trả tiền còn lại", mod10);
        verify(orderService, never()).completePaidOrder(any(), any());
    }

    @Test
    @DisplayName("UT-UUT08-020: executeAction - complete cho đơn PAID (completePaidOrder)")
    void executeAction_completePaidOrder() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PAID").assignedTo(mod10).build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("complete");

        Map<String, Object> result = orderActionService.executeAction("BSMS-100", req, mod10);

        assertTrue((Boolean) result.get("success"));
        assertEquals("complete", result.get("action"));
        assertEquals("COMPLETED", result.get("newStatus"));

        verify(orderService).completePaidOrder("BSMS-100", mod10);
        verify(orderService, never()).confirmRemainingPayment(any(), any(), any());
    }

    // =========================================================================
    // Group 7: Action "customer_no_show"
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-021: executeAction - customer_no_show khi status != DEPOSITED")
    void executeAction_customerNoShowNonDeposited_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PAID").assignedTo(mod10).build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("customer_no_show");
        req.setReason("Khách không nhận");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Chỉ có thể ghi nhận khách không nhận hàng sau khi khách đã thanh toán tiền đặt cọc.",
                ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-022: executeAction - customer_no_show thiếu lý do")
    void executeAction_customerNoShowMissingReason_throwsIllegalArgumentException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("DEPOSITED").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("customer_no_show");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Lý do khách không nhận hàng là bắt buộc.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-023: executeAction - customer_no_show thành công")
    void executeAction_customerNoShowSuccess() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("DEPOSITED").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("customer_no_show");
        req.setReason("Không liên lạc được");

        Map<String, Object> result = orderActionService.executeAction("BSMS-100", req, mod10);

        assertTrue((Boolean) result.get("success"));
        assertEquals("customer_no_show", result.get("action"));
        assertEquals("CANCELLED", result.get("newStatus"));

        verify(orderService).markDepositedOrderCustomerNoShow("BSMS-100", "Không liên lạc được", mod10);
    }

    // =========================================================================
    // Group 8: Action "record_fault_refund" / "fault_refund"
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-024: executeAction - record_fault_refund khi status không hợp lệ")
    void executeAction_faultRefundInvalidStatus_throwsIllegalStateException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(mod10)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("record_fault_refund");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
        assertEquals("Chỉ có thể ghi nhận hoàn tiền khi đơn đã có khoản thanh toán thành công.", ex.getMessage());
    }

    @Test
    @DisplayName("UT-UUT08-025: executeAction - record_fault_refund thành công")
    void executeAction_faultRefundSuccess() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PAID").assignedTo(mod10).build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("fault_refund");
        req.setFaultParty("NURSERY");
        req.setReason("Cây hỏng");

        Map<String, Object> result = orderActionService.executeAction("BSMS-100", req, mod10);

        assertTrue((Boolean) result.get("success"));
        assertEquals("record_fault_refund", result.get("action"));
        assertEquals("CANCELLED", result.get("newStatus"));

        verify(orderService).recordFaultRefundAndCancel(
                eq("BSMS-100"),
                eq("NURSERY"),
                any(),
                eq("Cây hỏng"),
                any(),
                any(),
                any(),
                any(),
                eq(mod10));
    }

    // =========================================================================
    // Group 9: Dependency Failure
    // =========================================================================

    @Test
    @DisplayName("UT-UUT08-026: Dependency Failure - orderRepository.save ném DataAccessException")
    void executeAction_dependencyFailure_throwsException() {
        User mod10 = User.builder().userId(10).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-100").orderStatus("PENDING").assignedTo(null)
                .build();
        when(orderRepository.findByOrderCode("BSMS-100")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenThrow(new DataAccessException("DB Save Error") {
        });

        OrderActionRequestDTO req = new OrderActionRequestDTO();
        req.setAction("claim");

        assertThrows(DataAccessException.class,
                () -> orderActionService.executeAction("BSMS-100", req, mod10));
    }
}
