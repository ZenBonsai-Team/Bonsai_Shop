package com.example.bonsai_shop.owner.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
// DTO gom du lieu mot dong lich su don tai vuon cho man hinh Owner.
public class InPersonOrderHistoryDTO {
    // Id noi bo cua don hang.
    private Integer orderId;
    // Ma don hien thi cho nguoi dung.
    private String orderCode;
    // Ten khach hang dat don tai vuon.
    private String customerName;
    // So dien thoai khach hang, dung de Owner tra cuu nhanh.
    private String customerPhone;
    // Ten artisan/nhan su phu trach san pham trong don.
    private String handlerName;
    // Email artisan/nhan su phu trach.
    private String handlerEmail;
    // Ten cay/san pham chinh trong don.
    private String productName;
    // Tong gia tri don hang.
    private BigDecimal totalAmount;
    // Thoi diem tao don.
    private LocalDateTime orderDate;
    // Trang thai hien tai cua don.
    private String orderStatus;
}
