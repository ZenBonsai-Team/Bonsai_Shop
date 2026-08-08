package com.example.bonsai_shop.finance.service;

import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.entity.Order;
import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.entity.Payment;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.finance.dto.FinancialLedgerDTO;
import com.example.bonsai_shop.finance.enums.FaultParty;
import com.example.bonsai_shop.finance.enums.FinancialLedgerDirection;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.product.enums.PaymentType;
import com.example.bonsai_shop.product.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    // =========================================================================
    // Group 1: requireSuccessfulDepositPayment
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-001: requireSuccessfulDepositPayment - order = null hoặc orderId = null -> IllegalArgumentException")
    void requireSuccessfulDepositPayment_nullOrder_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> financialLedgerService.requireSuccessfulDepositPayment(null));

        Order nullIdOrder = Order.builder().orderId(null).build();
        assertThrows(IllegalArgumentException.class, () -> financialLedgerService.requireSuccessfulDepositPayment(nullIdOrder));
    }

    @Test
    @DisplayName("UT-UUT11-002: requireSuccessfulDepositPayment - Không tìm thấy deposit payment SUCCESS -> IllegalStateException")
    void requireSuccessfulDepositPayment_noSuccessfulDeposit_throwsIllegalStateException() {
        Order order = order(1);
        Payment pendingDeposit = payment(10, order, PaymentType.DEPOSIT.name(), "PENDING", "500000");

        when(paymentRepository.findByOrderOrderIdAndPaymentType(1, PaymentType.DEPOSIT.name()))
                .thenReturn(List.of(pendingDeposit));

        assertThrows(IllegalStateException.class, () -> financialLedgerService.requireSuccessfulDepositPayment(order));
    }

    @Test
    @DisplayName("UT-UUT11-003: requireSuccessfulDepositPayment - Tìm thấy deposit payment SUCCESS -> Trả về Payment")
    void requireSuccessfulDepositPayment_successfulDepositExists_returnsPayment() {
        Order order = order(1);
        Payment successDeposit = payment(11, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");

        when(paymentRepository.findByOrderOrderIdAndPaymentType(1, PaymentType.DEPOSIT.name()))
                .thenReturn(List.of(successDeposit));

        Payment result = financialLedgerService.requireSuccessfulDepositPayment(order);

        assertNotNull(result);
        assertEquals(11, result.getPaymentId());
        assertEquals("SUCCESS", result.getPaymentStatus());
    }

    // =========================================================================
    // Group 2: recordCompletedOrderRevenueIfAbsent
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-004: recordCompletedOrderRevenueIfAbsent - order hoặc actor = null -> IllegalArgumentException")
    void recordCompletedOrderRevenueIfAbsent_invalidOrderOrActor_throwsIllegalArgumentException() {
        Order order = order(2);
        User actor = moderator(99);

        assertThrows(IllegalArgumentException.class, () -> financialLedgerService.recordCompletedOrderRevenueIfAbsent(null, actor, null));
        assertThrows(IllegalArgumentException.class, () -> financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, null, null));

        User nullIdActor = User.builder().userId(null).build();
        assertThrows(IllegalArgumentException.class, () -> financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, nullIdActor, null));
    }

    @Test
    @DisplayName("UT-UUT11-005: recordCompletedOrderRevenueIfAbsent - Đã tồn tại sổ cái doanh thu -> Trả về null")
    void recordCompletedOrderRevenueIfAbsent_alreadyExists_returnsNull() {
        Order order = order(2);
        User actor = moderator(99);

        when(financialLedgerRepository.existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(
                2, FinancialLedgerType.COMPLETED_ORDER_REVENUE, FinancialLedgerStatus.RECORDED))
                .thenReturn(true);

        FinancialLedger result = financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, actor, null);

        assertNull(result);
        verify(financialLedgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-UUT11-006: recordCompletedOrderRevenueIfAbsent - Chưa tồn tại, có recognizedAt -> Lưu FinancialLedger")
    void recordCompletedOrderRevenueIfAbsent_validInputWithRecognizedAt_savesLedger() {
        Order order = order(3);
        order.setShippingFee(new BigDecimal("100000"));
        order.setCraneFee(new BigDecimal("50000"));
        order.setOrderDetails(List.of(detail(order, "500000", 2))); // 1,000,000 tree
        User actor = moderator(99);
        LocalDateTime specificTime = LocalDateTime.of(2026, 8, 8, 10, 0);

        when(financialLedgerRepository.existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(
                3, FinancialLedgerType.COMPLETED_ORDER_REVENUE, FinancialLedgerStatus.RECORDED))
                .thenReturn(false);
        when(financialLedgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialLedger result = financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, actor, specificTime);

        assertNotNull(result);
        assertEquals(FinancialLedgerType.COMPLETED_ORDER_REVENUE, result.getLedgerType());
        assertEquals(FinancialLedgerDirection.INCOME, result.getDirection());
        assertThat(result.getAmount()).isEqualByComparingTo("1150000"); // 1M + 100k + 50k
        assertEquals(specificTime, result.getRecognizedAt());
    }

    @Test
    @DisplayName("UT-UUT11-007: recordCompletedOrderRevenueIfAbsent - Chưa tồn tại, recognizedAt = null -> Gán now()")
    void recordCompletedOrderRevenueIfAbsent_nullRecognizedAt_assignsNow() {
        Order order = order(4);
        User actor = moderator(99);

        when(financialLedgerRepository.existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(
                4, FinancialLedgerType.COMPLETED_ORDER_REVENUE, FinancialLedgerStatus.RECORDED))
                .thenReturn(false);
        when(financialLedgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialLedger result = financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, actor, null);

        assertNotNull(result);
        assertNotNull(result.getRecognizedAt());
    }

    // =========================================================================
    // Group 3: recordForfeitedDepositIncome
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-008: recordForfeitedDepositIncome - depositPayment = null hoặc ID null -> IllegalArgumentException")
    void recordForfeitedDepositIncome_nullDepositPayment_throwsIllegalArgumentException() {
        Order order = order(5);
        User actor = moderator(99);

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordForfeitedDepositIncome(order, null, new BigDecimal("100000"), "reason", actor));

        Payment nullIdPayment = Payment.builder().paymentId(null).build();
        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordForfeitedDepositIncome(order, nullIdPayment, new BigDecimal("100000"), "reason", actor));
    }

    @Test
    @DisplayName("UT-UUT11-009: recordForfeitedDepositIncome - Đã tịch thu cọc trước đó -> IllegalStateException")
    void recordForfeitedDepositIncome_alreadyForfeited_throwsIllegalStateException() {
        Order order = order(5);
        Payment deposit = payment(50, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");
        User actor = moderator(99);

        when(financialLedgerRepository.existsByRelatedPaymentPaymentIdAndLedgerTypeAndLedgerStatus(
                50, FinancialLedgerType.FORFEITED_DEPOSIT_INCOME, FinancialLedgerStatus.RECORDED))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                financialLedgerService.recordForfeitedDepositIncome(order, deposit, new BigDecimal("500000"), "reason", actor));
    }

    @Test
    @DisplayName("UT-UUT11-010: recordForfeitedDepositIncome - amount <= 0 -> IllegalArgumentException")
    void recordForfeitedDepositIncome_nonPositiveAmount_throwsIllegalArgumentException() {
        Order order = order(6);
        Payment deposit = payment(60, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");
        User actor = moderator(99);

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordForfeitedDepositIncome(order, deposit, BigDecimal.ZERO, "reason", actor));

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordForfeitedDepositIncome(order, deposit, new BigDecimal("-100"), "reason", actor));
    }

    @Test
    @DisplayName("UT-UUT11-011: recordForfeitedDepositIncome - amount > depositPayment.amount -> IllegalArgumentException")
    void recordForfeitedDepositIncome_amountExceedsDeposit_throwsIllegalArgumentException() {
        Order order = order(7);
        Payment deposit = payment(70, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");
        User actor = moderator(99);

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordForfeitedDepositIncome(order, deposit, new BigDecimal("600000"), "reason", actor));
    }

    @Test
    @DisplayName("UT-UUT11-012: recordForfeitedDepositIncome - reason null/blank -> IllegalArgumentException")
    void recordForfeitedDepositIncome_blankReason_throwsIllegalArgumentException() {
        Order order = order(8);
        Payment deposit = payment(80, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");
        User actor = moderator(99);

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordForfeitedDepositIncome(order, deposit, new BigDecimal("500000"), null, actor));

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordForfeitedDepositIncome(order, deposit, new BigDecimal("500000"), "   ", actor));
    }

    @Test
    @DisplayName("UT-UUT11-013: recordForfeitedDepositIncome - Giữ cọc hợp lệ -> Lưu FinancialLedger FORFEITED_DEPOSIT_INCOME")
    void recordForfeitedDepositIncome_validInput_savesLedger() {
        Order order = order(9);
        Payment deposit = payment(90, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");
        User actor = moderator(99);

        when(financialLedgerRepository.existsByRelatedPaymentPaymentIdAndLedgerTypeAndLedgerStatus(
                90, FinancialLedgerType.FORFEITED_DEPOSIT_INCOME, FinancialLedgerStatus.RECORDED))
                .thenReturn(false);
        when(financialLedgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialLedger result = financialLedgerService.recordForfeitedDepositIncome(order, deposit, new BigDecimal("300000"), "Khách bỏ đơn", actor);

        assertNotNull(result);
        assertEquals(FinancialLedgerType.FORFEITED_DEPOSIT_INCOME, result.getLedgerType());
        assertEquals(FinancialLedgerDirection.INCOME, result.getDirection());
        assertEquals(FaultParty.CUSTOMER, result.getFaultParty());
        assertThat(result.getAmount()).isEqualByComparingTo("300000");
        assertEquals("Khách bỏ đơn", result.getReason());
    }

    // =========================================================================
    // Group 4: recordManualFaultRefund
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-014: recordManualFaultRefund - faultParty = CUSTOMER -> IllegalArgumentException")
    void recordManualFaultRefund_customerFaultParty_throwsIllegalArgumentException() {
        Order order = order(10);
        User actor = moderator(99);

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordManualFaultRefund(order, FaultParty.CUSTOMER, new BigDecimal("100000"), "reason", null, null, actor));
    }

    @Test
    @DisplayName("UT-UUT11-015: recordManualFaultRefund - amount <= 0 -> IllegalArgumentException")
    void recordManualFaultRefund_nonPositiveAmount_throwsIllegalArgumentException() {
        Order order = order(11);
        User actor = moderator(99);

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordManualFaultRefund(order, FaultParty.NURSERY, BigDecimal.ZERO, "reason", null, null, actor));

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordManualFaultRefund(order, FaultParty.NURSERY, new BigDecimal("-500"), "reason", null, null, actor));
    }

    @Test
    @DisplayName("UT-UUT11-016: recordManualFaultRefund - amount > calculateRefundableCash -> IllegalArgumentException")
    void recordManualFaultRefund_amountExceedsRefundable_throwsIllegalArgumentException() {
        Order order = order(12);
        User actor = moderator(99);
        Payment payment = payment(120, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "1000000");

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(12)).thenReturn(List.of(payment));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(12, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
                financialLedgerService.recordManualFaultRefund(order, FaultParty.NURSERY, new BigDecimal("1500000"), "reason", null, null, actor));
    }

    @Test
    @DisplayName("UT-UUT11-017: recordManualFaultRefund - Hoàn tiền hợp lệ có FULL_PAYMENT -> Lưu FULL_REFUND, OUTFLOW")
    void recordManualFaultRefund_validWithFullPayment_savesLedger() {
        Order order = order(13);
        User actor = moderator(99);
        Payment fullPayment = payment(130, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "1000000");

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(13)).thenReturn(List.of(fullPayment));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(13, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of());
        when(financialLedgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialLedger result = financialLedgerService.recordManualFaultRefund(
                order, FaultParty.NURSERY, new BigDecimal("400000"), "Cây bị nứt chậu", "Ảnh chứng minh", "REF-12345", actor);

        assertNotNull(result);
        assertEquals(FinancialLedgerType.FULL_REFUND, result.getLedgerType());
        assertEquals(FinancialLedgerDirection.OUTFLOW, result.getDirection());
        assertEquals(FaultParty.NURSERY, result.getFaultParty());
        assertEquals("Cây bị nứt chậu", result.getReason());
        assertEquals("Ảnh chứng minh", result.getEvidenceNote());
        assertEquals("REF-12345", result.getExternalReference());
        assertEquals(fullPayment, result.getRelatedPayment());
    }

    @Test
    @DisplayName("UT-UUT11-018: recordManualFaultRefund - Ưu tiên tìm payment theo thứ tự: FULL -> REMAINING -> DEPOSIT")
    void recordManualFaultRefund_paymentResolutionPriority_selectsCorrectPayment() {
        Order order = order(14);
        User actor = moderator(99);
        Payment depositPayment = payment(141, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");
        Payment remainingPayment = payment(142, order, PaymentType.REMAINING_PAYMENT.name(), "SUCCESS", "500000");

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(14)).thenReturn(List.of(depositPayment, remainingPayment));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(14, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of());
        when(financialLedgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialLedger result = financialLedgerService.recordManualFaultRefund(
                order, FaultParty.DELIVERY, new BigDecimal("200000"), "Giao muộn 2 ngày", null, null, actor);

        assertEquals(remainingPayment, result.getRelatedPayment());
    }

    @Test
    @DisplayName("UT-UUT11-019: recordManualFaultRefund - evidenceNote & externalReference rỗng/blank -> blankToNull gán null")
    void recordManualFaultRefund_blankNotesAndRef_convertsToNull() {
        Order order = order(15);
        User actor = moderator(99);
        Payment fullPayment = payment(150, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "1000000");

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(15)).thenReturn(List.of(fullPayment));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(15, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of());
        when(financialLedgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialLedger result = financialLedgerService.recordManualFaultRefund(
                order, FaultParty.NURSERY, new BigDecimal("100000"), "Lỗi lá úa", "  ", "", actor);

        assertNull(result.getEvidenceNote());
        assertNull(result.getExternalReference());
    }

    // =========================================================================
    // Group 5: hasRecordedDepositForfeiture
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-020: hasRecordedDepositForfeiture - depositPayment = null hoặc ID null -> return false")
    void hasRecordedDepositForfeiture_nullPaymentOrId_returnsFalse() {
        assertFalse(financialLedgerService.hasRecordedDepositForfeiture(null));

        Payment nullId = Payment.builder().paymentId(null).build();
        assertFalse(financialLedgerService.hasRecordedDepositForfeiture(nullId));
    }

    @Test
    @DisplayName("UT-UUT11-021: hasRecordedDepositForfeiture - Payment hợp lệ, repository trả về true -> return true")
    void hasRecordedDepositForfeiture_validPaymentExists_returnsTrue() {
        Payment payment = Payment.builder().paymentId(50).build();

        when(financialLedgerRepository.existsByRelatedPaymentPaymentIdAndLedgerTypeAndLedgerStatus(
                50, FinancialLedgerType.FORFEITED_DEPOSIT_INCOME, FinancialLedgerStatus.RECORDED))
                .thenReturn(true);

        assertTrue(financialLedgerService.hasRecordedDepositForfeiture(payment));
    }

    // =========================================================================
    // Group 6: Summary & Calculation Methods
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-022: sumMethods - order = null hoặc ID null -> return ZERO")
    void sumMethods_nullOrder_returnsZero() {
        assertThat(financialLedgerService.sumRecognizedCompletedRevenue(null)).isEqualByComparingTo("0");
        assertThat(financialLedgerService.sumForfeitedDepositIncome(null)).isEqualByComparingTo("0");
        assertThat(financialLedgerService.sumFullRefunds(null)).isEqualByComparingTo("0");
        assertThat(financialLedgerService.sumRecordedRefunds(null)).isEqualByComparingTo("0");
        assertThat(financialLedgerService.calculateRefundableCash(null)).isEqualByComparingTo("0");

        Order nullId = Order.builder().orderId(null).build();
        assertThat(financialLedgerService.sumRecognizedCompletedRevenue(nullId)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("UT-UUT11-023: sumByType - Tính toán tổng tiền theo từng loại sổ cái RECORDED")
    void sumByType_calculatesCorrectSumsPerType() {
        Order order = order(16);
        FinancialLedger rev = FinancialLedger.builder().ledgerType(FinancialLedgerType.COMPLETED_ORDER_REVENUE).amount(new BigDecimal("1000000")).build();
        FinancialLedger forfeit = FinancialLedger.builder().ledgerType(FinancialLedgerType.FORFEITED_DEPOSIT_INCOME).amount(new BigDecimal("200000")).build();
        FinancialLedger refund = FinancialLedger.builder().ledgerType(FinancialLedgerType.FULL_REFUND).amount(new BigDecimal("150000")).build();

        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(16, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of(rev, forfeit, refund));

        assertThat(financialLedgerService.sumRecognizedCompletedRevenue(order)).isEqualByComparingTo("1000000");
        assertThat(financialLedgerService.sumForfeitedDepositIncome(order)).isEqualByComparingTo("200000");
        assertThat(financialLedgerService.sumFullRefunds(order)).isEqualByComparingTo("150000");
        assertThat(financialLedgerService.sumRecordedRefunds(order)).isEqualByComparingTo("150000");
    }

    @Test
    @DisplayName("UT-UUT11-024: sumNetRecognizedAmount - Net = CompletedRevenue + ForfeitedDeposit - Refunds")
    void sumNetRecognizedAmount_calculatesNetCorrectly() {
        Order order = order(17);
        FinancialLedger rev = FinancialLedger.builder().ledgerType(FinancialLedgerType.COMPLETED_ORDER_REVENUE).amount(new BigDecimal("1000000")).build();
        FinancialLedger forfeit = FinancialLedger.builder().ledgerType(FinancialLedgerType.FORFEITED_DEPOSIT_INCOME).amount(new BigDecimal("200000")).build();
        FinancialLedger refund = FinancialLedger.builder().ledgerType(FinancialLedgerType.FULL_REFUND).amount(new BigDecimal("100000")).build();

        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(17, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of(rev, forfeit, refund));

        BigDecimal net = financialLedgerService.sumNetRecognizedAmount(order);

        assertThat(net).isEqualByComparingTo("1100000"); // 1M + 200k - 100k
    }

    @Test
    @DisplayName("UT-UUT11-025: calculateRefundableCash - Nếu refunds > successfulCash -> return ZERO (không âm)")
    void calculateRefundableCash_refundsExceedCash_returnsZero() {
        Order order = order(18);
        Payment p1 = payment(180, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "100000");
        FinancialLedger refund = FinancialLedger.builder().ledgerType(FinancialLedgerType.FULL_REFUND).amount(new BigDecimal("200000")).build();

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(18)).thenReturn(List.of(p1));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(18, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of(refund));

        BigDecimal refundable = financialLedgerService.calculateRefundableCash(order);

        assertThat(refundable).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("UT-UUT11-026: calculateRefundableCash - Trả về successfulCash - refunds")
    void calculateRefundableCash_validPaymentsAndRefunds_returnsRemaining() {
        Order order = order(19);
        Payment p1 = payment(190, order, PaymentType.DEPOSIT.name(), "SUCCESS", "500000");
        Payment p2 = payment(191, order, PaymentType.REMAINING_PAYMENT.name(), "SUCCESS", "500000");
        FinancialLedger refund = FinancialLedger.builder().ledgerType(FinancialLedgerType.FULL_REFUND).amount(new BigDecimal("200000")).build();

        when(paymentRepository.findByOrderOrderIdOrderByPaymentIdAsc(19)).thenReturn(List.of(p1, p2));
        when(financialLedgerRepository.findByOrderOrderIdAndLedgerStatus(19, FinancialLedgerStatus.RECORDED))
                .thenReturn(List.of(refund));

        BigDecimal refundable = financialLedgerService.calculateRefundableCash(order);

        assertThat(refundable).isEqualByComparingTo("800000"); // 1M - 200k
    }

    // =========================================================================
    // Group 7: getLedgerHistory & toDTO Mapping
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-027: getLedgerHistory - orderId = null -> return List.of()")
    void getLedgerHistory_nullOrderId_returnsEmptyList() {
        List<FinancialLedgerDTO> result = financialLedgerService.getLedgerHistory(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT-UUT11-028: getLedgerHistory - Dữ liệu đầy đủ -> map DTO và nhãn tiếng Việt")
    void getLedgerHistory_validData_returnsMappedDTOs() {
        Order order = order(20);
        Payment payment = payment(200, order, PaymentType.FULL_PAYMENT.name(), "SUCCESS", "1000000");
        User actor = moderator(99);

        FinancialLedger ledger = FinancialLedger.builder()
                .financialLedgerId(1)
                .order(order)
                .relatedPayment(payment)
                .recordedBy(actor)
                .ledgerType(FinancialLedgerType.COMPLETED_ORDER_REVENUE)
                .direction(FinancialLedgerDirection.INCOME)
                .amount(new BigDecimal("1000000"))
                .faultParty(FaultParty.CUSTOMER)
                .reason("Order finished")
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .recognizedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(financialLedgerRepository.findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(20))
                .thenReturn(List.of(ledger));

        List<FinancialLedgerDTO> dtos = financialLedgerService.getLedgerHistory(20);

        assertThat(dtos).hasSize(1);
        FinancialLedgerDTO dto = dtos.get(0);
        assertEquals(1, dto.getFinancialLedgerId());
        assertEquals(20, dto.getOrderId());
        assertEquals(200, dto.getRelatedPaymentId());
        assertEquals(99, dto.getRecordedById());
        assertEquals("Moderator 99", dto.getRecordedByName());
        assertEquals(FinancialLedgerType.COMPLETED_ORDER_REVENUE, dto.getLedgerType());
        assertNotNull(dto.getLedgerTypeLabel());
        assertNotNull(dto.getDirectionLabel());
    }

    @Test
    @DisplayName("UT-UUT11-029: getLedgerHistory - Quan hệ null (order, payment, recordedBy null) -> map DTO an toàn")
    void getLedgerHistory_nullRelations_mapsSafelyWithoutNPE() {
        FinancialLedger nullRelationLedger = FinancialLedger.builder()
                .financialLedgerId(2)
                .order(null)
                .relatedPayment(null)
                .recordedBy(null)
                .ledgerType(FinancialLedgerType.FULL_REFUND)
                .direction(FinancialLedgerDirection.OUTFLOW)
                .amount(new BigDecimal("100000"))
                .ledgerStatus(FinancialLedgerStatus.RECORDED)
                .build();

        when(financialLedgerRepository.findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(21))
                .thenReturn(List.of(nullRelationLedger));

        List<FinancialLedgerDTO> dtos = financialLedgerService.getLedgerHistory(21);

        assertThat(dtos).hasSize(1);
        FinancialLedgerDTO dto = dtos.get(0);
        assertNull(dto.getOrderId());
        assertNull(dto.getRelatedPaymentId());
        assertNull(dto.getRecordedById());
        assertNull(dto.getRecordedByName());
    }

    // =========================================================================
    // Group 8: calculateCompletedOrderRevenue & calculateTreeAmount
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-030: calculateCompletedOrderRevenue - shippingFee/craneFee = null -> quy đổi ZERO")
    void calculateCompletedOrderRevenue_nullFees_handlesAsZero() {
        Order order = order(22);
        order.setShippingFee(null);
        order.setCraneFee(null);
        order.setOrderDetails(List.of(detail(order, "300000", 1)));

        BigDecimal total = financialLedgerService.calculateCompletedOrderRevenue(order);

        assertThat(total).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("UT-UUT11-031: calculateTreeAmount - orderDetails null hoặc rỗng -> return ZERO")
    void calculateTreeAmount_nullOrEmptyDetails_returnsZero() {
        assertThat(financialLedgerService.calculateTreeAmount(null)).isEqualByComparingTo("0");

        Order nullDetailsOrder = Order.builder().orderDetails(null).build();
        assertThat(financialLedgerService.calculateTreeAmount(nullDetailsOrder)).isEqualByComparingTo("0");

        Order emptyDetailsOrder = Order.builder().orderDetails(List.of()).build();
        assertThat(financialLedgerService.calculateTreeAmount(emptyDetailsOrder)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("UT-UUT11-032: calculateTreeAmount - detail = null hoặc quantity = null -> mặc định quantity = 1")
    void calculateTreeAmount_nullDetailOrQuantity_defaultsQuantityToOne() {
        Order order = order(23);
        OrderDetail nullQtyDetail = OrderDetail.builder().priceAtPurchase(new BigDecimal("200000")).quantity(null).build();
        OrderDetail validDetail = detail(order, "100000", 2);

        List<OrderDetail> details = new ArrayList<>();
        details.add(nullQtyDetail);
        details.add(validDetail);
        details.add(null);

        order.setOrderDetails(details);

        BigDecimal treeAmount = financialLedgerService.calculateTreeAmount(order);

        assertThat(treeAmount).isEqualByComparingTo("400000"); // 200k*1 + 100k*2 + 0
    }

    // =========================================================================
    // Group 9: Repository Exception Propagation
    // =========================================================================

    @Test
    @DisplayName("UT-UUT11-033: financialLedgerRepository.save ném DataAccessException -> Lan truyền exception")
    void recordCompletedOrderRevenueIfAbsent_repositoryThrowsException_propagatesError() {
        Order order = order(24);
        User actor = moderator(99);

        when(financialLedgerRepository.existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(
                24, FinancialLedgerType.COMPLETED_ORDER_REVENUE, FinancialLedgerStatus.RECORDED))
                .thenReturn(false);
        when(financialLedgerRepository.save(any())).thenThrow(new DataAccessException("DB Save Failed") {});

        assertThrows(DataAccessException.class, () -> financialLedgerService.recordCompletedOrderRevenueIfAbsent(order, actor, null));
    }

    // =========================================================================
    // Helper Fixture Builders
    // =========================================================================

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
