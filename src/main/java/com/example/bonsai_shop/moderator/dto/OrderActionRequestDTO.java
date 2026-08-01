package com.example.bonsai_shop.moderator.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderActionRequestDTO {
    private String action;
    private String reason;
    private BigDecimal craneFee;
    private BigDecimal shippingFee;
    private BigDecimal depositAmount;
}
