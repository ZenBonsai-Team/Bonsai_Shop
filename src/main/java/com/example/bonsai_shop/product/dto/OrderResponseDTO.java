package com.example.bonsai_shop.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.bonsai_shop.product.dto.PaymentDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [DTO PHẢN HỒI THÔNG TIN ĐƠN HÀNG - ORDER RESPONSE DTO]
 *
 * Mục đích:
 * - Đóng gói dữ liệu đơn hàng trả về cho Frontend hiển thị danh sách đơn, chi tiết đơn, và trạng thái thanh toán.
 */
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

    /**
     * Phương thức thanh toán khách chọn lúc checkout: "DEPOSIT" hoặc "FULL".
     * Dùng để hiển thị label đúng trên giao diện Moderator.
     */
    private String paymentMethod;

    /**
     * Giá trị gốc của các cây trong đơn hàng (không bao gồm phụ phí).
     */
    private BigDecimal treePrice;

    /**
     * Số tiền khách cần thanh toán ngay qua VNPay (Nấc 1):
     *  - Đặt cọc: depositAmount + craneFee + shippingFee
     *  - Thanh toán 100%: totalAmount (treePrice + craneFee + shippingFee)
     */
    private BigDecimal immediatePaymentAmount;

    /**
     * Số tiền khách sẽ thanh toán khi nhận cây (Nấc 2):
     *  - Đặt cọc: treePrice - depositAmount
     *  - Thanh toán 100%: 0 VND
     */
    private BigDecimal remainingPaymentAmount;

    /**
     * Lịch sử thanh toán của đơn hàng (có thể có nhiều records).
     * Ví dụ: [DEPOSIT/PENDING, REMAINING_PAYMENT/SUCCESS]
     */
    private List<PaymentDTO> payments;

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
