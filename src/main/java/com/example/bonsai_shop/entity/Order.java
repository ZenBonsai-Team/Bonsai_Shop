package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "`ORDER`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @ManyToOne
    @JoinColumn(name = "CustomerID")
    private User customer;

    @Column(name = "OrderCode", nullable = false, unique = true, length = 100)
    private String orderCode;

    @Column(name = "CustomerName", length = 255)
    private String customerName;

    @Column(name = "CustomerPhone", length = 20)
    private String customerPhone;

    @Column(name = "CustomerEmail", length = 255)
    private String customerEmail;

    @Column(name = "ShippingAddress", length = 500)
    private String shippingAddress;

    @Column(name = "OrderDate")
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "TotalAmount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "DepositAmount", precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "OrderStatus", length = 50)
    private String orderStatus = "PENDING";

    @Column(name = "OrderType", length = 50, nullable = false)
    private String orderType = "ONLINE";

    /**
     * Phương thức thanh toán khách đã chọn lúc checkout.
     * Giá trị: "DEPOSIT" hoặc "FULL"
     *
     * Dùng bởi Moderator để biết flow nào khi approve:
     *   - DEPOSIT → form nhập depositAmount + craneFee + shippingFee → tạo Payment DEPOSIT qua VNPay
     *   - FULL    → form chỉ cần craneFee + shippingFee → tạo Payment FULL_PAYMENT qua VNPay
     *
     * Khác với orderType (ONLINE/OFFLINE): paymentMethod là về cách trả tiền,
     * orderType là về kênh bán hàng.
     */
    @Column(name = "PaymentMethod", length = 50)
    private String paymentMethod;

    @Column(name = "CraneFee", precision = 15, scale = 2)
    private BigDecimal craneFee = BigDecimal.ZERO;

    @Column(name = "ShippingFee", precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "Notes", length = 500)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Version
    @Column(name = "version")
    private Integer version = 0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    /**
     * Danh sách tất cả Payment của đơn hàng này.
     *
     * Quan hệ 1-N: 1 Order có thể có nhiều Payment records:
     *   - Flow Đặt Cọc:    Payment #1 DEPOSIT (VNPay) + Payment #2 REMAINING_PAYMENT (Cash)
     *   - Flow Thanh Đủ:   Payment #1 FULL_PAYMENT (VNPay)
     *
     * cascade=ALL: khi xóa Order thì xóa Payment liên quan (ON DELETE CASCADE đã có ở DB)
     * orphanRemoval=true: Payment không còn thuộc Order nào sẽ bị xóa khỏi DB
     *
     * TRƯỚC ĐÂY: @OneToOne — bị giới hạn 1 Payment/Order
     * SAU KHI REFACTOR: @OneToMany — không giới hạn
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderLog> orderLogs;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderHandling> orderHandlings;
}
