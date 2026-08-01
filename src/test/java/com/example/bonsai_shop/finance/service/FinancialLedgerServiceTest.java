package com.example.bonsai_shop.finance.service;

import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerDirection;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.product.enums.PaymentType;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialLedgerServiceTest {

    private FinancialLedgerRepository financialLedgerRepository;
    private PaymentRepository paymentRepository;
    private FinancialLedgerService financialLedgerService;

    @BeforeEach
    void setUp() {
        financialLedgerRepository = mock(FinancialLedgerRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        financialLedgerService = new FinancialLedgerService(financialLedgerRepository, paymentRepository);
    }

    @Test
    void completedOrderCreatesOneRecordedIncomeLedgerEntry() {
        Order order = order(1);
        order.setShippingFee(new BigDecimal("200000"));
        order.setCraneFee(new BigDecimal("300000"));
        order.setOrderDetails(List.of(detail(order, "1000000", 2)));

        when(financialLedgerRepository.existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(
                1,
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED
        )).thenReturn(false);
        when(financialLedgerRepository.save(any(FinancialLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, moderator(99), null);

        ArgumentCaptor<FinancialLedger> captor = ArgumentCaptor.forClass(FinancialLedger.class);
        verify(financialLedgerRepository).save(captor.capture());
        assertThat(captor.getValue().getLedgerType()).isEqualTo(FinancialLedgerType.COMPLETED_ORDER_REVENUE);
        assertThat(captor.getValue().getDirection()).isEqualTo(FinancialLedgerDirection.INCOME);
        assertThat(captor.getValue().getLedgerStatus()).isEqualTo(FinancialLedgerStatus.RECORDED);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("2500000");
    }

    @Test
    void retryCompletedOrderDoesNotDuplicateRevenue() {
        Order order = order(2);

        when(financialLedgerRepository.existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(
                2,
                FinancialLedgerType.COMPLETED_ORDER_REVENUE,
                FinancialLedgerStatus.RECORDED
        )).thenReturn(true);

        FinancialLedger result = financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, moderator(99), null);

        assertThat(result).isNull();
        verify(financialLedgerRepository, never()).save(any(FinancialLedger.class));
    }

    @Test
    void customerFaultNoShowCreatesOneRecordedForfeitedDepositIncome() {
        Order order = order(3);
        Payment deposit = payment(10, order, PaymentType.DEPOSIT.name(), "SUCCESS", "1000000");
        User moderator = moderator(99);

        when(financialLedgerRepository.existsByRelatedPaymentPaymentIdAndLedgerTypeAndLedgerStatus(
                10,
                FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                FinancialLedgerStatus.RECORDED
        )).thenReturn(false);
        when(financialLedgerRepository.save(any(FinancialLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        financialLedgerService.recordForfeitedDepositIncome(order, deposit, new BigDecimal("1000000"),
                "Customer refused delivery", moderator);

        ArgumentCaptor<FinancialLedger> captor = ArgumentCaptor.forClass(FinancialLedger.class);
        verify(financialLedgerRepository).save(captor.capture());
        assertThat(captor.getValue().getLedgerType()).isEqualTo(FinancialLedgerType.FORFEITED_DEPOSIT_INCOME);
        assertThat(captor.getValue().getDirection()).isEqualTo(FinancialLedgerDirection.INCOME);
        assertThat(captor.getValue().getFaultParty()).isEqualTo(FaultParty.CUSTOMER);
        assertThat(captor.getValue().getLedgerStatus()).isEqualTo(FinancialLedgerStatus.RECORDED);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("1000000");
    }

    @Test
    void repeatedCustomerFaultNoShowIsRejected() {
        Order order = order(4);
        Payment deposit = payment(11, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");

        when(financialLedgerRepository.existsByRelatedPaymentPaymentIdAndLedgerTypeAndLedgerStatus(
                11,
                FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                FinancialLedgerStatus.RECORDED
        )).thenReturn(true);

        assertThatThrownBy(() -> financialLedgerService.recordForfeitedDepositIncome(
                order,
                deposit,
                new BigDecimal("500000"),
                "Customer disappeared",
                moderator(99)
        )).isInstanceOf(IllegalStateException.class);

        verify(financialLedgerRepository, never()).save(any(FinancialLedger.class));
    }

    @Test
    void partialRefundCreatesOutflowLedgerEntry() {
        Order order = order(5);
        Payment fullPayment = payment(12, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "1000000");

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(5)).thenReturn(List.of(fullPayment));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(5, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of());
        when(financialLedgerRepository.save(any(FinancialLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        financialLedgerService.recordManualFaultRefund(
                order,
                FaultParty.NURSERY,
                new BigDecimal("300000"),
                "Nursery compensation",
                null,
                null,
                moderator(99)
        );

        ArgumentCaptor<FinancialLedger> captor = ArgumentCaptor.forClass(FinancialLedger.class);
        verify(financialLedgerRepository).save(captor.capture());
        assertThat(captor.getValue().getLedgerType()).isEqualTo(FinancialLedgerType.PARTIAL_REFUND);
        assertThat(captor.getValue().getDirection()).isEqualTo(FinancialLedgerDirection.OUTFLOW);
        assertThat(captor.getValue().getFaultParty()).isEqualTo(FaultParty.NURSERY);
    }

    @Test
    void fullRefundCreatesOutflowLedgerEntry() {
        Order order = order(6);
        Payment deposit = payment(13, order, PaymentType.DEPOSIT.name(), "SUCCESS", "1000000");

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(6)).thenReturn(List.of(deposit));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(6, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of());
        when(financialLedgerRepository.save(any(FinancialLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        financialLedgerService.recordManualFaultRefund(
                order,
                FaultParty.DELIVERY,
                new BigDecimal("1000000"),
                "Delivery damaged the tree",
                null,
                null,
                moderator(99)
        );

        ArgumentCaptor<FinancialLedger> captor = ArgumentCaptor.forClass(FinancialLedger.class);
        verify(financialLedgerRepository).save(captor.capture());
        assertThat(captor.getValue().getLedgerType()).isEqualTo(FinancialLedgerType.FULL_REFUND);
        assertThat(captor.getValue().getDirection()).isEqualTo(FinancialLedgerDirection.OUTFLOW);
        assertThat(captor.getValue().getFaultParty()).isEqualTo(FaultParty.DELIVERY);
    }

    @Test
    void refundCannotExceedSuccessfulCashMinusPreviousRefunds() {
        Order order = order(7);
        Payment deposit = payment(14, order, PaymentType.DEPOSIT.name(), "SUCCESS", "1000000");
        FinancialLedger previousRefund = FinancialLedger.builder()
                .order(order)
                .ledgerType(FinancialLedgerType.PARTIAL_REFUND)
                .direction(FinancialLedgerDirection.OUTFLOW)
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .amount(new BigDecimal("300000"))
                .build();

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(7)).thenReturn(List.of(deposit));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(7, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of(previousRefund));

        assertThatThrownBy(() -> financialLedgerService.recordManualFaultRefund(
                order,
                FaultParty.NURSERY,
                new BigDecimal("800000"),
                "Wrong tree delivered",
                null,
                null,
                moderator(99)
        )).isInstanceOf(IllegalArgumentException.class);

        verify(financialLedgerRepository, never()).save(any(FinancialLedger.class));
    }

    @Test
    void refundableCashIgnoresNonSuccessfulPaymentsAndVoidedRefunds() {
        Order order = order(8);
        Payment success = payment(15, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "1000000");
        Payment pending = payment(16, order, PaymentType.DEPOSIT.name(), "PENDING", "2000000");
        Payment failed = payment(17, order, PaymentType.DEPOSIT.name(), "FAILED", "3000000");
        Payment cancelled = payment(18, order, PaymentType.DEPOSIT.name(), "CANCELLED", "4000000");
        FinancialLedger recordedRefund = FinancialLedger.builder()
                .order(order)
                .ledgerType(FinancialLedgerType.PARTIAL_REFUND)
                .direction(FinancialLedgerDirection.OUTFLOW)
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .amount(new BigDecimal("250000"))
                .build();

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(8))
                .thenReturn(List.of(success, pending, failed, cancelled));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(8, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of(recordedRefund));

        BigDecimal refundable = financialLedgerService.calculateRefundableCash(order);

        assertThat(refundable).isEqualByComparingTo("750000");
    }

    @Test
    void refundableCashNeverReturnsNegativeAmount() {
        Order order = order(9);
        Payment success = payment(19, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "100000");
        FinancialLedger recordedRefund = FinancialLedger.builder()
                .order(order)
                .ledgerType(FinancialLedgerType.FULL_REFUND)
                .direction(FinancialLedgerDirection.OUTFLOW)
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .amount(new BigDecimal("200000"))
                .build();

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(9)).thenReturn(List.of(success));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(9, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of(recordedRefund));

        BigDecimal refundable = financialLedgerService.calculateRefundableCash(order);

        assertThat(refundable).isEqualByComparingTo("0");
    }

    @Test
    void zeroAndNegativeRefundAmountsAreRejected() {
        Order order = order(10);
        Payment success = payment(20, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "1000000");

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(10)).thenReturn(List.of(success));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(10, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of());

        assertThatThrownBy(() -> financialLedgerService.recordManualFaultRefund(
                order,
                FaultParty.NURSERY,
                BigDecimal.ZERO,
                "Compensation",
                null,
                null,
                moderator(99)
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> financialLedgerService.recordManualFaultRefund(
                order,
                FaultParty.NURSERY,
                new BigDecimal("-1"),
                "Compensation",
                null,
                null,
                moderator(99)
        )).isInstanceOf(IllegalArgumentException.class);

        verify(financialLedgerRepository, never()).save(any(FinancialLedger.class));
    }

    private Order order(Integer orderId) {
        return Order.builder()
                .orderId(orderId)
                .orderCode("BSMS-" + orderId)
                .build();
    }

    private OrderDetail detail(Order order, String price, int quantity) {
        return OrderDetail.builder()
                .order(order)
                .priceAtPurchase(new BigDecimal(price))
                .quantity(quantity)
                .build();
    }

    private Payment payment(Integer paymentId, Order order, String type, String status, String amount) {
        return Payment.builder()
                .paymentId(paymentId)
                .order(order)
                .paymentType(type)
                .paymentStatus(status)
                .amount(new BigDecimal(amount))
                .build();
    }

    private User moderator(Integer userId) {
        return User.builder()
                .userId(userId)
                .fullName("Moderator " + userId)
                .build();
    }
}
