package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * [ENTITY ĐẠI DIỆN BẢNG ORDER_LOG TRONG CSDL - NHẬT KÝ BIẾN ĐỘNG ĐƠN HÀNG]
 *
 * Bảng CSDL tương ứng: `ORDER_LOG`
 *
 * Mô tả:
 * - Lưu vết toàn bộ lịch sử biến động trạng thái và hành vi xử lý trên đơn hàng phục vụ kiểm toán và hiển thị Timeline tiến trình.
 */
@Entity
@Table(name = "ORDER_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLog {

    /** Khóa chính tự tăng (OrderLogID). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderLogID")
    private Integer orderLogId;

    /** Đơn hàng ghi nhận nhật ký (ManyToOne). */
    @ManyToOne
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    /** Người thực hiện hành động (User: Customer, Moderator, hoặc null nếu do Hệ thống/Scheduler). */
    @ManyToOne
    @JoinColumn(name = "ActionByID", nullable = false)
    private User actionBy;

    /** Loại hành động (VERIFY, REJECT, DEPOSIT, REMAINING_PAYMENT_CONFIRMED, ORDER_COMPLETED, v.v.). */
    @Column(name = "ActionType", length = 100)
    private String actionType;

    /** Trạng thái đơn trước khi thực hiện hành động. */
    @Column(name = "FromStatus", length = 50)
    private String fromStatus;

    /** Trạng thái đơn sau khi thực hiện hành động. */
    @Column(name = "ToStatus", length = 50)
    private String toStatus;

    /** Thời điểm thực hiện hành động. */
    @Column(name = "ActionAt")
    private LocalDateTime actionAt = LocalDateTime.now();
}