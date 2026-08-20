package com.example.bonsai_shop.product.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * [DTO TIẾP NHẬN YÊU CẦU ĐẶT HÀNG TRỰC TUYẾN - PURCHASE ORDER REQUEST DTO]
 *
 * Mục đích:
 * - Đóng gói dữ liệu người dùng gửi lên từ form Checkout giỏ hàng hoặc mua ngay 1 cây.
 *
 * Được sử dụng tại:
 * - OrderApiController.checkout() (POST /api/orders/checkout)
 * - OrderService.createOrder()
 */
@Data
public class PurchaseOrderRequestDTO {

    /** ID sản phẩm duy nhất (nếu mua ngay). */
    private Integer productId;

    /** Danh sách ID sản phẩm (nếu thanh toán nhiều cây từ giỏ hàng). */
    private java.util.List<Integer> productIds;

    /** Mã OTP xác thực (bắt buộc đối với khách vãng lai Guest Checkout). */
    private String otpCode;

    /** Ghi chú đơn hàng từ khách. */
    @Size(max = 400, message = "Ghi chú không được vượt quá 400 ký tự")
    private String notes;

    /** Họ tên người nhận hàng. */
    @NotBlank(message = "Tên khách hàng không được trống")
    @Size(min = 3, max = 50, message = "Họ và tên người nhận phải có từ 3 đến 50 ký tự")
    private String customerName;

    /** Số điện thoại nhận hàng (bắt buộc đúng 10 số). */
    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^(0|\\+84)(\\d{9})$", message = "Số điện thoại không hợp lệ (Cần 10 chữ số)")
    private String customerPhone;

    /** Email nhận thông tin và link thanh toán. */
    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không hợp lệ")
    private String customerEmail;

    /** Địa chỉ nhận cây chi tiết. */
    @NotBlank(message = "Địa chỉ giao hàng không được trống")
    @Size(max = 255, message = "Địa chỉ nhận không được vượt quá 255 ký tự")
    private String shippingAddress;

    /** Phương thức thanh toán lựa chọn: "DEPOSIT" (đặt cọc trước) hoặc "FULL_PAYMENT" (thanh toán đủ 100%). */
    @NotBlank(message = "Phương thức thanh toán không được trống")
    private String paymentMethod;

}
