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
    private String faultParty;
    private BigDecimal refundAmount;
    private String evidenceNote;
    private String externalReference;
    private Boolean customerKeepsTree;
    private String productResolution;
}
