package com.example.bonsai_shop.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private String orderType;
    private BigDecimal craneFee;
    private BigDecimal shippingFee;
    private String notes;
    private List<OrderItemDTO> items;

    /*
     * --- THÔNG TIN PHÂN BỔ & TIMELINE ---
     * AI ĐỘNG VÀO LÀ CHÓ
     * 
     * Dùng để hiện thị cho bên Order Moderator
     * Check lịch sử orderHandling, xem người xử lý cuối cùng
     */
    private String assignedToUsername;
    private String assignedToFullName;
    private LocalDateTime assignedAt;
    private List<OrderHandlingDTO> handlingHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Integer id;
        private String name;
        private String image;
        private BigDecimal price;
        private Integer quantity;
    }

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderHandlingDTO {
        private Integer handlingId;
        private String moderatorUsername;
        private String moderatorFullName;
        private LocalDateTime handledAt;
        private LocalDateTime releasedAt;
        private Boolean isActive;
    }

}
