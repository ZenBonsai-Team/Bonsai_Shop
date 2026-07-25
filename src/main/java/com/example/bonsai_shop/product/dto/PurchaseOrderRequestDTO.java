package com.example.bonsai_shop.product.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PurchaseOrderRequestDTO {

    private Integer productId;
    private java.util.List<Integer> productIds;

    @NotBlank(message = "Tên khách hàng không được trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^(0|\\+84)(\\d{9})$", message = "Số điện thoại không hợp lệ (Cần 10 chữ số)")
    private String customerPhone;

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không hợp lệ")
    private String customerEmail;

    @NotBlank(message = "Địa chỉ giao hàng không được trống")
    private String shippingAddress;

    @NotBlank(message = "Phương thức thanh toán không được trống")
    private String paymentMethod;

}
