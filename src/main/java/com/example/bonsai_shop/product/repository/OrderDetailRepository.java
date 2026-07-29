package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    @Query("""
            SELECT COALESCE(SUM(od.priceAtPurchase), 0)
            FROM OrderDetail od
            WHERE od.product.createdBy.userId = :artisanUserId
              AND od.order.orderDate >= :startDate
              AND od.order.orderDate < :endDate
              AND UPPER(COALESCE(od.order.orderStatus, '')) = 'PAID'
            """)
    BigDecimal sumMonthlyRevenueByArtisan(@Param("artisanUserId") Integer artisanUserId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(od)
            FROM OrderDetail od
            WHERE od.product.createdBy.userId = :artisanUserId
              AND od.order.orderDate >= :startDate
              AND od.order.orderDate < :endDate
              AND UPPER(COALESCE(od.order.orderStatus, '')) = 'PAID'
            """)
    long countMonthlySoldItemsByArtisan(@Param("artisanUserId") Integer artisanUserId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COALESCE(SUM(od.priceAtPurchase), 0)
            FROM OrderDetail od
            WHERE od.order.orderDate >= :startDate
              AND od.order.orderDate < :endDate
              AND UPPER(COALESCE(od.order.orderStatus, '')) = 'PAID'
            """)
    BigDecimal sumMonthlyRevenue(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(od)
            FROM OrderDetail od
            WHERE od.order.orderDate >= :startDate
              AND od.order.orderDate < :endDate
              AND UPPER(COALESCE(od.order.orderStatus, '')) = 'PAID'
            """)
    long countMonthlySoldItems(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
}
