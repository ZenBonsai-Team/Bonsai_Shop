package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE LOWER(o.orderCode) = LOWER(:orderCode)")
    Optional<Order> findByOrderCodeWithDetails(@Param("orderCode") String orderCode);

    long countByOrderStatus(String orderStatus);

    // Truy vấn lịch sử đơn hàng của người mua (Customer)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE o.customer.userId = :customerId ORDER BY o.orderDate DESC")
    List<Order> findByCustomerUserIdWithDetailsOrderByOrderDateDesc(@Param("customerId") Integer customerId);

    // Đếm số đơn thuộc về Moderator cụ thể
    long countByAssignedToUserId(Integer moderatorId);
    long countByAssignedToUserIdAndOrderStatus(Integer moderatorId, String orderStatus);

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN o.orderDetails od
            JOIN od.product p
            JOIN p.createdBy a
            WHERE a.userId = :artisanUserId
              AND o.orderType = :orderType
              AND (:status = 'ALL' OR o.orderStatus = :status)
            ORDER BY o.orderDate DESC
            """)
    Page<Order> findByArtisanUserIdAndTypeAndStatus(
            @Param("artisanUserId") Integer artisanUserId,
            @Param("orderType") String orderType,
            @Param("status") String status,
            Pageable pageable);


    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE (:status = 'ALL' OR o.orderStatus = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrdersForModerator(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    // 1. Orders Pool Query (Chưa có ai nhận & đang PENDING)
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE o.assignedTo IS NULL AND o.orderStatus = 'PENDING' AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrdersPool(
            @Param("search") String search,
            Pageable pageable);

    // 2. My Orders Query (Được gán cho Moderator cụ thể)
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE o.assignedTo.userId = :moderatorId AND " +
            "(:status = 'ALL' OR o.orderStatus = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchMyOrders(
            @Param("moderatorId") Integer moderatorId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    // 3. Expired Orders Queries (Online 15m & Offline 48h)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE (o.orderStatus = 'PENDING' OR o.orderStatus = 'APPROVED') AND LOWER(o.orderType) = 'online' AND (o.assignedAt <= :cutoffTime OR (o.assignedAt IS NULL AND o.orderDate <= :cutoffTime))")
    List<Order> findExpiredOnlineOrders(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE (o.orderStatus = 'PENDING' OR o.orderStatus = 'APPROVED') AND (o.depositAmount IS NULL OR o.depositAmount = 0) AND (o.orderType IS NULL OR LOWER(o.orderType) != 'online') AND o.orderDate <= :cutoffTime")
    List<Order> findExpiredOfflineOrders(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);
}

