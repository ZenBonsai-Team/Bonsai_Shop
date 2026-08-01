package com.example.bonsai_shop.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyOrderKPIsDTO {
    private long criticalCount;
    private long waitingApprovalCount;
    private long waitingPaymentCount;
    private long waitingDeliveryCount;
    private long completedCount;
    private long cancelledCount;
}
