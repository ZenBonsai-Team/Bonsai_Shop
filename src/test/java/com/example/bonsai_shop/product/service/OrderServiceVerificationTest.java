package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderHandling;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.event.OrderRejectedEvent;
import com.example.bonsai_shop.product.event.OrderVerifiedEvent;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.product.enums.PaymentMethod;
import com.example.bonsai_shop.product.enums.PaymentType;
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
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceVerificationTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private OrderLogRepository orderLogRepository;
    private OrderHandlingRepository orderHandlingRepository;
    private PaymentRepository paymentRepository;
    private ApplicationEventPublisher eventPublisher;
    private MailService mailService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        orderLogRepository = mock(OrderLogRepository.class);
        orderHandlingRepository = mock(OrderHandlingRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        mailService = mock(MailService.class);
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
    @DisplayName("UT-UUT03-001: Uỷ quyền từ verifyOrder (4 tham số) sang 5 tham số với depositAmount = null trên đơn FULL_PAYMENT")
    void verifyOrder_4param_delegation_fullPayment() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        Payment fullPayment = Payment.builder().paymentId(200).paymentType(PaymentType.FULL_PAYMENT.name()).amount(new BigDecimal("1000000")).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(fullPayment));
        when(orderHandlingRepository.findAll()).thenReturn(List.of());

        boolean result = orderService.verifyOrder("BSMS-123", new BigDecimal("100000"), new BigDecimal("50000"), moderator);

        assertThat(result).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("1150000"));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(new BigDecimal("1150000"));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("PENDING_PAYMENT");

        ArgumentCaptor<OrderLog> logCaptor = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogRepository, times(1)).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("VERIFY");
        assertThat(logCaptor.getValue().getFromStatus()).isEqualTo("PENDING");
        assertThat(logCaptor.getValue().getToStatus()).isEqualTo("PENDING_PAYMENT");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(OrderVerifiedEvent.class);
        OrderVerifiedEvent verifiedEvent = (OrderVerifiedEvent) eventCaptor.getValue();
        assertThat(verifiedEvent.getOrder().getOrderId()).isEqualTo(100);

        verify(mailService, never()).sendOrderApprovedEmail(any());
    }

    @Test
    @DisplayName("UT-UUT03-002: Uỷ quyền từ verifyOrder (4 tham số) sang 5 tham số với depositAmount = null trên đơn DEPOSIT gây lỗi thiếu cọc")
    void verifyOrder_4param_delegation_deposit_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        Payment depositPayment = Payment.builder().paymentId(200).paymentType(PaymentType.DEPOSIT.name()).amount(new BigDecimal("500000")).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", new BigDecimal("100000"), new BigDecimal("50000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Vui lòng nhập số tiền đặt cọc.");
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verify(orderLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("UT-UUT03-003: Duyệt đơn thành công cho luồng đặt cọc (DEPOSIT flow) với Payment PENDING cũ và Fixture 4 OrderHandling")
    void verifyOrder_depositFlow_existingPayment_success() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order100 = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator5).build();
        order100.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        Order order200 = Order.builder().orderId(200).orderCode("BSMS-456").build();

        Payment depositPayment = Payment.builder().paymentId(200).paymentType("DEPOSIT").paymentStatus("PENDING").amount(new BigDecimal("500000")).build();

        OrderHandling h1 = OrderHandling.builder().orderHandlingId(1).order(order100).moderator(moderator5).isActive(true).build();
        OrderHandling h2 = OrderHandling.builder().orderHandlingId(2).order(order200).moderator(moderator5).isActive(true).build();
        OrderHandling h3 = OrderHandling.builder().orderHandlingId(3).order(order100).moderator(moderator99).isActive(true).build();
        OrderHandling h4 = OrderHandling.builder().orderHandlingId(4).order(order100).moderator(moderator5).isActive(false).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order100));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));
        when(orderHandlingRepository.findAll()).thenReturn(List.of(h1, h2, h3, h4));

        boolean result = orderService.verifyOrder("BSMS-123", new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("300000"), moderator5);

        assertThat(result).isTrue();
        assertThat(order100.getCraneFee()).isEqualTo(new BigDecimal("100000"));
        assertThat(order100.getShippingFee()).isEqualTo(new BigDecimal("50000"));
        assertThat(order100.getTotalAmount()).isEqualTo(new BigDecimal("1150000"));
        assertThat(order100.getDepositAmount()).isEqualTo(new BigDecimal("300000"));
        assertThat(order100.getOrderStatus()).isEqualTo("PENDING_PAYMENT");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPaymentId()).isEqualTo(200);
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(new BigDecimal("300000"));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("PENDING_PAYMENT");

        ArgumentCaptor<OrderLog> logCaptor = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogRepository, times(1)).save(logCaptor.capture());
        OrderLog log = logCaptor.getValue();
        assertThat(log.getOrder().getOrderId()).isEqualTo(100);
        assertThat(log.getActionBy().getUserId()).isEqualTo(5);
        assertThat(log.getActionType()).isEqualTo("VERIFY");
        assertThat(log.getFromStatus()).isEqualTo("PENDING");
        assertThat(log.getToStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(log.getActionAt()).isNotNull();

        ArgumentCaptor<OrderHandling> handlingCaptor = ArgumentCaptor.forClass(OrderHandling.class);
        verify(orderHandlingRepository, times(1)).save(handlingCaptor.capture());
        assertThat(handlingCaptor.getValue().getOrderHandlingId()).isEqualTo(1);
        assertThat(handlingCaptor.getValue().getIsActive()).isFalse();
        assertThat(handlingCaptor.getValue().getReleasedAt()).isNotNull();

        assertThat(h2.getIsActive()).isTrue();
        assertThat(h3.getIsActive()).isTrue();
        assertThat(h4.getIsActive()).isFalse();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(((OrderVerifiedEvent) eventCaptor.getValue()).getOrder().getOrderId()).isEqualTo(100);

        verify(mailService, never()).sendOrderApprovedEmail(any());
    }

    @Test
    @DisplayName("UT-UUT03-004: Duyệt đơn cọc thành công khi CHƯA CÓ Payment DEPOSIT (tạo mới Payment từ paymentMethod = COD)")
    void verifyOrder_depositFlow_createNewPayment_success() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        Payment initialPayment = Payment.builder().paymentId(200).paymentType("INITIAL").paymentMethod("COD").build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(initialPayment));
        when(orderHandlingRepository.findAll()).thenReturn(List.of());

        boolean result = orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500000"), moderator);

        assertThat(result).isTrue();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment newPayment = paymentCaptor.getValue();
        assertThat(newPayment.getPaymentType()).isEqualTo(PaymentType.DEPOSIT.name());
        assertThat(newPayment.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY.name());
        assertThat(newPayment.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(newPayment.getAmount()).isEqualTo(new BigDecimal("500000"));

        verify(orderRepository, times(1)).save(any());
        verify(orderLogRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(OrderVerifiedEvent.class));
        verify(mailService, never()).sendOrderApprovedEmail(any());
    }

    @Test
    @DisplayName("UT-UUT03-005: Duyệt đơn thành công cho luồng thanh toán 100% (FULL_PAYMENT flow)")
    void verifyOrder_fullPaymentFlow_success() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        Payment fullPayment = Payment.builder().paymentId(200).paymentType("FULL_PAYMENT").amount(new BigDecimal("1000000")).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(fullPayment));
        when(orderHandlingRepository.findAll()).thenReturn(List.of());

        boolean result = orderService.verifyOrder("BSMS-123", new BigDecimal("200000"), new BigDecimal("100000"), null, moderator);

        assertThat(result).isTrue();
        assertThat(order.getDepositAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getCraneFee()).isEqualTo(new BigDecimal("200000"));
        assertThat(order.getShippingFee()).isEqualTo(new BigDecimal("100000"));
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("1300000"));
        assertThat(order.getOrderStatus()).isEqualTo("PENDING_PAYMENT");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(new BigDecimal("1300000"));

        verify(orderRepository, times(1)).save(any());
        verify(orderLogRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(OrderVerifiedEvent.class));
        verify(mailService, never()).sendOrderApprovedEmail(any());
    }

    @Test
    @DisplayName("UT-UUT03-006: Duyệt đơn thất bại do OrderRepository mock trả về Optional.empty()")
    void verifyOrder_notFound_returnsFalse() {
        User moderator = User.builder().userId(5).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-999")).thenReturn(Optional.empty());

        boolean result = orderService.verifyOrder("BSMS-999", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500000"), moderator);

        assertThat(result).isFalse();
        verify(paymentRepository, never()).findByOrderOrderIdOrderByPaymentIdAsc(any());
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("UT-UUT03-007: Duyệt đơn thất bại do Đơn hàng không ở trạng thái PENDING")
    void verifyOrder_notPendingStatus_returnsFalse() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        boolean result = orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500000"), moderator);

        assertThat(result).isFalse();
        assertThat(order.getOrderStatus()).isEqualTo("DEPOSITED");
        verify(paymentRepository, never()).findByOrderOrderIdOrderByPaymentIdAsc(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-008: Duyệt đơn thất bại do Đơn hàng chưa có Moderator tiếp quản (assignedTo = null)")
    void verifyOrder_unassigned_throwsSecurityException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(null).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        SecurityException exception = assertThrows(SecurityException.class, () ->
                orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Bạn không có quyền duyệt đơn hàng này!");
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-009: Duyệt đơn thất bại do Moderator gọi hàm không sở hữu đơn")
    void verifyOrder_moderatorNotOwner_throwsSecurityException() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator99).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        SecurityException exception = assertThrows(SecurityException.class, () ->
                orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500000"), moderator5)
        );

        assertThat(exception.getMessage()).isEqualTo("Bạn không có quyền duyệt đơn hàng này!");
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-010: Duyệt đơn cọc thất bại do depositAmount = null")
    void verifyOrder_depositNull_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        Payment depositPayment = Payment.builder().paymentId(200).paymentType("DEPOSIT").build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, BigDecimal.ZERO, null, moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Vui lòng nhập số tiền đặt cọc.");
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-011: Duyệt đơn cọc thất bại do depositAmount <= 0")
    void verifyOrder_depositZeroOrNegative_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        Payment depositPayment = Payment.builder().paymentId(200).paymentType("DEPOSIT").build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Vui lòng nhập số tiền đặt cọc.");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-012: Duyệt đơn cọc thất bại do depositAmount không phải số nguyên (scale > 0)")
    void verifyOrder_depositDecimal_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));
        Payment depositPayment = Payment.builder().paymentId(200).paymentType("DEPOSIT").build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500000.50"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Tiền đặt cọc phải là số nguyên.");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-013: Duyệt đơn cọc thất bại do depositAmount vượt quá tổng giá trị cây (treePrice)")
    void verifyOrder_depositExceedsTotal_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));
        Payment depositPayment = Payment.builder().paymentId(200).paymentType("DEPOSIT").build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));

        // treePrice = 1,000,000; craneFee = 500,000; shippingFee = 200,000 -> newTotal = 1,700,000
        // deposit = 1,200,000 (< newTotal nhưng > treePrice) -> Phải ném ngoại lệ
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", new BigDecimal("500000"), new BigDecimal("200000"), new BigDecimal("1200000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Số tiền đặt cọc không được vượt quá tổng giá trị cây.");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-014: Duyệt đơn thất bại do phí cẩu (craneFee) bị âm (< 0)")
    void verifyOrder_craneFeeNegative_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", new BigDecimal("-50000"), BigDecimal.ZERO, new BigDecimal("300000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Phí cẩu không được âm.");
        verify(paymentRepository, never()).findByOrderOrderIdOrderByPaymentIdAsc(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-014B: Duyệt đơn thất bại do phí cẩu (craneFee) vượt quá 200.000.000 VNĐ")
    void verifyOrder_craneFeeExceedsMax_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", new BigDecimal("250000000"), BigDecimal.ZERO, new BigDecimal("300000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Phí cẩu không được vượt quá 200.000.000 VNĐ.");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-015: Duyệt đơn thất bại do phí vận chuyển (shippingFee) bị âm (< 0)")
    void verifyOrder_shippingFeeNegative_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, new BigDecimal("-30000"), new BigDecimal("300000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Phí vận chuyển không được âm.");
        verify(paymentRepository, never()).findByOrderOrderIdOrderByPaymentIdAsc(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-015B: Duyệt đơn thất bại do phí vận chuyển (shippingFee) vượt quá 200.000.000 VNĐ")
    void verifyOrder_shippingFeeExceedsMax_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, new BigDecimal("200000001"), new BigDecimal("300000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Phí vận chuyển không được vượt quá 200.000.000 VNĐ.");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-016: Duyệt đơn thành công khi craneFee và shippingFee truyền vào là null (tự động chuẩn hóa về 0)")
    void verifyOrder_feesNull_normalizedToZero_success() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        Payment fullPayment = Payment.builder().paymentId(200).paymentType("FULL_PAYMENT").amount(new BigDecimal("1000000")).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(fullPayment));
        when(orderHandlingRepository.findAll()).thenReturn(List.of());

        boolean result = orderService.verifyOrder("BSMS-123", null, null, null, moderator);

        assertThat(result).isTrue();
        assertThat(order.getCraneFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getShippingFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("1000000"));
        assertThat(order.getOrderStatus()).isEqualTo("PENDING_PAYMENT");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getCraneFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(orderCaptor.getValue().getShippingFee()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("UT-UUT03-017: Duyệt đơn thất bại do phí cẩu (craneFee) không phải số nguyên (scale > 0)")
    void verifyOrder_craneFeeDecimal_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", new BigDecimal("100000.75"), BigDecimal.ZERO, new BigDecimal("300000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Phí cẩu phải là số nguyên.");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-018: Duyệt đơn thất bại do phí vận chuyển (shippingFee) không phải số nguyên (scale > 0)")
    void verifyOrder_shippingFeeDecimal_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.verifyOrder("BSMS-123", BigDecimal.ZERO, new BigDecimal("50000.25"), new BigDecimal("300000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Phí vận chuyển phải là số nguyên.");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-019: Từ chối đơn hàng (rejectOrder) thành công với nhiều sản phẩm và Fixture 4 OrderHandling")
    void rejectOrder_multipleProducts_success() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Product p1 = Product.builder().productId(10).productStatus("RESERVED").build();
        Product p2 = Product.builder().productId(20).productStatus("RESERVED").build();

        Order order100 = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator5).build();
        order100.setOrderDetails(List.of(OrderDetail.builder().product(p1).build(), OrderDetail.builder().product(p2).build()));
        Order order200 = Order.builder().orderId(200).orderCode("BSMS-456").build();

        OrderHandling h1 = OrderHandling.builder().orderHandlingId(1).order(order100).moderator(moderator5).isActive(true).build();
        OrderHandling h2 = OrderHandling.builder().orderHandlingId(2).order(order200).moderator(moderator5).isActive(true).build();
        OrderHandling h3 = OrderHandling.builder().orderHandlingId(3).order(order100).moderator(moderator99).isActive(true).build();
        OrderHandling h4 = OrderHandling.builder().orderHandlingId(4).order(order100).moderator(moderator5).isActive(false).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order100));
        when(orderHandlingRepository.findAll()).thenReturn(List.of(h1, h2, h3, h4));

        boolean result = orderService.rejectOrder("BSMS-123", "Thông tin giao hàng không khớp", moderator5);

        assertThat(result).isTrue();
        assertThat(order100.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(order100.getNotes()).isEqualTo("Hủy đơn với lý do: Thông tin giao hàng không khớp");
        assertThat(p1.getProductStatus()).isEqualTo("AVAILABLE");
        assertThat(p2.getProductStatus()).isEqualTo("AVAILABLE");

        verify(orderRepository, times(1)).save(any());
        verify(productRepository, times(2)).save(any());

        ArgumentCaptor<OrderLog> logCaptor = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogRepository, times(1)).save(logCaptor.capture());
        OrderLog log = logCaptor.getValue();
        assertThat(log.getOrder().getOrderId()).isEqualTo(100);
        assertThat(log.getActionBy().getUserId()).isEqualTo(5);
        assertThat(log.getActionType()).isEqualTo("REJECT");
        assertThat(log.getFromStatus()).isEqualTo("PENDING");
        assertThat(log.getToStatus()).isEqualTo("CANCELLED");
        assertThat(log.getActionAt()).isNotNull();

        ArgumentCaptor<OrderHandling> handlingCaptor = ArgumentCaptor.forClass(OrderHandling.class);
        verify(orderHandlingRepository, times(1)).save(handlingCaptor.capture());
        assertThat(handlingCaptor.getValue().getOrderHandlingId()).isEqualTo(1);
        assertThat(handlingCaptor.getValue().getIsActive()).isFalse();
        assertThat(handlingCaptor.getValue().getReleasedAt()).isNotNull();

        assertThat(h2.getIsActive()).isTrue();
        assertThat(h3.getIsActive()).isTrue();
        assertThat(h4.getIsActive()).isFalse();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        OrderRejectedEvent event = (OrderRejectedEvent) eventCaptor.getValue();
        assertThat(event.getOrder().getOrderId()).isEqualTo(100);
        assertThat(event.getReason()).isEqualTo("Thông tin giao hàng không khớp");

        verify(mailService, never()).sendOrderRejectedEmail(any(), any());
    }

    @Test
    @DisplayName("UT-UUT03-020: Từ chối đơn thất bại do Lý do từ chối = null (Short-circuit check)")
    void rejectOrder_reasonNull_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.rejectOrder("BSMS-123", null, moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Lý do từ chối là bắt buộc.");
        verify(orderRepository, never()).findByOrderCodeWithDetails(any());
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("UT-UUT03-021: Từ chối đơn thất bại do Lý do từ chối rỗng / trắng (reason.isBlank())")
    void rejectOrder_reasonBlank_throwsIllegalArgumentException() {
        User moderator = User.builder().userId(5).build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.rejectOrder("BSMS-123", "   ", moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Lý do từ chối là bắt buộc.");
        verify(orderRepository, never()).findByOrderCodeWithDetails(any());
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-022: Từ chối đơn thất bại do OrderRepository mock trả về Optional.empty()")
    void rejectOrder_notFound_returnsFalse() {
        User moderator = User.builder().userId(5).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-999")).thenReturn(Optional.empty());

        boolean result = orderService.rejectOrder("BSMS-999", "Khách đổi ý", moderator);

        assertThat(result).isFalse();
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("UT-UUT03-023: Từ chối đơn thất bại do Đơn không ở trạng thái PENDING")
    void rejectOrder_notPendingStatus_returnsFalse() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PAID").assignedTo(moderator).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        boolean result = orderService.rejectOrder("BSMS-123", "Không liên lạc được", moderator);

        assertThat(result).isFalse();
        assertThat(order.getOrderStatus()).isEqualTo("PAID");
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-024: Từ chối đơn thất bại do Đơn hàng chưa có Moderator tiếp quản (assignedTo = null)")
    void rejectOrder_unassigned_throwsSecurityException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(null).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        SecurityException exception = assertThrows(SecurityException.class, () ->
                orderService.rejectOrder("BSMS-123", "Địa chỉ không hợp lệ", moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Bạn không có quyền hủy đơn hàng này!");
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-025: Từ chối đơn thất bại do Moderator gọi hàm không sở hữu đơn")
    void rejectOrder_moderatorNotOwner_throwsSecurityException() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator99).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        SecurityException exception = assertThrows(SecurityException.class, () ->
                orderService.rejectOrder("BSMS-123", "Nghi ngờ giả mạo", moderator5)
        );

        assertThat(exception.getMessage()).isEqualTo("Bạn không có quyền hủy đơn hàng này!");
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT03-026: Từ chối đơn khi orderDetails chứa OrderDetail có product = null")
    void rejectOrder_productInDetailNull_skipsNullProduct_success() {
        User moderator = User.builder().userId(5).build();
        Product validProduct = Product.builder().productId(10).productStatus("RESERVED").build();

        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(
                OrderDetail.builder().product(null).build(),
                OrderDetail.builder().product(validProduct).build()
        ));

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(orderHandlingRepository.findAll()).thenReturn(List.of());

        boolean result = orderService.rejectOrder("BSMS-123", "Hủy đơn phần dư", moderator);

        assertThat(result).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(validProduct.getProductStatus()).isEqualTo("AVAILABLE");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getProductId()).isEqualTo(10);
        assertThat(productCaptor.getValue().getProductStatus()).isEqualTo("AVAILABLE");

        verify(productRepository, never()).save(null);
    }

    @Test
    @DisplayName("UT-UUT03-027: Từ chối đơn thành công khi orderDetails = null")
    void rejectOrder_orderDetailsNull_success() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(null);

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(orderHandlingRepository.findAll()).thenReturn(List.of());

        boolean result = orderService.rejectOrder("BSMS-123", "Hủy đơn kỹ thuật", moderator);

        assertThat(result).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        verify(orderRepository, times(1)).save(any());
        verify(productRepository, never()).save(any());
        verify(orderLogRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(OrderRejectedEvent.class));
    }

    @Test
    @DisplayName("UT-UUT03-028: Từ chối đơn phát sinh NullPointerException khi orderDetails chứa phần tử OrderDetail bằng null")
    void rejectOrder_detailNullElement_throwsNullPointerException() {
        User moderator = User.builder().userId(5).build();
        Product validProduct = Product.builder().productId(10).productStatus("RESERVED").build();

        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        List<OrderDetail> details = new ArrayList<>();
        details.add(null); // Element itself is null
        details.add(OrderDetail.builder().product(validProduct).build());
        order.setOrderDetails(details);

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));

        assertThrows(NullPointerException.class, () ->
                orderService.rejectOrder("BSMS-123", "Hủy đơn lỗi", moderator)
        );

        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        verify(orderRepository, times(1)).save(any());
        verify(productRepository, never()).save(any());
        verify(orderLogRepository, never()).save(any());
        verify(orderHandlingRepository, never()).findAll();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("UT-UUT03-029: Xử lý ngoại lệ thất bại tại Dependency Persistence (orderRepository.save phát sinh DataAccessException trong verifyOrder)")
    void verifyOrder_orderRepositorySaveThrowsDataAccessException_propagatesException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().priceAtPurchase(new BigDecimal("1000000")).quantity(1).build()));

        Payment depositPayment = Payment.builder().paymentId(200).paymentType("DEPOSIT").paymentStatus("PENDING").amount(new BigDecimal("500000")).build();

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(depositPayment));
        when(orderRepository.save(any(Order.class))).thenThrow(new DataAccessException("DB Write Timeout") {});

        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                orderService.verifyOrder("BSMS-123", new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("300000"), moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("DB Write Timeout");
        assertThat(order.getCraneFee()).isEqualTo(new BigDecimal("100000"));
        assertThat(order.getShippingFee()).isEqualTo(new BigDecimal("50000"));
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("1150000"));
        assertThat(order.getDepositAmount()).isEqualTo(new BigDecimal("300000"));
        assertThat(order.getOrderStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(depositPayment.getAmount()).isEqualTo(new BigDecimal("300000"));

        verify(paymentRepository, times(1)).save(depositPayment);
        verify(orderRepository, times(1)).save(order);

        verify(orderLogRepository, never()).save(any());
        verify(orderHandlingRepository, never()).findAll();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("UT-UUT03-030: Xử lý ngoại lệ thất bại tại Dependency Persistence (productRepository.save phát sinh DataAccessException trong rejectOrder)")
    void rejectOrder_productRepositorySaveThrowsDataAccessException_propagatesException() {
        User moderator = User.builder().userId(5).build();
        Product p1 = Product.builder().productId(10).productStatus("RESERVED").build();
        Product p2 = Product.builder().productId(20).productStatus("RESERVED").build();

        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        order.setOrderDetails(List.of(OrderDetail.builder().product(p1).build(), OrderDetail.builder().product(p2).build()));

        when(orderRepository.findByOrderCodeWithDetails("BSMS-123")).thenReturn(Optional.of(order));
        when(productRepository.save(argThat(p -> p != null && p.getProductId().equals(10))))
                .thenThrow(new DataAccessException("Lock wait timeout") {});

        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                orderService.rejectOrder("BSMS-123", "Hủy đơn", moderator)
        );

        assertThat(exception.getMessage()).isEqualTo("Lock wait timeout");
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(order.getNotes()).isEqualTo("Hủy đơn với lý do: Hủy đơn");
        assertThat(p1.getProductStatus()).isEqualTo("AVAILABLE");
        assertThat(p2.getProductStatus()).isEqualTo("RESERVED"); // Not reached

        verify(orderRepository, times(1)).save(order);
        verify(productRepository, times(1)).save(argThat(p -> p != null && p.getProductId().equals(10)));
        verify(productRepository, never()).save(argThat(p -> p != null && p.getProductId().equals(20)));

        verify(orderLogRepository, never()).save(any());
        verify(orderHandlingRepository, never()).findAll();
        verify(eventPublisher, never()).publishEvent(any());
    }
}
