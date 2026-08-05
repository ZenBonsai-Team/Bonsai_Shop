package com.example.bonsai_shop.owner.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InPersonOrderHistoryDTO {
    private Integer orderId;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String handlerName;
    private String handlerEmail;
    private String productName;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private String orderStatus;
}
