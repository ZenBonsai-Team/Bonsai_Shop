package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.OrderLog;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private OrderLogRepository orderLogRepository;
    private FinancialLedgerService financialLedgerService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        orderLogRepository = mock(OrderLogRepository.class);
        OrderHandlingRepository orderHandlingRepository = mock(OrderHandlingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        MailService mailService = mock(MailService.class);
        CartService cartService = mock(CartService.class);
        financialLedgerService = mock(FinancialLedgerService.class);

        orderService = new OrderService(
                orderRepository,
                productRepository,
                orderLogRepository,
                orderHandlingRepository,
                paymentRepository,
                eventPublisher,
                mailService,
                cartService,
                financialLedgerService
        );
    }

    @Test
    void completePaidOrderMarksProductSoldAndRecordsCompletedRevenueOnce() {
        User moderator = moderator(10);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-COMPLETE", "PAID", moderator, product);
        FinancialLedger revenue = ledger(order, FinancialLedgerType.COMPLETED_ORDER_REVENUE);

        when(orderRepository.findByOrderCode("BSMS-COMPLETE")).thenReturn(Optional.of(order));
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
    void partialRefundWithCustomerKeepingTreeCompletesOrderAndKeepsProductSold() {
        User moderator = moderator(11);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-KEEP", "PAID", moderator, product);
        FinancialLedger refund = ledger(order, FinancialLedgerType.PARTIAL_REFUND);

        when(orderRepository.findByOrderCode("BSMS-KEEP")).thenReturn(Optional.of(order));
        when(financialLedgerService.recordManualFaultRefund(
                any(Order.class), any(FaultParty.class), any(BigDecimal.class), any(String.class), any(), any(), any(User.class)))
                .thenReturn(refund);

        boolean result = orderService.recordFaultRefundAndCancel(
                "BSMS-KEEP",
                "NURSERY",
                new BigDecimal("100000"),
                "Tree damaged",
                null,
                null,
                true,
                null,
                moderator
        );

        assertThat(result).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(product.getProductStatus()).isEqualTo("SOLD");
        verify(productRepository).save(product);
        verify(financialLedgerService).recordCompletedOrderRevenueIfAbsent(any(Order.class), any(User.class), any(LocalDateTime.class));
    }

    @Test
    void partialRefundWithReturnedResellableTreeCancelsOrderAndReleasesProduct() {
        User moderator = moderator(12);
        Product product = product("RESERVED");
        Order order = assignedOrder("BSMS-RETURN", "PAID", moderator, product);
        FinancialLedger refund = ledger(order, FinancialLedgerType.PARTIAL_REFUND);

        when(orderRepository.findByOrderCode("BSMS-RETURN")).thenReturn(Optional.of(order));
        when(financialLedgerService.recordManualFaultRefund(
                any(Order.class), any(FaultParty.class), any(BigDecimal.class), any(String.class), any(), any(), any(User.class)))
                .thenReturn(refund);

        boolean result = orderService.recordFaultRefundAndCancel(
                "BSMS-RETURN",
                "DELIVERY",
                new BigDecimal("100000"),
                "Delivery issue",
                null,
                null,
                false,
                "RETURNED_AND_RESELLABLE",
                moderator
        );

        assertThat(result).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(product.getProductStatus()).isEqualTo("AVAILABLE");
        verify(productRepository).save(product);
    }

    @Test
    void partialRefundDoesNotReleaseDamagedOrNotReturnedProduct() {
        User moderator = moderator(13);
        Product damagedProduct = product("RESERVED");
        Order damagedOrder = assignedOrder("BSMS-DAMAGED", "PAID", moderator, damagedProduct);
        Product notReturnedProduct = product("RESERVED");
        Order notReturnedOrder = assignedOrder("BSMS-NOTRETURNED", "PAID", moderator, notReturnedProduct);

        when(orderRepository.findByOrderCode("BSMS-DAMAGED")).thenReturn(Optional.of(damagedOrder));
        when(orderRepository.findByOrderCode("BSMS-NOTRETURNED")).thenReturn(Optional.of(notReturnedOrder));
        when(financialLedgerService.recordManualFaultRefund(
                any(Order.class), any(FaultParty.class), any(BigDecimal.class), any(String.class), any(), any(), any(User.class)))
                .thenReturn(ledger(damagedOrder, FinancialLedgerType.PARTIAL_REFUND));

        orderService.recordFaultRefundAndCancel(
                "BSMS-DAMAGED",
                "NURSERY",
                new BigDecimal("100000"),
                "Damaged",
                null,
                null,
                false,
                "RETURNED_AND_DAMAGED",
                moderator
        );
        orderService.recordFaultRefundAndCancel(
                "BSMS-NOTRETURNED",
                "DELIVERY",
                new BigDecimal("100000"),
                "Not returned",
                null,
                null,
                false,
                "NOT_RETURNED",
                moderator
        );

        assertThat(damagedProduct.getProductStatus()).isEqualTo("SOLD");
        assertThat(notReturnedProduct.getProductStatus()).isEqualTo("SOLD");
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
