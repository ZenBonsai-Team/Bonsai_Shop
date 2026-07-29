package com.example.bonsai_shop.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body từ Moderator khi approve một đơn hàng.
 *
 * Moderator bắt buộc nhập: craneFee, shippingFee.
 * Nếu paymentMethod = "DEPOSIT": Moderator cũng nhập depositAmount.
 * Nếu paymentMethod = "FULL": depositAmount bỏ trống (sẽ bị ignore ở service).
 *
 * Sau khi nhận request này, OrderService.verifyOrder() sẽ:
 *   1. Tính totalAmount mới = treePrice + craneFee + shippingFee
 *   2. Tạo Payment record (DEPOSIT hoặc FULL_PAYMENT)
 *   3. Cập nhật Order.status = APPROVED
 *   4. Ghi OrderLog
 */
@Data
public class ApproveOrderRequestDTO {

    /**
     * Phí cẩu hàng (đơn vị: VNĐ). Bắt buộc.
     * Ví dụ: 500000 (500,000 VNĐ)
     */
    @DecimalMin(value = "0", message = "Phí cẩu không được âm")
    private BigDecimal craneFee = BigDecimal.ZERO;

    /**
     * Phí vận chuyển (đơn vị: VNĐ). Bắt buộc.
     */
    @DecimalMin(value = "0", message = "Phí vận chuyển không được âm")
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /**
     * Số tiền đặt cọc (đơn vị: VNĐ).
     * Chỉ bắt buộc khi Order.paymentMethod = "DEPOSIT".
     * Service sẽ ignore nếu paymentMethod = "FULL".
     *
     * Ý nghĩa: phần tiền cây mà khách đặt cọc trước.
     * Không bao gồm craneFee và shippingFee (hai phí này đã cộng thêm).
     */
    @DecimalMin(value = "0", message = "Tiền đặt cọc không được âm")
    private BigDecimal depositAmount;

    /**
     * Ghi chú tùy chọn từ Moderator (lý do, điều kiện đặc biệt, v.v.)
     */
    private String notes;
}
