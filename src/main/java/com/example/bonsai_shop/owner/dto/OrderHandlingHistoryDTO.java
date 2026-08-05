package com.example.bonsai_shop.owner.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderHandlingHistoryDTO {
    private Integer handlingId;
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private String moderatorName;
    private String moderatorEmail;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private String status;
    private String orderStatus;
    private LocalDateTime handledAt;
    private LocalDateTime releasedAt;
}
