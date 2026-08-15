package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [ENTITY ĐẠI DIỆN BẢNG ORDER TRONG CSDL - ĐƠN HÀNG]
 *
 * Bảng CSDL tương ứng: `ORDER`
 *
 * Mô tả nghiệp vụ:
 * - Lưu trữ toàn bộ thông tin đơn hàng mua cây Bonsai trực tuyến (ONLINE) hoặc tại vườn (IN_PERSON).
 * - Quản lý trạng thái đơn hàng qua các giai đoạn:
 *   + PENDING: Mới tạo, chờ Moderator tiếp nhận và duyệt.
 *   + PENDING_PAYMENT: Moderator đã duyệt phí & tiền cọc, chờ khách thanh toán qua VNPay.
 *   + DEPOSITED: Khách đã thanh toán tiền cọc thành công, chờ giao cây và thu nốt đợt 2.
 *   + PAID: Khách đã thanh toán 100% qua VNPay, chờ giao cây.
 *   + COMPLETED: Đơn hàng hoàn tất (đã thu đủ 100% tiền và cây đã giao cho khách).
 *   + CANCELLED: Đơn bị từ chối/hủy/quá hạn/khách bùng cọc/hoàn tiền lỗi.
 */
@Entity
@Table(name = "`ORDER`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /** Khóa chính tự tăng (OrderID). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    /** Khách hàng đặt mua (User entity, nullable nếu là Guest Checkout). */
    @ManyToOne
    @JoinColumn(name = "CustomerID")
    private User customer;

    /** Mã đơn hàng sinh ngẫu nhiên định dạng duy nhất, ví dụ: BSMS-ABC123. */
    @Column(name = "OrderCode", nullable = false, unique = true, length = 100)
    private String orderCode;

    /** Tên người nhận hàng. */
    @Column(name = "CustomerName", length = 255)
    private String customerName;

    /** Số điện thoại nhận hàng (10 chữ số). */
    @Column(name = "CustomerPhone", length = 20)
    private String customerPhone;

    /** Email nhận hóa đơn, thông tin đơn hàng và link thanh toán. */
    @Column(name = "CustomerEmail", length = 255)
    private String customerEmail;

    /** Địa chỉ giao hàng chi tiết. */
    @Column(name = "ShippingAddress", length = 500)
    private String shippingAddress;

    /** Thời điểm đặt hàng. */
    @Column(name = "OrderDate")
    private LocalDateTime orderDate = LocalDateTime.now();

    /** Tổng giá trị đơn hàng thực tế = Tổng giá các cây + Phí cẩu + Phí vận chuyển. */
    @Column(name = "TotalAmount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /** Số tiền đặt cọc cần thanh toán đợt 1 (nếu là flow DEPOSIT). */
    @Column(name = "DepositAmount", precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    /** Trạng thái đơn hàng: PENDING, PENDING_PAYMENT, DEPOSITED, PAID, COMPLETED, CANCELLED. */
    @Column(name = "OrderStatus", length = 50)
    private String orderStatus = "PENDING";

    /** Loại đơn hàng: ONLINE (mua trực tuyến) hoặc IN_PERSON (mua trực tiếp tại vườn). */
    @Column(name = "OrderType", length = 50, nullable = false)
    private String orderType = "ONLINE";

    /** Phí cẩu cây nặng đặc thù (áp sau khi Moderator duyệt). */
    @Column(name = "CraneFee", precision = 15, scale = 2)
    private BigDecimal craneFee = BigDecimal.ZERO;

    /** Phí vận chuyển cây (áp sau khi Moderator duyệt). */
    @Column(name = "ShippingFee", precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /** Ghi chú giao hàng hoặc lý do hủy/hoàn tiền. */
    @Column(name = "Notes", length = 500)
    private String notes;

    /** Order Moderator đang phụ trách xử lý đơn này. */
    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    /** Thời điểm Moderator nhận xử lý đơn. */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    /** Thời điểm đơn hàng hoàn tất (COMPLETED). */
    @Column(name = "CompletedAt")
    private LocalDateTime completedAt;

    /** Khóa lạc quan (Optimistic Locking) chống ghi đè đồng thời. */
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
