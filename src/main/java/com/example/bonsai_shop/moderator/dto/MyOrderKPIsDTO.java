package com.example.bonsai_shop.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [DTO CHỈ SỐ KPI ĐƠN HÀNG CỦA MODERATOR - MY ORDER KPIS DTO]
 *
 * Mục đích:
 * - Đóng gói số lượng đơn hàng theo từng tab KPI: criticalCount (sắp quá hạn), waitingApprovalCount (chờ duyệt), waitingPaymentCount (chờ khách trả tiền), waitingDeliveryCount (chờ giao & thu nốt tiền), completedCount (hoàn thành), cancelledCount (đã hủy).
 */
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
