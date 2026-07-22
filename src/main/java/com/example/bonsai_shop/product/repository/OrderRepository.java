package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Order;
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

    long countByOrderStatus(String orderStatus);

    // Đếm số đơn thuộc về Moderator cụ thể
    long countByAssignedToUserId(Integer moderatorId);
    long countByAssignedToUserIdAndOrderStatus(Integer moderatorId, String orderStatus);

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN o.orderDetails od
            JOIN od.product p
            JOIN p.artisan a
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
}

