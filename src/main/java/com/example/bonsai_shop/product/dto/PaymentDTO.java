package com.example.bonsai_shop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO để trả về thông tin Payment trong API response.
 *
 * Dùng trong:
 *   - OrderResponseDTO.payments (danh sách tất cả Payment của đơn hàng)
 *   - Hiển thị lịch sử thanh toán trên trang Order Detail
 *
 * Không expose entity Payment trực tiếp để:
 *   1. Tránh circular reference (Payment → Order → Payment...)
 *   2. Kiểm soát field nào được trả về FE
 *   3. Dễ thay đổi response format mà không ảnh hưởng DB schema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    /** ID của payment record */
    private Integer paymentId;

    /**
     * Loại giao dịch: "DEPOSIT", "FULL_PAYMENT", "REMAINING_PAYMENT"
     * Xem: PaymentType enum
     */
    private String paymentType;

    /**
     * Phương thức: "VNPAY", "CASH", "BANK_TRANSFER"
     * Xem: PaymentMethod enum
     */
    private String paymentMethod;

    /**
     * Trạng thái: "PENDING", "SUCCESS", "FAILED"
     */
    private String paymentStatus;

    /**
     * Số tiền của giao dịch này (VNĐ).
     */
    private BigDecimal amount;

    /**
     * Thời điểm thanh toán thành công.
     * Null nếu PaymentStatus = PENDING.
     */
    private LocalDateTime paymentDate;

    /**
     * Ghi chú từ Moderator.
     * Thường có giá trị khi paymentType = REMAINING_PAYMENT.
     */
    private String notes;
}
