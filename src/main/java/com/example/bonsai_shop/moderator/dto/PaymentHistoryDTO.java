package com.example.bonsai_shop.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryDTO {
    private Integer paymentId;
    private int paymentNumber;
    private String method;
    private String paymentType;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdTime;
    private String transactionCode;
    private String vnpayRef;
    private String notes;
}
