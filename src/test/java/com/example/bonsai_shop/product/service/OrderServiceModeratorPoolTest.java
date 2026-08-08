package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceModeratorPoolTest {

    private OrderRepository orderRepository;
    private OrderHandlingRepository orderHandlingRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        OrderLogRepository orderLogRepository = mock(OrderLogRepository.class);
        orderHandlingRepository = mock(OrderHandlingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        MailService mailService = mock(MailService.class);
        CartService cartService = mock(CartService.class);
        FinancialLedgerService financialLedgerService = mock(FinancialLedgerService.class);
        ModerationNotificationRepository notificationRepository = mock(ModerationNotificationRepository.class);

        orderService = new OrderService(
                orderRepository,
                productRepository,
                orderLogRepository,
                orderHandlingRepository,
                paymentRepository,
                eventPublisher,
                mailService,
                cartService,
                financialLedgerService,
                notificationRepository
        );
    }

    @Test
    @DisplayName("UT-UUT02-001: Lọc danh sách đơn pool chưa ai nhận (getPoolOrders)")
    void getPoolOrders_success() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").build();
        Page<Order> mockPage = new PageImpl<>(List.of(order));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(orderRepository.searchOrdersPool(eq("BSMS"), any(Pageable.class))).thenReturn(mockPage);

        Page<Order> result = orderService.getPoolOrders("BSMS", "date_asc", 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository, times(1)).searchOrdersPool(eq("BSMS"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("orderDate")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("orderDate").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("UT-UUT02-002: Tra cứu đơn cá nhân của Moderator với status ALL (getMyOrders)")
    void getMyOrders_statusAll() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").build();
        Page<Order> mockPage = new PageImpl<>(List.of(order));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(orderRepository.searchMyOrders(eq(5), eq(null), eq(""), any(Pageable.class))).thenReturn(mockPage);

        Page<Order> result = orderService.getMyOrders(5, "", "ALL", "price_desc", 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository, times(1)).searchMyOrders(eq(5), eq(null), eq(""), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("totalAmount")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("totalAmount").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("UT-UUT02-003: Tra cứu đơn cá nhân với trạng thái cụ thể PENDING_PAYMENT (getMyOrders)")
    void getMyOrders_specificStatus() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").build();
        Page<Order> mockPage = new PageImpl<>(List.of(order));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(orderRepository.searchMyOrders(eq(5), eq(List.of("PENDING_PAYMENT")), eq(""), any(Pageable.class))).thenReturn(mockPage);

        Page<Order> result = orderService.getMyOrders(5, "", "PENDING_PAYMENT", "date_desc", 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository, times(1)).searchMyOrders(eq(5), eq(List.of("PENDING_PAYMENT")), eq(""), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("orderDate")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("orderDate").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("UT-UUT02-004: Lọc đơn hàng tổng hợp cho Moderator (getFilteredOrders)")
    void getFilteredOrders_success() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").build();
        Page<Order> mockPage = new PageImpl<>(List.of(order));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(orderRepository.searchOrdersForModerator(eq(List.of("PENDING")), eq("BSMS"), any(Pageable.class))).thenReturn(mockPage);

        Page<Order> result = orderService.getFilteredOrders("BSMS", "PENDING", "price_asc", 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository, times(1)).searchOrdersForModerator(eq(List.of("PENDING")), eq("BSMS"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("totalAmount")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("totalAmount").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("UT-UUT02-005: Thống kê KPI tổng thể hệ thống (getKPIs)")
    void getKPIs_success() {
        when(orderRepository.count()).thenReturn(10L);
        when(orderRepository.countByOrderStatus("PENDING")).thenReturn(3L);
        when(orderRepository.countByOrderStatus("PENDING_PAYMENT")).thenReturn(2L);
        when(orderRepository.countByOrderStatus("PAID")).thenReturn(4L);
        when(orderRepository.countByOrderStatus("CANCELLED")).thenReturn(1L);

        Map<String, Long> kpis = orderService.getKPIs();

        assertThat(kpis).isNotNull();
        assertThat(kpis.get("total")).isEqualTo(10L);
        assertThat(kpis.get("pending")).isEqualTo(3L);
        assertThat(kpis.get("approved")).isEqualTo(2L);
        assertThat(kpis.get("paid")).isEqualTo(4L);
        assertThat(kpis.get("cancelled")).isEqualTo(1L);
        assertThat(kpis.get("rejected")).isEqualTo(1L);

        verify(orderRepository, times(1)).count();
        verify(orderRepository, times(1)).countByOrderStatus("PENDING");
        verify(orderRepository, times(1)).countByOrderStatus("PENDING_PAYMENT");
        verify(orderRepository, times(1)).countByOrderStatus("PAID");
        verify(orderRepository, times(2)).countByOrderStatus("CANCELLED");
    }

    @Test
    @DisplayName("UT-UUT02-006: Thống kê KPI cá nhân Moderator (getModeratorPersonalKPIs)")
    void getModeratorPersonalKPIs_success() {
        when(orderRepository.countByAssignedToUserId(5)).thenReturn(5L);
        when(orderRepository.countByAssignedToUserIdAndOrderStatus(5, "PENDING")).thenReturn(2L);
        when(orderRepository.countByAssignedToUserIdAndOrderStatus(5, "PENDING_PAYMENT")).thenReturn(1L);
        when(orderRepository.countByAssignedToUserIdAndOrderStatus(5, "PAID")).thenReturn(1L);
        when(orderRepository.countByAssignedToUserIdAndOrderStatus(5, "CANCELLED")).thenReturn(1L);

        Map<String, Long> kpis = orderService.getModeratorPersonalKPIs(5);

        assertThat(kpis).isNotNull();
        assertThat(kpis.get("total")).isEqualTo(5L);
        assertThat(kpis.get("pending")).isEqualTo(2L);
        assertThat(kpis.get("approved")).isEqualTo(1L);
        assertThat(kpis.get("paid")).isEqualTo(1L);
        assertThat(kpis.get("rejected")).isEqualTo(1L);

        verify(orderRepository, times(1)).countByAssignedToUserId(5);
        verify(orderRepository, times(1)).countByAssignedToUserIdAndOrderStatus(5, "PENDING");
        verify(orderRepository, times(1)).countByAssignedToUserIdAndOrderStatus(5, "PENDING_PAYMENT");
        verify(orderRepository, times(1)).countByAssignedToUserIdAndOrderStatus(5, "PAID");
        verify(orderRepository, times(1)).countByAssignedToUserIdAndOrderStatus(5, "CANCELLED");
    }

    @Test
    @DisplayName("UT-UUT02-007: Tra cứu lịch sử nhật ký xử lý đơn (getOrderHandlingHistory)")
    void getOrderHandlingHistory_success() {
        OrderHandling h1 = OrderHandling.builder().orderHandlingId(1).build();
        OrderHandling h2 = OrderHandling.builder().orderHandlingId(2).build();

        when(orderHandlingRepository.findByOrderOrderIdOrderByHandledAtDesc(100)).thenReturn(List.of(h1, h2));

        List<OrderHandling> history = orderService.getOrderHandlingHistory(100);

        assertThat(history).isNotNull().hasSize(2);
        verify(orderHandlingRepository, times(1)).findByOrderOrderIdOrderByHandledAtDesc(100);
    }

    @Test
    @DisplayName("UT-UUT02-008: Moderator nhận tiếp quản đơn thành công (claimOrder)")
    void claimOrder_success() {
        User moderator = User.builder().userId(5).fullName("Moderator 5").build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(null).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        boolean result = orderService.claimOrder("BSMS-123", moderator);

        assertThat(result).isTrue();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getAssignedTo()).isEqualTo(moderator);
        assertThat(savedOrder.getAssignedAt()).isNotNull();

        ArgumentCaptor<OrderHandling> handlingCaptor = ArgumentCaptor.forClass(OrderHandling.class);
        verify(orderHandlingRepository, times(1)).save(handlingCaptor.capture());
        OrderHandling savedHandling = handlingCaptor.getValue();
        assertThat(savedHandling.getOrder().getOrderId()).isEqualTo(100);
        assertThat(savedHandling.getModerator().getUserId()).isEqualTo(5);
        assertThat(savedHandling.getIsActive()).isTrue();
        assertThat(savedHandling.getHandledAt()).isNotNull();
    }

    @Test
    @DisplayName("UT-UUT02-009: Claim đơn thất bại do Đơn hàng không tồn tại (claimOrder)")
    void claimOrder_notFound() {
        User moderator = User.builder().userId(5).build();

        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                orderService.claimOrder("BSMS-999", moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Đơn hàng không tồn tại!");
        verify(orderRepository, never()).save(any());
        verify(orderHandlingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT02-010: Claim đơn thất bại do Đơn đã được Moderator KHÁC tiếp quản (claimOrder)")
    void claimOrder_alreadyClaimedByOther() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator99).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                orderService.claimOrder("BSMS-123", moderator5)
        );

        assertThat(exception.getMessage()).isEqualTo("Đơn hàng đã được nhận bởi người khác!");
        verify(orderRepository, never()).save(any());
        verify(orderHandlingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT02-011: Claim đơn thất bại khi Đơn đã được CHÍNH Moderator hiện tại tiếp quản trước đó (claimOrder)")
    void claimOrder_alreadyClaimedBySameModerator() {
        User moderator5 = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator5).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                orderService.claimOrder("BSMS-123", moderator5)
        );

        assertThat(exception.getMessage()).isEqualTo("Đơn hàng đã được nhận bởi người khác!");
        verify(orderRepository, never()).save(any());
        verify(orderHandlingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT02-012: Claim đơn thất bại do Đơn không ở trạng thái PENDING (claimOrder)")
    void claimOrder_notPendingStatus() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(null).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                orderService.claimOrder("BSMS-123", moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Chỉ được phép nhận đơn hàng đang chờ duyệt!");
        verify(orderRepository, never()).save(any());
        verify(orderHandlingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT02-013: Moderator trả đơn về pool thành công với bản ghi OrderHandling active (unclaimOrder)")
    void unclaimOrder_successWithActiveHandling() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order targetOrder = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator5).build();
        Order otherOrder = Order.builder().orderId(200).orderCode("BSMS-456").orderStatus("PENDING").assignedTo(moderator5).build();

        OrderHandling h1 = OrderHandling.builder().orderHandlingId(1).order(targetOrder).moderator(moderator5).isActive(true).build();
        OrderHandling h2 = OrderHandling.builder().orderHandlingId(2).order(otherOrder).moderator(moderator5).isActive(true).build();
        OrderHandling h3 = OrderHandling.builder().orderHandlingId(3).order(targetOrder).moderator(moderator99).isActive(true).build();
        OrderHandling h4 = OrderHandling.builder().orderHandlingId(4).order(targetOrder).moderator(moderator5).isActive(false).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(targetOrder));
        when(orderHandlingRepository.findAll()).thenReturn(List.of(h1, h2, h3, h4));

        boolean result = orderService.unclaimOrder("BSMS-123", moderator5);

        assertThat(result).isTrue();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getAssignedTo()).isNull();
        assertThat(orderCaptor.getValue().getAssignedAt()).isNull();

        ArgumentCaptor<OrderHandling> handlingCaptor = ArgumentCaptor.forClass(OrderHandling.class);
        verify(orderHandlingRepository, times(1)).save(handlingCaptor.capture());
        OrderHandling savedHandling = handlingCaptor.getValue();
        assertThat(savedHandling.getOrderHandlingId()).isEqualTo(1);
        assertThat(savedHandling.getIsActive()).isFalse();
        assertThat(savedHandling.getReleasedAt()).isNotNull();

        assertThat(h2.getIsActive()).isTrue();
        assertThat(h3.getIsActive()).isTrue();
        assertThat(h4.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("UT-UUT02-014: Unclaim đơn thất bại do Đơn hàng không tồn tại (unclaimOrder)")
    void unclaimOrder_notFound() {
        User moderator = User.builder().userId(5).build();

        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                orderService.unclaimOrder("BSMS-999", moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Đơn hàng không tồn tại!");
        verify(orderRepository, never()).save(any());
        verify(orderHandlingRepository, never()).findAll();
    }

    @Test
    @DisplayName("UT-UUT02-015: Unclaim đơn thất bại do Đơn chưa được ai tiếp quản (unclaimOrder)")
    void unclaimOrder_unassigned() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(null).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                orderService.unclaimOrder("BSMS-123", moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Bạn không sở hữu quyền xử lý đơn hàng này!");
        verify(orderRepository, never()).save(any());
        verify(orderHandlingRepository, never()).findAll();
    }

    @Test
    @DisplayName("UT-UUT02-016: Unclaim đơn thất bại do Moderator không sở hữu đơn (unclaimOrder)")
    void unclaimOrder_assignedToOther() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator99).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                orderService.unclaimOrder("BSMS-123", moderator5)
        );

        assertThat(exception.getMessage()).isEqualTo("Bạn không sở hữu quyền xử lý đơn hàng này!");
        verify(orderRepository, never()).save(any());
        verify(orderHandlingRepository, never()).findAll();
    }

    @Test
    @DisplayName("UT-UUT02-017: Moderator trả đơn về pool thành công khi KHÔNG CÓ bản ghi OrderHandling active (unclaimOrder)")
    void unclaimOrder_successWithoutActiveHandling() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(orderHandlingRepository.findAll()).thenReturn(List.of());

        boolean result = orderService.unclaimOrder("BSMS-123", moderator);

        assertThat(result).isTrue();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getAssignedTo()).isNull();
        verify(orderHandlingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT02-018: Unclaim thất bại do Đơn không ở trạng thái PENDING (unclaimOrder)")
    void unclaimOrder_notPendingStatus() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                orderService.unclaimOrder("BSMS-123", moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Chỉ được phép trả lại đơn hàng chưa duyệt!");
        verify(orderRepository, never()).save(any());
        verify(orderHandlingRepository, never()).findAll();
    }
}
