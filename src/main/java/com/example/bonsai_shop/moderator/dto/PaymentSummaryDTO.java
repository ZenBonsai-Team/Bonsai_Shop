package com.example.bonsai_shop.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSummaryDTO {
    private BigDecimal treePrice;
    private BigDecimal shippingFee;
    private BigDecimal craneFee;
    private BigDecimal depositAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingPaymentAmount;
    private BigDecimal grandTotal;
    private BigDecimal orderTotal;
    private BigDecimal successfulDepositAmount;
    private BigDecimal successfulRemainingPaymentAmount;
    private BigDecimal successfulFullPaymentAmount;
    private BigDecimal totalCashReceived;
    private BigDecimal recognizedCompletedRevenue;
    private BigDecimal forfeitedDepositIncome;
    private BigDecimal partialRefundAmount;
    private BigDecimal fullRefundAmount;
    private BigDecimal netRecognizedAmount;
    private BigDecimal refundableCash;
}
