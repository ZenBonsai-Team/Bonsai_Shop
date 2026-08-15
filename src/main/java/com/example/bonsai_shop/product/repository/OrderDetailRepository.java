package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.OrderDetail;
import com.example.bonsai_shop.owner.dto.OwnerArtisanRevenueDTO;
import com.example.bonsai_shop.owner.dto.OwnerSoldTreeDTO;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    @Query("""
            SELECT new com.example.bonsai_shop.owner.dto.OwnerSoldTreeDTO(
                od.product.productId,
                od.product.productCode,
                od.product.productName,
                od.product.variety.varietyName,
                artisan.fullName,
                od.order.orderCode,
                od.order.customerName,
                od.priceAtPurchase * od.quantity,
                COALESCE(od.order.completedAt, od.order.orderDate)
            )
            FROM OrderDetail od
            LEFT JOIN od.product.createdBy artisan
            WHERE UPPER(COALESCE(od.order.orderStatus, '')) = 'COMPLETED'
              AND UPPER(COALESCE(od.product.productStatus, '')) = 'SOLD'
            ORDER BY COALESCE(od.order.completedAt, od.order.orderDate) DESC, od.order.orderId DESC
            """)
    List<OwnerSoldTreeDTO> findOwnerSoldTrees();

    @Query("""
            SELECT new com.example.bonsai_shop.owner.dto.OwnerArtisanRevenueDTO(
                artisan.userId,
                COALESCE(artisan.fullName, 'Chưa gán nghệ nhân'),
                COALESCE(artisan.email, '-'),
                COALESCE(SUM(od.quantity), 0),
                COALESCE(SUM(od.priceAtPurchase * od.quantity), 0)
            )
            FROM OrderDetail od
            LEFT JOIN od.product.createdBy artisan
            WHERE EXISTS (
                SELECT 1
                FROM FinancialLedger fl
                WHERE fl.order = od.order
                  AND fl.ledgerType = :ledgerType
                  AND fl.ledgerStatus = :ledgerStatus
                  AND fl.recognizedAt >= :startDate
                  AND fl.recognizedAt < :endDate
            )
            GROUP BY artisan.userId, artisan.fullName, artisan.email
            ORDER BY COALESCE(SUM(od.priceAtPurchase * od.quantity), 0) DESC, artisan.fullName ASC
            """)
    List<OwnerArtisanRevenueDTO> findCurrentMonthRevenueByArtisan(@Param("ledgerType") FinancialLedgerType ledgerType,
                                                                  @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
                                                                  @Param("startDate") LocalDateTime startDate,
                                                                  @Param("endDate") LocalDateTime endDate);
}
