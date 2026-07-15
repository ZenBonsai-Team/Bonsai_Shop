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
            WHERE od.product.seller.userId = :sellerId
              AND od.order.orderDate >= :startDate
              AND od.order.orderDate < :endDate
              AND UPPER(COALESCE(od.order.orderStatus, '')) = 'COMPLETED'
            """)
    BigDecimal sumMonthlyRevenueBySeller(@Param("sellerId") Integer sellerId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(od)
            FROM OrderDetail od
            WHERE od.product.seller.userId = :sellerId
              AND od.order.orderDate >= :startDate
              AND od.order.orderDate < :endDate
              AND UPPER(COALESCE(od.order.orderStatus, '')) = 'COMPLETED'
            """)
    long countMonthlySoldItemsBySeller(@Param("sellerId") Integer sellerId,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}
