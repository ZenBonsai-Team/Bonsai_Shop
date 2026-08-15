package com.example.bonsai_shop.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [DTO HIỂN THỊ ĐƠN HÀNG TRÊN DANH SÁCH MY ORDERS - MODERATOR]
 *
 * Mục đích:
 * - Đóng gói dữ liệu tóm tắt đơn hàng của riêng Moderator đang đăng nhập, kèm chỉ số ưu tiên (Priority), nhãn trạng thái thân thiện (orderStatusLabel), và thời gian tồn đọng (ageFormatted).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyOrderDTO {
    private Integer orderId;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private BigDecimal depositAmount;
    private BigDecimal remainingPaymentAmount;
    private BigDecimal totalAmount;
    private String priority;
    private String priorityLabel;
    private String orderStatus;
    private String orderStatusLabel;
    private String orderType;
    private LocalDateTime statusTimestamp;
    private String ageFormatted;
    private Integer itemCount;
    private String firstProductName;
    private String firstProductImage;
}
