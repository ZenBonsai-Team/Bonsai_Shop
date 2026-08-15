package com.example.bonsai_shop.owner.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
// DTO gom du lieu mot dong lich su moderator xu ly don cho Owner.
public class OrderHandlingHistoryDTO {
    // Id cua ban ghi phan cong/xu ly don.
    private Integer handlingId;
    // Ma don hang duoc xu ly.
    private String orderCode;
    // Ten khach hang trong don.
    private String customerName;
    // Email khach hang trong don.
    private String customerEmail;
    // Ten moderator da/ dang phu trach.
    private String moderatorName;
    // Email moderator da/ dang phu trach.
    private String moderatorEmail;
    // Ngay tao don hang.
    private LocalDateTime orderDate;
    // Tong tien don hang.
    private BigDecimal totalAmount;
    // Trang thai ban ghi xu ly, vi du dang phu trach/da ban giao.
    private String status;
    // Trang thai nghiep vu cua don hang.
    private String orderStatus;
    // Thoi diem moderator nhan xu ly.
    private LocalDateTime handledAt;
    // Thoi diem moderator ban giao/ket thuc xu ly.
    private LocalDateTime releasedAt;
}
