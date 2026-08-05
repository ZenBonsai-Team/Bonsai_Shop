package com.example.bonsai_shop.product.enums;

/**
 * Phương thức thanh toán được hỗ trợ trong hệ thống.
 *
 * Thiết kế theo nguyên tắc mở rộng (Open for Extension):
 *   - Thêm phương thức mới chỉ cần thêm value vào enum này
 *   - Không cần thay đổi DB schema (cột PaymentMethod là VARCHAR)
 *   - Không cần thay đổi business logic đã có
 *
 * Khả năng mở rộng:
 *   - MOMO, ZALOPAY → thêm value + tạo service tương ứng
 *   - INSTALLMENT (trả góp) → thêm value + tạo PaymentSchedule entity
 *   - REFUND → xử lý hoàn tiền, tạo Payment record âm
 */
public enum PaymentMethod {

    /**
     * Thanh toán qua cổng VNPay.
     * Dùng cho: DEPOSIT và FULL_PAYMENT.
     * Flow: Backend tạo URL → Redirect khách → VNPay callback → cập nhật hệ thống.
     */
    VNPAY,

    /**
     * Thanh toán tiền mặt (hoặc chuyển khoản ngân hàng trực tiếp không qua cổng).
     * Dùng cho: REMAINING_PAYMENT — Moderator xác nhận thủ công.
     * Không có callback từ bên ngoài.
     */
    CASH,

    /**
     * Chuyển khoản ngân hàng trực tiếp (để mở rộng sau).
     * Tách riêng với CASH để phân biệt rõ phương thức.
     * Hiện tại chưa sử dụng.
     */
    BANK_TRANSFER,

    /**
     * Đặt cọc trước (thanh toán khoản cọc trực tuyến/bằng phương thức cọc, phần còn lại thu khi nhận cây).
     */
    DEPOSIT
}
