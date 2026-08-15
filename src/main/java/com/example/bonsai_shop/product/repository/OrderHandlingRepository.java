package com.example.bonsai_shop.product.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.bonsai_shop.entity.OrderHandling;

/**
 * [REPOSITORY TRUY VẤN LỊCH SỬ TIẾP NHẬN & BÀN GIAO ĐƠN HÀNG - ORDER HANDLING REPOSITORY]
 *
 * Chịu trách nhiệm:
 * - Lưu vết các phiên làm việc của Moderator đối với từng đơn hàng (Tiếp nhận claim, bàn giao unclaim, thời gian bắt đầu handledAt, thời gian kết thúc releasedAt, trạng thái active).
 */
@Repository
public interface OrderHandlingRepository extends JpaRepository<OrderHandling, Integer> {
    /**
     * [LẤY LỊCH SỬ XỬ LÝ CỦA MỘT ĐƠN HÀNG SẮP XẾP THEO THỜI GIAN GIẢM DẦN]
     */
    List<OrderHandling> findByOrderOrderIdOrderByHandledAtDesc(Integer orderId);

    @Query(value = """
            SELECT h
            FROM OrderHandling h
            JOIN h.order o
            JOIN h.moderator m
            JOIN m.role r
            WHERE UPPER(r.roleName) IN ('MODERATOR', 'ROLE_MODERATOR')
              AND UPPER(o.orderStatus) IN ('COMPLETED', 'CANCELLED')
              AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR UPPER(o.orderStatus) = :status)
              AND h.orderHandlingId = (
                  SELECT MAX(h2.orderHandlingId)
                  FROM OrderHandling h2
                  WHERE h2.order = h.order
                    AND h2.moderator = h.moderator
              )
              AND (:search IS NULL OR :search = ''
                   OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.orderStatus) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.email) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY h.handledAt DESC, h.orderHandlingId DESC
            """,
            countQuery = """
            SELECT COUNT(h)
            FROM OrderHandling h
            JOIN h.order o
            JOIN h.moderator m
            JOIN m.role r
            WHERE UPPER(r.roleName) IN ('MODERATOR', 'ROLE_MODERATOR')
              AND UPPER(o.orderStatus) IN ('COMPLETED', 'CANCELLED')
              AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR UPPER(o.orderStatus) = :status)
              AND h.orderHandlingId = (
                  SELECT MAX(h2.orderHandlingId)
                  FROM OrderHandling h2
                  WHERE h2.order = h.order
                    AND h2.moderator = h.moderator
              )
              AND (:search IS NULL OR :search = ''
                   OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.orderStatus) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<OrderHandling> findModeratorHandlingHistory(@Param("search") String search,
                                                     @Param("status") String status,
                                                     Pageable pageable);
}
