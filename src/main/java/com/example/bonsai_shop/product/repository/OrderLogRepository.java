package com.example.bonsai_shop.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bonsai_shop.entity.OrderLog;

import java.util.List;

/**
 * [REPOSITORY TRUY VẤN NHẬT KÝ BIẾN ĐỘNG TRẠNG THÁI ĐƠN HÀNG - ORDER LOG REPOSITORY]
 *
 * Chịu trách nhiệm:
 * - Lưu vết lịch sử thay đổi trạng thái (Audit Log), người thực hiện (actionBy), loại hành động (actionType: VERIFY, REJECT, DEPOSIT, REMAINING_PAYMENT_CONFIRMED, ORDER_COMPLETED...), trạng thái trước (fromStatus), trạng thái sau (toStatus), thời điểm (actionAt).
 */
@Repository
public interface OrderLogRepository extends JpaRepository<OrderLog, Integer> {
    /**
     * [LẤY TOÀN BỘ NHẬT KÝ BIẾN ĐỘNG CỦA ĐƠN HÀNG THEO THỜI GIAN TĂNG DẦN (TIMELINE)]
     */
    List<OrderLog> findByOrderOrderIdOrderByActionAtAsc(Integer orderId);
}
