package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * [ENTITY ĐẠI DIỆN BẢNG ORDER_DETAIL TRONG CSDL - CHI TIẾT ĐƠN HÀNG]
 *
 * Bảng CSDL tương ứng: `ORDER_DETAIL`
 *
 * Mô tả:
 * - Lưu trữ từng cây Bonsai có trong đơn hàng.
 * - Lưu snapshot giá bán tại thời điểm khách bấm đặt hàng (priceAtPurchase) để đảm bảo giá không bị biến động nếu giá cây sau này thay đổi.
 */
@Entity
@Table(name = "ORDER_DETAIL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetail {

    /** Khóa chính tự tăng (OrderDetailID). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderDetailID")
    private Integer orderDetailId;

    /** Đơn hàng chứa sản phẩm này (ManyToOne). */
    @ManyToOne
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    /** Tác phẩm Bonsai được mua (ManyToOne). */
    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    /** Giá mua tại thời điểm đặt hàng (Snapshot price). */
    @Column(name = "PriceAtPurchase", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAtPurchase;

    /** Số lượng sản phẩm (mặc định 1 đối với bonsai độc bản). */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;
}