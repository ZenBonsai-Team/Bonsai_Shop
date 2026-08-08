package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.ModerationNotification;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.product.enums.PaymentMethod;
import com.example.bonsai_shop.product.enums.PaymentType;
import com.example.bonsai_shop.product.event.OrderPaidEvent;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServicePaymentTest {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private ApplicationEventPublisher eventPublisher;
    private MailService mailService;
    private ModerationNotificationRepository notificationRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        OrderLogRepository orderLogRepository = mock(OrderLogRepository.class);
        OrderHandlingRepository orderHandlingRepository = mock(OrderHandlingRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        mailService = mock(MailService.class);
        CartService cartService = mock(CartService.class);
        FinancialLedgerService financialLedgerService = mock(FinancialLedgerService.class);
        notificationRepository = mock(ModerationNotificationRepository.class);

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

    // =========================================================================
    // Group 1: processPaymentSuccess
    // =========================================================================

    @Test
    @DisplayName("UT-UUT04-001: processPaymentSuccess - Tra cứu đơn không tồn tại (order = null)")
    void processPaymentSuccess_orderNotFound_returnsFalse() {
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        boolean result = orderService.processPaymentSuccess("BSMS-999");

        assertFalse(result);
        verify(orderRepository, times(1)).findByOrderCode("BSMS-999");
        verify(paymentRepository, never()).findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT04-002: processPaymentSuccess - Đơn đã ở trạng thái PAID")
    void processPaymentSuccess_alreadyPaid_returnsTrue() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PAID").build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        boolean result = orderService.processPaymentSuccess("BSMS-123");

        assertTrue(result);
        verify(paymentRepository, never()).findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT04-003: processPaymentSuccess - Thanh toán cọc (DEPOSIT flow) thành công")
    void processPaymentSuccess_depositFlow_success() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment pendingPayment = Payment.builder()
                .paymentId(200)
                .order(order)
                .paymentType(PaymentType.DEPOSIT.name())
                .paymentStatus("PENDING")
                .amount(new BigDecimal("500000"))
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));

        boolean result = orderService.processPaymentSuccess("BSMS-123");

        assertTrue(result);
        assertThat(pendingPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(pendingPayment.getPaymentDate()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo("DEPOSITED");

        verify(paymentRepository, times(1)).save(pendingPayment);
        verify(orderRepository, times(1)).save(order);
        verify(mailService, times(1)).sendOrderDepositedEmail(order);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("UT-UUT04-004: processPaymentSuccess - Thanh toán cọc thành công nhưng gửi mail bị ngoại lệ")
    void processPaymentSuccess_depositFlow_emailException_stillReturnsTrue() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment pendingPayment = Payment.builder()
                .paymentId(200)
                .order(order)
                .paymentType(PaymentType.DEPOSIT.name())
                .paymentStatus("PENDING")
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));
        doThrow(new RuntimeException("SMTP connection failed")).when(mailService).sendOrderDepositedEmail(order);

        boolean result = orderService.processPaymentSuccess("BSMS-123");

        assertTrue(result);
        assertThat(pendingPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(order.getOrderStatus()).isEqualTo("DEPOSITED");
        verify(paymentRepository, times(1)).save(pendingPayment);
        verify(orderRepository, times(1)).save(order);
        verify(mailService, times(1)).sendOrderDepositedEmail(order);
    }

    @Test
    @DisplayName("UT-UUT04-005: processPaymentSuccess - Thanh toán FULL_PAYMENT thành công")
    void processPaymentSuccess_fullPaymentFlow_success() {
        User customer = User.builder().userId(10).email("customer@example.com").build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").customer(customer).build();
        Payment pendingPayment = Payment.builder()
                .paymentId(200)
                .order(order)
                .paymentType(PaymentType.FULL_PAYMENT.name())
                .paymentStatus("PENDING")
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));

        boolean result = orderService.processPaymentSuccess("BSMS-123");

        assertTrue(result);
        assertThat(pendingPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(order.getOrderStatus()).isEqualTo("PAID");

        verify(paymentRepository, times(1)).save(pendingPayment);
        verify(orderRepository, times(1)).save(order);
        verify(notificationRepository, times(1)).save(any(ModerationNotification.class));
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("UT-UUT04-006: processPaymentSuccess - Thanh toán thành công khi pendingPayment = null (fallback flow)")
    void processPaymentSuccess_noPendingPayment_fallbackToPaid() {
        User customer = User.builder().userId(10).email("customer@example.com").build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").customer(customer).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());

        boolean result = orderService.processPaymentSuccess("BSMS-123");

        assertTrue(result);
        assertThat(order.getOrderStatus()).isEqualTo("PAID");
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, times(1)).save(order);
        verify(notificationRepository, times(1)).save(any(ModerationNotification.class));
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("UT-UUT04-007: processPaymentSuccess - Thanh toán FULL_PAYMENT thành công nhưng notificationRepository.save bị lỗi")
    void processPaymentSuccess_fullPayment_notificationException_stillReturnsTrue() {
        User customer = User.builder().userId(10).email("customer@example.com").build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").customer(customer).build();
        Payment pendingPayment = Payment.builder()
                .paymentId(200)
                .order(order)
                .paymentType(PaymentType.FULL_PAYMENT.name())
                .paymentStatus("PENDING")
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));
        doThrow(new RuntimeException("DB notification error")).when(notificationRepository).save(any());

        boolean result = orderService.processPaymentSuccess("BSMS-123");

        assertTrue(result);
        assertThat(order.getOrderStatus()).isEqualTo("PAID");
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("UT-UUT04-008: processPaymentSuccess - Thanh toán FULL_PAYMENT cho đơn khách vãng lai (customer = null)")
    void processPaymentSuccess_guestCustomer_noNotificationSaved() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").customer(null).build();
        Payment pendingPayment = Payment.builder()
                .paymentId(200)
                .order(order)
                .paymentType(PaymentType.FULL_PAYMENT.name())
                .paymentStatus("PENDING")
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));

        boolean result = orderService.processPaymentSuccess("BSMS-123");

        assertTrue(result);
        assertThat(order.getOrderStatus()).isEqualTo("PAID");
        verify(notificationRepository, never()).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("UT-UUT04-009: Dependency Failure - paymentRepository.save ném DataAccessException trong processPaymentSuccess")
    void processPaymentSuccess_dependencyFailure_throwsException() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment pendingPayment = Payment.builder()
                .paymentId(200)
                .order(order)
                .paymentType(PaymentType.DEPOSIT.name())
                .paymentStatus("PENDING")
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenThrow(new DataAccessException("DB lock error") {});

        assertThrows(DataAccessException.class, () -> orderService.processPaymentSuccess("BSMS-123"));

        assertThat(pendingPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        verify(orderRepository, never()).save(any());
        verify(mailService, never()).sendOrderDepositedEmail(any());
    }

    // =========================================================================
    // Group 2: processPaymentFailure
    // =========================================================================

    @Test
    @DisplayName("UT-UUT04-010: processPaymentFailure - Tra cứu đơn không tồn tại (order = null)")
    void processPaymentFailure_orderNotFound_returnsFalse() {
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        boolean result = orderService.processPaymentFailure("BSMS-999", "24", "02", "VNPAY");

        assertFalse(result);
        verify(orderRepository, times(1)).findByOrderCode("BSMS-999");
        verify(paymentRepository, never()).findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(any(), any());
    }

    @Test
    @DisplayName("UT-UUT04-011: processPaymentFailure - pendingPayment = null và danh sách payments không có payment FAILED")
    void processPaymentFailure_noPending_noFailedPayment_returnsFalse() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").build();
        Payment p1 = Payment.builder().paymentId(1).paymentStatus("EXPIRED").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(p1));

        boolean result = orderService.processPaymentFailure("BSMS-123", "24", "02", "VNPAY");

        assertFalse(result);
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT04-012: processPaymentFailure - pendingPayment = null nhưng danh sách payments có payment FAILED")
    void processPaymentFailure_noPending_hasFailedPayment_returnsTrue() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").build();
        Payment p1 = Payment.builder().paymentId(1).paymentStatus("FAILED").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(p1));

        boolean result = orderService.processPaymentFailure("BSMS-123", "24", "02", "VNPAY");

        assertTrue(result);
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT04-013: processPaymentFailure - pendingPayment != null, order đang ở PENDING_PAYMENT")
    void processPaymentFailure_pendingPaymentExists_pendingPaymentStatus_success() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").notes("Ghi chú cũ").build();
        Payment pendingPayment = Payment.builder().paymentId(200).order(order).paymentStatus("PENDING").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));

        boolean result = orderService.processPaymentFailure("BSMS-123", "24", "02", "VNPAY");

        assertTrue(result);
        assertThat(pendingPayment.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(pendingPayment.getNotes()).contains("responseCode=24").contains("transactionStatus=02");
        assertThat(order.getNotes()).startsWith("Ghi chú cũ | Thanh toán VNPay thất bại.");

        verify(paymentRepository, times(1)).save(pendingPayment);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("UT-UUT04-014: processPaymentFailure - pendingPayment != null, order KHÔNG ở PENDING_PAYMENT (ví dụ PENDING)")
    void processPaymentFailure_pendingPaymentExists_otherOrderStatus_paymentSavedOnly() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").notes("Ghi chú").build();
        Payment pendingPayment = Payment.builder().paymentId(200).order(order).paymentStatus("PENDING").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));

        boolean result = orderService.processPaymentFailure("BSMS-123", "24", "02", "VNPAY");

        assertTrue(result);
        assertThat(pendingPayment.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(order.getNotes()).isEqualTo("Ghi chú");

        verify(paymentRepository, times(1)).save(pendingPayment);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT04-015: processPaymentFailure - buildVnPayFailureNote với các giá trị gateway null/blank")
    void processPaymentFailure_nullGatewayValues_formattedWithNA() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment pendingPayment = Payment.builder().paymentId(200).order(order).paymentStatus("PENDING").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));

        boolean result = orderService.processPaymentFailure("BSMS-123", null, "", "   ");

        assertTrue(result);
        assertThat(pendingPayment.getNotes())
                .contains("source=N/A")
                .contains("responseCode=N/A")
                .contains("transactionStatus=N/A");
    }

    @Test
    @DisplayName("UT-UUT04-016: processPaymentFailure - buildVnPayFailureNote dài quá 500 ký tự được cắt về 500 ký tự")
    void processPaymentFailure_longNote_truncatedTo500() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment pendingPayment = Payment.builder().paymentId(200).order(order).paymentStatus("PENDING").build();

        String longSource = "A".repeat(600);

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));

        boolean result = orderService.processPaymentFailure("BSMS-123", "24", "02", longSource);

        assertTrue(result);
        assertThat(pendingPayment.getNotes().length()).isEqualTo(500);
    }

    @Test
    @DisplayName("UT-UUT04-017: Dependency Failure - paymentRepository.save ném DataAccessException trong processPaymentFailure")
    void processPaymentFailure_dependencyFailure_throwsException() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment pendingPayment = Payment.builder().paymentId(200).order(order).paymentStatus("PENDING").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenThrow(new DataAccessException("DB timeout") {});

        assertThrows(DataAccessException.class, () -> orderService.processPaymentFailure("BSMS-123", "24", "02", "VNPAY"));

        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // Group 3: preparePendingVnPayPayment
    // =========================================================================

    @Test
    @DisplayName("UT-UUT04-018: preparePendingVnPayPayment - Đơn không tồn tại")
    void preparePendingVnPayPayment_orderNotFound_throwsException() {
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.preparePendingVnPayPayment("BSMS-999"));

        assertThat(ex.getMessage()).contains("Không tìm thấy đơn hàng: BSMS-999");
        verify(paymentRepository, never()).findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(any(), any());
    }

    @Test
    @DisplayName("UT-UUT04-019: preparePendingVnPayPayment - Đơn không ở trạng thái PENDING_PAYMENT")
    void preparePendingVnPayPayment_wrongOrderStatus_throwsException() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.preparePendingVnPayPayment("BSMS-123"));

        assertThat(ex.getMessage()).isEqualTo("Đơn hàng hiện không ở trạng thái chờ thanh toán.");
        verify(paymentRepository, never()).findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(any(), any());
    }

    @Test
    @DisplayName("UT-UUT04-020: preparePendingVnPayPayment - pendingPayment != null (tái sử dụng payment PENDING hiện có)")
    void preparePendingVnPayPayment_existingPendingPayment_returnsExisting() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment existingPending = Payment.builder().paymentId(200).order(order).paymentStatus("PENDING").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(existingPending));

        Payment result = orderService.preparePendingVnPayPayment("BSMS-123");

        assertThat(result).isSameAs(existingPending);
        verify(paymentRepository, never()).findByOrderOrderIdOrderByPaymentIdAsc(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT04-021: preparePendingVnPayPayment - tái tạo từ latestFailedVnPayPayment loại DEPOSIT")
    void preparePendingVnPayPayment_recreateFromLatestFailed_depositType() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment failed1 = Payment.builder()
                .paymentId(10)
                .paymentMethod(PaymentMethod.VNPAY.name())
                .paymentType(PaymentType.DEPOSIT.name())
                .paymentStatus("FAILED")
                .amount(new BigDecimal("500000"))
                .build();
        Payment savedPayment = Payment.builder().paymentId(300).paymentStatus("PENDING").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(failed1));
        when(paymentRepository.save(any())).thenReturn(savedPayment);

        Payment result = orderService.preparePendingVnPayPayment("BSMS-123");

        assertThat(result).isSameAs(savedPayment);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());

        Payment captured = captor.getValue();
        assertThat(captured.getOrder()).isEqualTo(order);
        assertThat(captured.getPaymentType()).isEqualTo(PaymentType.DEPOSIT.name());
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethod.DEPOSIT.name());
        assertThat(captured.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(captured.getAmount()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("UT-UUT04-022: preparePendingVnPayPayment - tái tạo từ latestFailedVnPayPayment loại FULL_PAYMENT")
    void preparePendingVnPayPayment_recreateFromLatestFailed_fullPaymentType() {
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();
        Payment failed1 = Payment.builder()
                .paymentId(10)
                .paymentMethod(PaymentMethod.VNPAY.name())
                .paymentType(PaymentType.FULL_PAYMENT.name())
                .paymentStatus("EXPIRED")
                .amount(new BigDecimal("1000000"))
                .build();
        Payment savedPayment = Payment.builder().paymentId(300).paymentStatus("PENDING").build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(failed1));
        when(paymentRepository.save(any())).thenReturn(savedPayment);

        Payment result = orderService.preparePendingVnPayPayment("BSMS-123");

        assertThat(result).isSameAs(savedPayment);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());

        Payment captured = captor.getValue();
        assertThat(captured.getPaymentType()).isEqualTo(PaymentType.FULL_PAYMENT.name());
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY.name());
        assertThat(captured.getAmount()).isEqualByComparingTo("1000000");
    }

    @Test
    @DisplayName("UT-UUT04-023: preparePendingVnPayPayment - không có payment cũ, đơn có depositAmount > 0")
    void preparePendingVnPayPayment_noFailedPayment_hasDepositAmount() {
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING_PAYMENT")
                .depositAmount(new BigDecimal("300000"))
                .totalAmount(new BigDecimal("1000000"))
                .build();
        Payment savedPayment = Payment.builder().paymentId(300).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(new ArrayList<>());
        when(paymentRepository.save(any())).thenReturn(savedPayment);

        Payment result = orderService.preparePendingVnPayPayment("BSMS-123");

        assertThat(result).isSameAs(savedPayment);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());

        Payment captured = captor.getValue();
        assertThat(captured.getPaymentType()).isEqualTo(PaymentType.DEPOSIT.name());
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethod.DEPOSIT.name());
        assertThat(captured.getAmount()).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("UT-UUT04-024: preparePendingVnPayPayment - không có payment cũ, depositAmount = null (FULL_PAYMENT flow)")
    void preparePendingVnPayPayment_noFailedPayment_nullDepositAmount_fullPayment() {
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING_PAYMENT")
                .depositAmount(null)
                .totalAmount(new BigDecimal("1200000"))
                .build();
        Payment savedPayment = Payment.builder().paymentId(300).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(new ArrayList<>());
        when(paymentRepository.save(any())).thenReturn(savedPayment);

        Payment result = orderService.preparePendingVnPayPayment("BSMS-123");

        assertThat(result).isSameAs(savedPayment);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());

        Payment captured = captor.getValue();
        assertThat(captured.getPaymentType()).isEqualTo(PaymentType.FULL_PAYMENT.name());
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY.name());
        assertThat(captured.getAmount()).isEqualByComparingTo("1200000");
    }

    @Test
    @DisplayName("UT-UUT04-025: preparePendingVnPayPayment - số tiền thanh toán quy đổi = null hoặc <= 0")
    void preparePendingVnPayPayment_invalidAmount_throwsException() {
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING_PAYMENT")
                .depositAmount(null)
                .totalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(new ArrayList<>());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.preparePendingVnPayPayment("BSMS-123"));

        assertThat(ex.getMessage()).isEqualTo("Số tiền thanh toán không hợp lệ.");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT04-026: Dependency Failure - paymentRepository.save ném DataAccessException trong preparePendingVnPayPayment")
    void preparePendingVnPayPayment_dependencyFailure_throwsException() {
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PENDING_PAYMENT")
                .depositAmount(new BigDecimal("500000"))
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(new ArrayList<>());
        when(paymentRepository.save(any())).thenThrow(new DataAccessException("DB constraint failure") {});

        assertThrows(DataAccessException.class, () -> orderService.preparePendingVnPayPayment("BSMS-123"));
    }
}
