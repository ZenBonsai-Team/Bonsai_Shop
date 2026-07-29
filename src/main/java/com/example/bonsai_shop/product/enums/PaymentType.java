package com.example.bonsai_shop.product.enums;

/**
 * Loại giao dịch thanh toán trong một đơn hàng.
 *
 * Mô hình thanh toán của Bonsai Shop:
 *
 * [DEPOSIT]
 *   Khách chọn "Đặt cọc trước".
 *   Moderator approve → tạo Payment #1 loại DEPOSIT qua VNPay.
 *   Amount = depositAmount + craneFee + shippingFee
 *   Sau khi VNPay callback thành công → Order.status = DEPOSITED
 *
 * [FULL_PAYMENT]
 *   Khách chọn "Thanh toán toàn bộ một lần".
 *   Moderator approve → tạo Payment #1 loại FULL_PAYMENT qua VNPay.
 *   Amount = treePrice + craneFee + shippingFee = Order.TotalAmount
 *   Sau khi VNPay callback thành công → Order.status = PAID, Product = SOLD
 *
 * [REMAINING_PAYMENT]
 *   Chỉ xuất hiện sau DEPOSIT đã thành công.
 *   Moderator bấm "Xác nhận đã thu đủ tiền" → tạo Payment #2 loại REMAINING_PAYMENT.
 *   PaymentMethod = CASH (tiền mặt hoặc chuyển khoản trực tiếp, không qua VNPay)
 *   Amount = Order.TotalAmount - depositPayment.Amount (phần còn lại)
 *   PaymentStatus = SUCCESS ngay lập tức (Moderator xác nhận thủ công)
 *   Sau khi lưu → Order.status = PAID, Product = SOLD
 *
 * Thiết kế: Dùng String trong DB thay vì @Enumerated(EnumType.ORDINAL)
 *   → Dễ đọc khi debug, không bị ảnh hưởng khi thêm value mới vào enum
 */
public enum PaymentType {

    /**
     * Thanh toán tiền đặt cọc (lần đầu, qua VNPay).
     * = depositAmount + craneFee + shippingFee
     */
    DEPOSIT,

    /**
     * Thanh toán toàn bộ một lần (qua VNPay).
     * = treePrice + craneFee + shippingFee
     */
    FULL_PAYMENT,

    /**
     * Thanh toán phần còn lại sau khi đã đặt cọc (thủ công, tiền mặt/chuyển khoản).
     * = treePrice - depositAmount
     * Được ghi nhận bởi Moderator, PaymentStatus = SUCCESS ngay lập tức.
     */
    REMAINING_PAYMENT
}
