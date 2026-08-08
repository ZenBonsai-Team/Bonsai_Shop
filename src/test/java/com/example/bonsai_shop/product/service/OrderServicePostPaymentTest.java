package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.product.enums.PaymentMethod;
import com.example.bonsai_shop.product.enums.PaymentType;
import com.example.bonsai_shop.product.event.OrderPaidEvent;
import com.example.bonsai_shop.product.event.OrderRejectedEvent;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServicePostPaymentTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private OrderLogRepository orderLogRepository;
    private PaymentRepository paymentRepository;
    private ApplicationEventPublisher eventPublisher;
    private FinancialLedgerService financialLedgerService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        orderLogRepository = mock(OrderLogRepository.class);
        OrderHandlingRepository orderHandlingRepository = mock(OrderHandlingRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        MailService mailService = mock(MailService.class);
        CartService cartService = mock(CartService.class);
        financialLedgerService = mock(FinancialLedgerService.class);
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

    // =========================================================================
    // Group 1: confirmRemainingPayment
    // =========================================================================

    @Test
    @DisplayName("UT-UUT05-001: confirmRemainingPayment - Đơn không tồn tại")
    void confirmRemainingPayment_orderNotFound_throwsException() {
        User moderator = User.builder().userId(5).build();
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.confirmRemainingPayment("BSMS-999", "Notes", moderator));

        assertThat(ex.getMessage()).contains("Không tìm thấy đơn hàng: BSMS-999");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT05-002: confirmRemainingPayment - Đơn không ở trạng thái DEPOSITED")
    void confirmRemainingPayment_notDeposited_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.confirmRemainingPayment("BSMS-123", "Notes", moderator));

        assertThat(ex.getMessage()).contains("Đơn hàng phải ở trạng thái ĐÃ ĐẶT CỌC (DEPOSITED)");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT05-003: confirmRemainingPayment - Moderator không phụ trách đơn")
    void confirmRemainingPayment_wrongModerator_throwsException() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator99).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.confirmRemainingPayment("BSMS-123", "Notes", moderator5));

        assertThat(ex.getMessage()).isEqualTo("Bạn không phụ trách đơn này.");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT05-004: confirmRemainingPayment - Xác nhận thanh toán phần còn lại thành công")
    void confirmRemainingPayment_success() {
        User moderator = User.builder().userId(5).build();
        Product prod1 = Product.builder().productId(10).productStatus("RESERVED").build();
        OrderDetail detail1 = OrderDetail.builder().product(prod1).priceAtPurchase(new BigDecimal("1000000")).quantity(1).build();

        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator)
                .totalAmount(new BigDecimal("1000000"))
                .orderDetails(List.of(detail1))
                .build();

        Payment depositPayment = Payment.builder()
                .paymentId(1)
                .paymentType(PaymentType.DEPOSIT.name())
                .paymentStatus("SUCCESS")
                .amount(new BigDecimal("300000"))
                .build();

        FinancialLedger ledger = FinancialLedger.builder().financialLedgerId(1000).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdAndPaymentType(100, PaymentType.DEPOSIT.name()))
                .thenReturn(List.of(depositPayment));
        when(financialLedgerService.recordCompletedOrderRevenueIfAbsent(eq(order), eq(moderator), any(LocalDateTime.class)))
                .thenReturn(ledger);

        boolean result = orderService.confirmRemainingPayment("BSMS-123", "Thanh toán đủ", moderator);

        assertTrue(result);
        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(order.getCompletedAt()).isNotNull();
        assertThat(prod1.getProductStatus()).isEqualTo("SOLD");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment remainingPayment = paymentCaptor.getValue();
        assertThat(remainingPayment.getPaymentType()).isEqualTo(PaymentType.REMAINING_PAYMENT.name());
        assertThat(remainingPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CASH.name());
        assertThat(remainingPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(remainingPayment.getAmount()).isEqualByComparingTo("700000");

        verify(orderRepository, times(1)).save(order);
        verify(productRepository, times(1)).save(prod1);
        verify(orderLogRepository, times(2)).save(any(OrderLog.class)); // 1 ledger log + 1 remaining payment log
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("UT-UUT05-005: confirmRemainingPayment - depositPaid lớn hơn totalAmount (remainingAmount reset về 0)")
    void confirmRemainingPayment_depositPaidExceedsTotal_remainingAmountZero() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator)
                .totalAmount(new BigDecimal("500000"))
                .build();

        Payment depositPayment = Payment.builder()
                .paymentId(1)
                .paymentType(PaymentType.DEPOSIT.name())
                .paymentStatus("SUCCESS")
                .amount(new BigDecimal("600000"))
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdAndPaymentType(100, PaymentType.DEPOSIT.name()))
                .thenReturn(List.of(depositPayment));

        boolean result = orderService.confirmRemainingPayment("BSMS-123", "Xác nhận", moderator);

        assertTrue(result);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("UT-UUT05-006: confirmRemainingPayment - resolveTreePrice fallback khi orderDetails = null")
    void confirmRemainingPayment_nullOrderDetails_resolveTreePriceFallback() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator)
                .totalAmount(new BigDecimal("1200000"))
                .craneFee(new BigDecimal("100000"))
                .shippingFee(new BigDecimal("100000"))
                .orderDetails(null)
                .build();

        Payment depositPayment = Payment.builder()
                .paymentId(1)
                .paymentType(PaymentType.DEPOSIT.name())
                .paymentStatus("SUCCESS")
                .amount(new BigDecimal("500000"))
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdAndPaymentType(100, PaymentType.DEPOSIT.name()))
                .thenReturn(List.of(depositPayment));

        boolean result = orderService.confirmRemainingPayment("BSMS-123", "Xác nhận", moderator);

        assertTrue(result);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("700000"); // 1200000 - 500000
    }

    @Test
    @DisplayName("UT-UUT05-007: confirmRemainingPayment - financialLedgerService.recordCompletedOrderRevenueIfAbsent trả về null")
    void confirmRemainingPayment_nullLedger_skipsLedgerLog() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator)
                .totalAmount(new BigDecimal("1000000"))
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdAndPaymentType(100, PaymentType.DEPOSIT.name()))
                .thenReturn(new ArrayList<>());
        when(financialLedgerService.recordCompletedOrderRevenueIfAbsent(any(), any(), any()))
                .thenReturn(null);

        boolean result = orderService.confirmRemainingPayment("BSMS-123", "Xác nhận", moderator);

        assertTrue(result);
        // CHỈ tạo 1 OrderLog cho REMAINING_PAYMENT_CONFIRMED (bỏ qua ledger log)
        verify(orderLogRepository, times(1)).save(any(OrderLog.class));
    }

    @Test
    @DisplayName("UT-UUT05-008: Dependency Failure - paymentRepository.save ném DataAccessException trong confirmRemainingPayment")
    void confirmRemainingPayment_dependencyFailure_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator)
                .totalAmount(new BigDecimal("1000000"))
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdAndPaymentType(100, PaymentType.DEPOSIT.name()))
                .thenReturn(new ArrayList<>());
        when(paymentRepository.save(any())).thenThrow(new DataAccessException("DB Save error") {});

        assertThrows(DataAccessException.class,
                () -> orderService.confirmRemainingPayment("BSMS-123", "Xác nhận", moderator));

        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // Group 2: markDepositedOrderCustomerNoShow
    // =========================================================================

    @Test
    @DisplayName("UT-UUT05-009: markDepositedOrderCustomerNoShow - Đơn không tồn tại")
    void markDepositedOrderCustomerNoShow_orderNotFound_throwsException() {
        User moderator = User.builder().userId(5).build();
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.markDepositedOrderCustomerNoShow("BSMS-999", "Khách hủy", moderator));

        assertThat(ex.getMessage()).contains("Không tìm thấy đơn hàng: BSMS-999");
    }

    @Test
    @DisplayName("UT-UUT05-010: markDepositedOrderCustomerNoShow - Moderator không phụ trách đơn")
    void markDepositedOrderCustomerNoShow_wrongModerator_throwsException() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator99).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.markDepositedOrderCustomerNoShow("BSMS-123", "Khách hủy", moderator5));

        assertThat(ex.getMessage()).isEqualTo("Bạn không phụ trách đơn này.");
    }

    @Test
    @DisplayName("UT-UUT05-011: markDepositedOrderCustomerNoShow - Đơn không ở trạng thái DEPOSITED")
    void markDepositedOrderCustomerNoShow_notDeposited_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PAID").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.markDepositedOrderCustomerNoShow("BSMS-123", "Khách hủy", moderator));

        assertThat(ex.getMessage()).contains("Chỉ có thể ghi nhận khách không nhận hàng");
    }

    @Test
    @DisplayName("UT-UUT05-012: markDepositedOrderCustomerNoShow - notes (lý do) = null hoặc rỗng")
    void markDepositedOrderCustomerNoShow_emptyNotes_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.markDepositedOrderCustomerNoShow("BSMS-123", "   ", moderator));

        assertThat(ex.getMessage()).isEqualTo("Lý do là bắt buộc.");
    }

    @Test
    @DisplayName("UT-UUT05-013: markDepositedOrderCustomerNoShow - Ghi nhận khách không nhận hàng thành công")
    void markDepositedOrderCustomerNoShow_success() {
        User moderator = User.builder().userId(5).build();
        Product prod1 = Product.builder().productId(10).productStatus("RESERVED").build();
        OrderDetail detail1 = OrderDetail.builder().product(prod1).build();

        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator)
                .notes("Ghi chú ban đầu")
                .orderDetails(List.of(detail1))
                .build();

        Payment depositPayment = Payment.builder().paymentId(1).amount(new BigDecimal("500000")).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.requireSuccessfulDepositPayment(order)).thenReturn(depositPayment);

        boolean result = orderService.markDepositedOrderCustomerNoShow("BSMS-123", "Khách đổi ý không lấy", moderator);

        assertTrue(result);
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(order.getNotes()).contains("Khách đổi ý không lấy Tiền cọc được ghi nhận giữ lại do lỗi khách hàng.");
        assertThat(prod1.getProductStatus()).isEqualTo("AVAILABLE");

        verify(financialLedgerService, times(1))
                .recordForfeitedDepositIncome(order, depositPayment, new BigDecimal("500000"), "Khách đổi ý không lấy", moderator);
        verify(orderRepository, times(1)).save(order);
        verify(productRepository, times(1)).save(prod1);
        verify(orderLogRepository, times(1)).save(any(OrderLog.class));
        verify(eventPublisher, times(1)).publishEvent(any(OrderRejectedEvent.class));
    }

    @Test
    @DisplayName("UT-UUT05-014: Dependency Failure - requireSuccessfulDepositPayment ném IllegalStateException trong markDepositedOrderCustomerNoShow")
    void markDepositedOrderCustomerNoShow_depositPaymentNotFound_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.requireSuccessfulDepositPayment(order))
                .thenThrow(new IllegalStateException("Không tìm thấy khoản đặt cọc thành công"));

        assertThrows(IllegalStateException.class,
                () -> orderService.markDepositedOrderCustomerNoShow("BSMS-123", "Lý do", moderator));

        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // Group 3: recordFaultRefundAndCancel
    // =========================================================================

    @Test
    @DisplayName("UT-UUT05-015: recordFaultRefundAndCancel - Đơn không tồn tại")
    void recordFaultRefundAndCancel_orderNotFound_throwsException() {
        User moderator = User.builder().userId(5).build();
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.recordFaultRefundAndCancel("BSMS-999", "NURSERY", null, "Cây hỏng", "Anh minh hoa", "REF123", false, "RETURNED_AND_RESELLABLE", moderator));

        assertThat(ex.getMessage()).contains("Không tìm thấy đơn hàng: BSMS-999");
    }

    @Test
    @DisplayName("UT-UUT05-016: recordFaultRefundAndCancel - Moderator không phụ trách đơn")
    void recordFaultRefundAndCancel_wrongModerator_throwsException() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator99).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.recordFaultRefundAndCancel("BSMS-123", "NURSERY", null, "Lỗi nhà vườn", null, null, false, null, moderator5));

        assertThat(ex.getMessage()).isEqualTo("Bạn không phụ trách đơn này.");
    }

    @Test
    @DisplayName("UT-UUT05-017: recordFaultRefundAndCancel - Đơn ở trạng thái PENDING không hợp lệ")
    void recordFaultRefundAndCancel_invalidOrderStatus_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.recordFaultRefundAndCancel("BSMS-123", "NURSERY", null, "Lỗi nhà vườn", null, null, false, null, moderator));

        assertThat(ex.getMessage()).contains("Chỉ có thể ghi nhận hoàn tiền khi đơn đã có khoản thanh toán thành công.");
    }

    @Test
    @DisplayName("UT-UUT05-018: recordFaultRefundAndCancel - faultPartyValue null hoặc không hợp lệ")
    void recordFaultRefundAndCancel_invalidFaultPartyEnum_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.recordFaultRefundAndCancel("BSMS-123", "INVALID_PARTY", null, "Lỗi", null, null, false, null, moderator));

        assertThat(ex.getMessage()).contains("Bên chịu lỗi không hợp lệ: INVALID_PARTY");
    }

    @Test
    @DisplayName("UT-UUT05-019: recordFaultRefundAndCancel - faultParty không phải NURSERY hoặc DELIVERY (ví dụ CUSTOMER)")
    void recordFaultRefundAndCancel_disallowedFaultParty_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.recordFaultRefundAndCancel("BSMS-123", "CUSTOMER", null, "Lỗi khách", null, null, false, null, moderator));

        assertThat(ex.getMessage()).isEqualTo("Bên chịu trách nhiệm phải là nhà vườn hoặc quá trình vận chuyển.");
    }

    @Test
    @DisplayName("UT-UUT05-020: recordFaultRefundAndCancel - calculateRefundableCash = null hoặc <= 0")
    void recordFaultRefundAndCancel_noRefundableCash_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(BigDecimal.ZERO);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.recordFaultRefundAndCancel("BSMS-123", "NURSERY", null, "Lỗi nhà vườn", null, null, false, null, moderator));

        assertThat(ex.getMessage()).contains("Đơn hàng này không có khoản thanh toán thành công nào còn có thể hoàn tiền.");
    }

    @Test
    @DisplayName("UT-UUT05-021: recordFaultRefundAndCancel - reason = null hoặc rỗng")
    void recordFaultRefundAndCancel_emptyReason_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("1000000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.recordFaultRefundAndCancel("BSMS-123", "NURSERY", null, "   ", null, null, false, null, moderator));

        assertThat(ex.getMessage()).isEqualTo("Lý do là bắt buộc.");
    }

    @Test
    @DisplayName("UT-UUT05-022: recordFaultRefundAndCancel - Thành công cho lỗi nhà vườn (NURSERY)")
    void recordFaultRefundAndCancel_success_nurseryFault() {
        User moderator = User.builder().userId(5).build();
        Product prod1 = Product.builder().productId(10).productStatus("RESERVED").build();
        OrderDetail detail1 = OrderDetail.builder().product(prod1).build();

        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("DEPOSITED")
                .assignedTo(moderator)
                .orderDetails(List.of(detail1))
                .build();

        FinancialLedger refundLedger = FinancialLedger.builder()
                .financialLedgerId(500)
                .ledgerType(FinancialLedgerType.FULL_REFUND)
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("500000"));
        when(financialLedgerService.recordManualFaultRefund(eq(order), eq(FaultParty.NURSERY), eq(new BigDecimal("500000")), eq("Cây hỏng do nhà vườn"), eq("Hinh anh"), eq("REF001"), eq(moderator)))
                .thenReturn(refundLedger);

        boolean result = orderService.recordFaultRefundAndCancel("BSMS-123", "NURSERY", null, "Cây hỏng do nhà vườn", "Hinh anh", "REF001", false, null, moderator);

        assertTrue(result);
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(order.getNotes()).contains("Cây hỏng do nhà vườn Hoàn tiền 100% chỉ được ghi nhận thủ công");
        assertThat(prod1.getProductStatus()).isEqualTo("AVAILABLE");

        verify(orderRepository, times(1)).save(order);
        verify(productRepository, times(1)).save(prod1);
        verify(orderLogRepository, times(1)).save(any(OrderLog.class));
        verify(eventPublisher, times(1)).publishEvent(any(OrderRejectedEvent.class));
    }

    @Test
    @DisplayName("UT-UUT05-023: recordFaultRefundAndCancel - Thành công cho lỗi vận chuyển (DELIVERY)")
    void recordFaultRefundAndCancel_success_deliveryFault() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PAID").assignedTo(moderator).build();
        FinancialLedger refundLedger = FinancialLedger.builder().ledgerType(FinancialLedgerType.FULL_REFUND).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("1000000"));
        when(financialLedgerService.recordManualFaultRefund(any(), eq(FaultParty.DELIVERY), any(), any(), any(), any(), any()))
                .thenReturn(refundLedger);

        boolean result = orderService.recordFaultRefundAndCancel("BSMS-123", "DELIVERY", null, "Vỡ chậu khi vận chuyển", null, null, false, null, moderator);

        assertTrue(result);
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("UT-UUT05-024: Dependency Failure - recordManualFaultRefund ném DataAccessException trong recordFaultRefundAndCancel")
    void recordFaultRefundAndCancel_dependencyFailure_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("500000"));
        when(financialLedgerService.recordManualFaultRefund(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataAccessException("Ledger save error") {});

        assertThrows(DataAccessException.class,
                () -> orderService.recordFaultRefundAndCancel("BSMS-123", "NURSERY", null, "Lỗi nhà vườn", null, null, false, null, moderator));

        verify(orderRepository, never()).save(any());
    }

    // =========================================================================
    // Group 4: completePaidOrder
    // =========================================================================

    @Test
    @DisplayName("UT-UUT05-025: completePaidOrder - Đơn không tồn tại")
    void completePaidOrder_orderNotFound_throwsException() {
        User moderator = User.builder().userId(5).build();
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.completePaidOrder("BSMS-999", moderator));

        assertThat(ex.getMessage()).contains("Không tìm thấy đơn hàng: BSMS-999");
    }

    @Test
    @DisplayName("UT-UUT05-026: completePaidOrder - Moderator không phụ trách đơn")
    void completePaidOrder_wrongModerator_throwsException() {
        User moderator5 = User.builder().userId(5).build();
        User moderator99 = User.builder().userId(99).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PAID").assignedTo(moderator99).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.completePaidOrder("BSMS-123", moderator5));

        assertThat(ex.getMessage()).isEqualTo("Bạn không phụ trách đơn này.");
    }

    @Test
    @DisplayName("UT-UUT05-027: completePaidOrder - Đơn không ở trạng thái PAID (ví dụ DEPOSITED)")
    void completePaidOrder_notPaidStatus_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("DEPOSITED").assignedTo(moderator).build();
        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.completePaidOrder("BSMS-123", moderator));

        assertThat(ex.getMessage()).contains("Trạng thái hiện tại của đơn không cho phép xác nhận hoàn thành.");
    }

    @Test
    @DisplayName("UT-UUT05-028: completePaidOrder - refundableCash < totalRequired (chưa thanh toán đủ)")
    void completePaidOrder_incompletePayment_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PAID")
                .assignedTo(moderator)
                .totalAmount(new BigDecimal("1000000"))
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("500000"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.completePaidOrder("BSMS-123", moderator));

        assertThat(ex.getMessage()).isEqualTo("Không thể hoàn thành đơn vì khách hàng chưa thanh toán đầy đủ.");
    }

    @Test
    @DisplayName("UT-UUT05-029: completePaidOrder - Xác nhận hoàn thành đơn thanh toán 100% thành công")
    void completePaidOrder_success() {
        User moderator = User.builder().userId(5).build();
        Product prod1 = Product.builder().productId(10).productStatus("RESERVED").build();
        OrderDetail detail1 = OrderDetail.builder().product(prod1).build();

        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PAID")
                .assignedTo(moderator)
                .totalAmount(new BigDecimal("1000000"))
                .orderDetails(List.of(detail1))
                .build();

        FinancialLedger ledger = FinancialLedger.builder().financialLedgerId(500).build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("1000000"));
        when(financialLedgerService.recordCompletedOrderRevenueIfAbsent(eq(order), eq(moderator), any(LocalDateTime.class)))
                .thenReturn(ledger);

        boolean result = orderService.completePaidOrder("BSMS-123", moderator);

        assertTrue(result);
        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(order.getCompletedAt()).isNotNull();
        assertThat(prod1.getProductStatus()).isEqualTo("SOLD");

        verify(orderRepository, times(1)).save(order);
        verify(productRepository, times(1)).save(prod1);
        verify(orderLogRepository, times(2)).save(any(OrderLog.class)); // 1 ledger log + 1 completion log
    }

    @Test
    @DisplayName("UT-UUT05-030: Dependency Failure - orderRepository.save ném DataAccessException trong completePaidOrder")
    void completePaidOrder_dependencyFailure_throwsException() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder()
                .orderId(100)
                .orderCode("BSMS-123")
                .orderStatus("PAID")
                .assignedTo(moderator)
                .totalAmount(new BigDecimal("1000000"))
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("1000000"));
        when(orderRepository.save(any())).thenThrow(new DataAccessException("DB Save error") {});

        assertThrows(DataAccessException.class,
                () -> orderService.completePaidOrder("BSMS-123", moderator));
    }

    // =========================================================================
    // Group 5: recordFinalPayment
    // =========================================================================

    @Test
    @DisplayName("UT-UUT05-031: recordFinalPayment - Đơn không tồn tại")
    void recordFinalPayment_orderNotFound_returnsFalse() {
        User moderator = User.builder().userId(5).build();
        when(orderRepository.findByOrderCode("BSMS-999")).thenReturn(Optional.empty());

        boolean result = orderService.recordFinalPayment("BSMS-999", moderator);

        assertFalse(result);
        verify(orderLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT05-032: recordFinalPayment - processPaymentSuccess trả về false")
    void recordFinalPayment_paymentSuccessReturnsFalse_returnsFalse() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();

        // Lần 1: findByOrderCode trong recordFinalPayment trả về Order hợp lệ.
        // Lần 2: findByOrderCode trong processPaymentSuccess trả về Optional.empty() (xử lý đơn không tồn tại).
        when(orderRepository.findByOrderCode("BSMS-123"))
                .thenReturn(Optional.of(order), Optional.empty());

        boolean result = orderService.recordFinalPayment("BSMS-123", moderator);

        assertFalse(result);
        verify(orderLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT05-033: recordFinalPayment - Ghi nhận thanh toán cuối cùng thành công")
    void recordFinalPayment_success() {
        User moderator = User.builder().userId(5).build();
        Order order = Order.builder().orderId(100).orderCode("BSMS-123").orderStatus("PENDING_PAYMENT").build();

        Payment pendingPayment = Payment.builder()
                .paymentId(200)
                .order(order)
                .paymentType(PaymentType.FULL_PAYMENT.name())
                .paymentStatus("PENDING")
                .build();

        when(orderRepository.findByOrderCode("BSMS-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(pendingPayment));

        boolean result = orderService.recordFinalPayment("BSMS-123", moderator);

        assertTrue(result);
        ArgumentCaptor<OrderLog> logCaptor = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogRepository, times(1)).save(logCaptor.capture());

        OrderLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getActionType()).isEqualTo("PAID");
        assertThat(capturedLog.getFromStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(capturedLog.getToStatus()).isEqualTo("PAID");
        assertThat(capturedLog.getActionBy()).isEqualTo(moderator);
    }
}
