package com.example.bonsai_shop.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Integer orderId;
    private String orderCode;
    private CustomerDTO customer;
    private ProductDTO product;
    private Integer quantity;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private LocalDateTime orderDate;
    private String orderStatus;
    private BigDecimal craneFee;
    private BigDecimal shippingFee;
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDTO {
        private String name;
        private String phone;
        private String email;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO {
        private Integer id;
        private String name;
        private String image;
        private BigDecimal price;
    }
}
