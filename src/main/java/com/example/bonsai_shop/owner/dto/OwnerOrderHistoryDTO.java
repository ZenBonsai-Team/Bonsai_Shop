package com.example.bonsai_shop.owner.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OwnerOrderHistoryDTO {
    private Integer orderId;
    private String orderCode;
    private String orderType;
    private String customerName;
    private String customerContact;
    private String handlerRoleLabel;
    private String handlerName;
    private String handlerEmail;
    private String productName;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private String orderStatus;
}
