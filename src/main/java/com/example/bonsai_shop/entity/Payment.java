package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho một giao dịch thanh toán.
 *
 * QUAN HỆ: Nhiều Payment → Một Order (ManyToOne)
 *   Lý do: Business rule Bonsai Shop yêu cầu 1 Order có thể có nhiều Payment records:
 *   - Payment #1: DEPOSIT (đặt cọc qua VNPay)
 *   - Payment #2: REMAINING_PAYMENT (phần còn lại, tiền mặt, Moderator xác nhận)
 *   Hoặc:
 *   - Payment #1: FULL_PAYMENT (thanh toán đủ qua VNPay)
 *
 *   Trước đây quan hệ là @OneToOne (UNIQUE constraint trên OrderID).
 *   Migration V3 đã bỏ UNIQUE constraint và chuyển thành @ManyToOne.
 *
 * PaymentType và PaymentMethod được lưu dưới dạng String trong DB (không dùng @Enumerated).
 *   → Dễ đọc khi debug SQL, không bị lỗi khi thêm enum value mới.
 *   → Các giá trị hợp lệ: xem enum PaymentType và PaymentMethod trong package enums.
 */
@Entity
@Table(name = "PAYMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    private Integer paymentId;

    /**
     * Order mà Payment này thuộc về.
     * Quan hệ N-1: nhiều Payment có thể cùng OrderID.
     * KHÔNG còn unique=true — DB constraint đã được bỏ ở migration V3.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    /**
     * Phương thức thanh toán: "VNPAY", "CASH", "BANK_TRANSFER"
     * Xem enum: PaymentMethod
     */
    @Column(name = "PaymentMethod", length = 100)
    private String paymentMethod;

    /**
     * Trạng thái giao dịch: "PENDING", "SUCCESS", "FAILED"
     * - PENDING: đã tạo Payment, chờ khách thanh toán / Moderator xác nhận
     * - SUCCESS: đã thanh toán thành công
     * - FAILED: giao dịch thất bại (VNPay trả về lỗi)
     */
    @Column(name = "PaymentStatus", length = 50)
    private String paymentStatus = "PENDING";

    /**
     * Loại giao dịch: "DEPOSIT", "FULL_PAYMENT", "REMAINING_PAYMENT"
     * Xem enum: PaymentType
     */
    @Column(name = "PaymentType", length = 100)
    private String paymentType;

    /**
     * Số tiền của giao dịch này (đơn vị: VNĐ).
     * Với DEPOSIT: = depositAmount + craneFee + shippingFee
     * Với FULL_PAYMENT: = treePrice + craneFee + shippingFee
     * Với REMAINING_PAYMENT: = treePrice - depositAmount (phần còn lại)
     */
    @Column(name = "Amount", precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Thời điểm thanh toán thành công.
     * Null khi PaymentStatus = PENDING.
     * Được set khi VNPay callback thành công hoặc Moderator xác nhận thủ công.
     */
    @Column(name = "PaymentDate")
    private LocalDateTime paymentDate;

    /**
     * Ghi chú từ Moderator (thường dùng cho REMAINING_PAYMENT).
     * Ví dụ: "Khách chuyển khoản lúc 10:30, mã GD: 123456"
     */
    @Column(name = "Notes", length = 500)
    private String notes;
}