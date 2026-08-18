package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * [ENTITY ĐẠI DIỆN BẢNG ORDER_HANDLING TRONG CSDL - PHIÊN XỬ LÝ ĐƠN CỦA
 * MODERATOR]
 *
 * Bảng CSDL tương ứng: `ORDER_HANDLING`
 *
 * Mô tả:
 * - Lưu vết các phiên làm việc của từng Order Moderator khi claim đơn từ Orders
 * Pool.
 * - Quản lý trạng thái đang giữ đơn (isActive = true), thời gian bắt đầu nhận
 * (handledAt) và thời gian giải phóng/bàn giao (releasedAt).
 */
@Entity
@Table(name = "ORDER_HANDLING")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHandling {

    /** Khóa chính tự tăng (OrderHandlingID). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderHandlingID")
    private Integer orderHandlingId;

    /** Đơn hàng đang được xử lý (ManyToOne). */
    @ManyToOne
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    /** Moderator phụ trách phiên xử lý này (ManyToOne). */
    @ManyToOne
    @JoinColumn(name = "ModeratorOrderID")
    private User moderator;

    /** Thời điểm bắt đầu nhận đơn (Claim). */
    @Builder.Default
    @Column(name = "HandledAt")
    private LocalDateTime handledAt = LocalDateTime.now();

    /** Thời điểm kết thúc/giải phóng đơn (Unclaim / Complete / Cancel). */
    @Column(name = "ReleasedAt")
    private LocalDateTime releasedAt;

    /** Đánh dấu phiên xử lý có đang hoạt động hay không. */
    @Builder.Default
    @Column(name = "IsActive")
    private Boolean isActive = true;
}