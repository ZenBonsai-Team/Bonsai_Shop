package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderLog;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerDirection;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.service.FinancialLedgerService;
import com.example.bonsai_shop.product.repository.OrderHandlingRepository;
import com.example.bonsai_shop.product.repository.OrderLogRepository;
import com.example.bonsai_shop.product.repository.OrderRepository;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.enums.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private OrderLogRepository orderLogRepository;
    private PaymentRepository paymentRepository;
    private FinancialLedgerService financialLedgerService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        orderLogRepository = mock(OrderLogRepository.class);
        OrderHandlingRepository orderHandlingRepository = mock(OrderHandlingRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
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

    @Test
    void fullPaymentSuccessMarksPaidAndRecordsRevenueLedger() {
        User moderator = moderator(14);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-FULL", "PENDING_PAYMENT", moderator, product);
        Payment fullPayment = Payment.builder()
                .paymentId(300)
                .order(order)
                .paymentType(PaymentType.FULL_PAYMENT.name())
                .paymentStatus("PENDING")
                .amount(new BigDecimal("1000000"))
                .build();
        FinancialLedger revenue = ledger(order, FinancialLedgerType.COMPLETED_ORDER_REVENUE);

        when(orderRepository.findByOrderCode("BSMS-FULL")).thenReturn(Optional.of(order));
        when(paymentRepository.findTopByOrderOrderIdAndPaymentStatusOrderByPaymentIdDesc(100, "PENDING"))
                .thenReturn(Optional.of(fullPayment));
        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(100)).thenReturn(List.of(fullPayment));
        when(financialLedgerService.recordCompletedOrderRevenueIfAbsent(any(Order.class), any(User.class), any(LocalDateTime.class)))
                .thenReturn(revenue);

        boolean result = orderService.processPaymentSuccess("BSMS-FULL");

        assertThat(result).isTrue();
        assertThat(fullPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(order.getOrderStatus()).isEqualTo("PAID");
        assertThat(product.getProductStatus()).isEqualTo("RESERVED");
        verify(financialLedgerService).recordCompletedOrderRevenueIfAbsent(any(Order.class), any(User.class), any(LocalDateTime.class));
        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    void completePaidOrderMarksProductSoldAndRecordsCompletedRevenueOnce() {
        User moderator = moderator(10);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-COMPLETE", "PAID", moderator, product);
        FinancialLedger revenue = ledger(order, FinancialLedgerType.COMPLETED_ORDER_REVENUE);

        when(orderRepository.findByOrderCode("BSMS-COMPLETE")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("1000000"));
        when(financialLedgerService.recordCompletedOrderRevenueIfAbsent(any(Order.class), any(User.class), any(LocalDateTime.class)))
                .thenReturn(revenue);

        boolean result = orderService.completePaidOrder("BSMS-COMPLETE", moderator);

        assertThat(result).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(product.getProductStatus()).isEqualTo("SOLD");
        verify(productRepository).save(product);
        verify(financialLedgerService).recordCompletedOrderRevenueIfAbsent(any(Order.class), any(User.class), any(LocalDateTime.class));
        verify(orderLogRepository, times(2)).save(any(OrderLog.class));
    }

    @Test
    void completePaidOrderFailsWhenNotFullyPaid() {
        User moderator = moderator(10);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-UNPAID-COMPLETE", "PAID", moderator, product);
        order.setTotalAmount(new BigDecimal("1000000"));

        when(orderRepository.findByOrderCode("BSMS-UNPAID-COMPLETE")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("500000"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                orderService.completePaidOrder("BSMS-UNPAID-COMPLETE", moderator)
        );
    }

    @Test
    void fullRefundCancelsOrderAndReleasesAllProductsToAvailable() {
        User moderator = moderator(11);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-RETURN", "PAID", moderator, product);
        order.setTotalAmount(new BigDecimal("1000000"));
        FinancialLedger refund = ledger(order, FinancialLedgerType.FULL_REFUND);

        when(orderRepository.findByOrderCode("BSMS-RETURN")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("1000000"));
        when(financialLedgerService.recordManualFaultRefund(
                any(Order.class), any(FaultParty.class), any(BigDecimal.class), any(String.class), any(), any(), any(User.class)))
                .thenReturn(refund);

        boolean result = orderService.recordFaultRefundAndCancel(
                "BSMS-RETURN",
                "NURSERY",
                null,
                "Tree damaged",
                null,
                null,
                false,
                null,
                moderator
        );

        assertThat(result).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
        verify(productRepository).save(product);
        verify(financialLedgerService).recordManualFaultRefund(
                eq(order), eq(FaultParty.NURSERY), eq(new BigDecimal("1000000")), eq("Tree damaged"), any(), any(), eq(moderator));
    }

    @Test
    void depositRefundRefundsOnlyPaidAmountNotGrandTotal() {
        User moderator = moderator(15);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-DEPOSIT-REFUND", "DEPOSITED", moderator, product);
        order.setTotalAmount(new BigDecimal("7800000")); // Order total is 7.800.000đ
        FinancialLedger refund = ledger(order, FinancialLedgerType.FULL_REFUND);

        when(orderRepository.findByOrderCode("BSMS-DEPOSIT-REFUND")).thenReturn(Optional.of(order));
        // Customer actually paid deposit of 1.200.000đ
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(new BigDecimal("1200000"));
        when(financialLedgerService.recordManualFaultRefund(
                any(Order.class), any(FaultParty.class), any(BigDecimal.class), any(String.class), any(), any(), any(User.class)))
                .thenReturn(refund);

        boolean result = orderService.recordFaultRefundAndCancel(
                "BSMS-DEPOSIT-REFUND",
                "NURSERY",
                null,
                "Nursery fault deposit refund",
                null,
                null,
                false,
                null,
                moderator
        );

        assertThat(result).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
        verify(productRepository).save(product);
        // Verify refund amount is 1.200.000đ, NOT 7.800.000đ!
        verify(financialLedgerService).recordManualFaultRefund(
                eq(order), eq(FaultParty.NURSERY), eq(new BigDecimal("1200000")), eq("Nursery fault deposit refund"), any(), any(), eq(moderator));
    }

    @Test
    void refundRejectsOrderWithoutSuccessfulPayments() {
        User moderator = moderator(12);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-NOPAY", "PAID", moderator, product);

        when(orderRepository.findByOrderCode("BSMS-NOPAY")).thenReturn(Optional.of(order));
        when(financialLedgerService.calculateRefundableCash(order)).thenReturn(BigDecimal.ZERO);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                orderService.recordFaultRefundAndCancel(
                        "BSMS-NOPAY",
                        "NURSERY",
                        null,
                        "No payment",
                        null,
                        null,
                        false,
                        null,
                        moderator
                )
        );
    }

    private Order assignedOrder(String code, String status, User moderator, Product product) {
        Order order = Order.builder()
                .orderId(100)
                .orderCode(code)
                .orderStatus(status)
                .assignedTo(moderator)
                .assignedAt(LocalDateTime.now())
                .totalAmount(new BigDecimal("1000000"))
                .build();
        order.setOrderDetails(List.of(OrderDetail.builder()
                .order(order)
                .product(product)
                .priceAtPurchase(new BigDecimal("1000000"))
                .quantity(1)
                .build()));
        return order;
    }

    private Product product(String status) {
        return Product.builder()
                .productId(200)
                .productStatus(status)
                .build();
    }

    private User moderator(Integer userId) {
        return User.builder()
                .userId(userId)
                .fullName("Moderator " + userId)
                .build();
    }

    private FinancialLedger ledger(Order order, FinancialLedgerType type) {
        return FinancialLedger.builder()
                .financialLedgerId(1)
                .order(order)
                .ledgerType(type)
                .direction(type == FinancialLedgerType.COMPLETED_ORDER_REVENUE
                        ? FinancialLedgerDirection.INCOME
                        : FinancialLedgerDirection.OUTFLOW)
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .amount(new BigDecimal("100000"))
                .build();
    }
}
