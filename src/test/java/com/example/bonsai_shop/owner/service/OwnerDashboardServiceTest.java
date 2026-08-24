package com.example.bonsai_shop.owner.service;

import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import com.example.bonsai_shop.finance.repository.FinancialLedgerRepository;
import com.example.bonsai_shop.product.repository.OrderDetailRepository;
import com.example.bonsai_shop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnerDashboardServiceTest {

    private FinancialLedgerRepository financialLedgerRepository;
    private ProductRepository productRepository;
    private OrderDetailRepository orderDetailRepository;
    private OwnerDashboardService ownerDashboardService;

    @BeforeEach
    void setUp() {
        financialLedgerRepository = mock(FinancialLedgerRepository.class);
        productRepository = mock(ProductRepository.class);
        orderDetailRepository = mock(OrderDetailRepository.class);
        ownerDashboardService = new OwnerDashboardService(
                financialLedgerRepository,
                productRepository,
                orderDetailRepository);
    }

    @Test
    @DisplayName("Dashboard: getMonthlyRefundAmount includes PRODUCT_REFUND_ONLY and FULL_REFUND")
    void getMonthlyRefundAmount_includesProductRefundOnly() {
        YearMonth ym = YearMonth.of(2026, 8);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        when(financialLedgerRepository.sumLedgerAmountByTypes(
                eq(List.of(FinancialLedgerType.FULL_REFUND, FinancialLedgerType.PRODUCT_REFUND_ONLY)),
                eq(FinancialLedgerStatus.RECORDED),
                eq(start),
                eq(end))).thenReturn(new BigDecimal("8000000"));

        BigDecimal refundAmount = ownerDashboardService.getMonthlyRefundAmount(ym);
        assertThat(refundAmount).isEqualByComparingTo("8000000");
    }

    @Test
    @DisplayName("Dashboard: getShippingFeeSources & getCraneFeeSources include PRODUCT_REFUND_ONLY in feeLedgerTypes")
    void getFeeSources_includesProductRefundOnly() {
        YearMonth ym = YearMonth.of(2026, 8);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        when(financialLedgerRepository.sumShippingFeeByLedgerTypes(
                eq(List.of(FinancialLedgerType.COMPLETED_ORDER_REVENUE, FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                        FinancialLedgerType.PRODUCT_REFUND_ONLY)),
                eq(FinancialLedgerStatus.RECORDED),
                eq(start),
                eq(end))).thenReturn(new BigDecimal("1000000"));

        when(financialLedgerRepository.sumCraneFeeByLedgerTypes(
                eq(List.of(FinancialLedgerType.COMPLETED_ORDER_REVENUE, FinancialLedgerType.FORFEITED_DEPOSIT_INCOME,
                        FinancialLedgerType.PRODUCT_REFUND_ONLY)),
                eq(FinancialLedgerStatus.RECORDED),
                eq(start),
                eq(end))).thenReturn(new BigDecimal("500000"));

        BigDecimal shippingFee = ownerDashboardService.getMonthlyShippingFee(ym);
        BigDecimal craneFee = ownerDashboardService.getMonthlyCraneFee(ym);

        assertThat(shippingFee).isEqualByComparingTo("1000000");
        assertThat(craneFee).isEqualByComparingTo("500000");
    }
}
