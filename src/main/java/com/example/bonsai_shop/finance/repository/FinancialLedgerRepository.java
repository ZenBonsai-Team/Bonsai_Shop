package com.example.bonsai_shop.finance.repository;

import com.example.bonsai_shop.entity.FinancialLedger;
import com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO;
import com.example.bonsai_shop.finance.enums.FinancialLedgerStatus;
import com.example.bonsai_shop.finance.enums.FinancialLedgerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialLedgerRepository extends JpaRepository<FinancialLedger, Integer> {

    List<FinancialLedger> findByOrderOrderIdOrderByRecognizedAtAscFinancialLedgerIdAsc(Integer orderId);

    List<FinancialLedger> findByOrderOrderIdAndLedgerStatus(Integer orderId, FinancialLedgerStatus ledgerStatus);

    boolean existsByOrderOrderIdAndLedgerTypeAndLedgerStatus(Integer orderId,
                                                            FinancialLedgerType ledgerType,
                                                            FinancialLedgerStatus ledgerStatus);

    Optional<FinancialLedger> findFirstByOrderOrderIdAndLedgerTypeAndLedgerStatus(Integer orderId,
                                                                                  FinancialLedgerType ledgerType,
                                                                                  FinancialLedgerStatus ledgerStatus);

    boolean existsByRelatedPaymentPaymentIdAndLedgerTypeAndLedgerStatus(Integer relatedPaymentId,
                                                                        FinancialLedgerType ledgerType,
                                                                        FinancialLedgerStatus ledgerStatus);

    @Query("""
            SELECT COALESCE(SUM(od.priceAtPurchase * od.quantity), 0)
            FROM OrderDetail od
            WHERE od.product.createdBy.userId = :artisanUserId
              AND EXISTS (
                  SELECT 1
                  FROM FinancialLedger fl
                  WHERE fl.order = od.order
                    AND fl.ledgerType = :ledgerType
                    AND fl.ledgerStatus = :ledgerStatus
                    AND fl.recognizedAt >= :startDate
                    AND fl.recognizedAt < :endDate
              )
            """)
    BigDecimal sumCompletedProductRevenueByArtisan(@Param("artisanUserId") Integer artisanUserId,
                                                   @Param("ledgerType") FinancialLedgerType ledgerType,
                                                   @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COALESCE(SUM(o.shippingFee), 0)
            FROM Order o
            WHERE EXISTS (
                  SELECT 1
                  FROM FinancialLedger fl
                  WHERE fl.order = o
                    AND fl.ledgerType IN :ledgerTypes
                    AND fl.ledgerStatus = :ledgerStatus
                    AND fl.recognizedAt >= :startDate
                    AND fl.recognizedAt < :endDate
              )
              AND EXISTS (
                  SELECT 1
                  FROM OrderDetail od
                  WHERE od.order = o
                    AND od.product.createdBy.userId = :artisanUserId
              )
            """)
    BigDecimal sumShippingFeeByArtisanAndLedgerTypes(@Param("artisanUserId") Integer artisanUserId,
                                                     @Param("ledgerTypes") List<FinancialLedgerType> ledgerTypes,
                                                     @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COALESCE(SUM(o.craneFee), 0)
            FROM Order o
            WHERE EXISTS (
                  SELECT 1
                  FROM FinancialLedger fl
                  WHERE fl.order = o
                    AND fl.ledgerType IN :ledgerTypes
                    AND fl.ledgerStatus = :ledgerStatus
                    AND fl.recognizedAt >= :startDate
                    AND fl.recognizedAt < :endDate
              )
              AND EXISTS (
                  SELECT 1
                  FROM OrderDetail od
                  WHERE od.order = o
                    AND od.product.createdBy.userId = :artisanUserId
              )
            """)
    BigDecimal sumCraneFeeByArtisanAndLedgerTypes(@Param("artisanUserId") Integer artisanUserId,
                                                  @Param("ledgerTypes") List<FinancialLedgerType> ledgerTypes,
                                                  @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COALESCE(SUM(fl.amount), 0)
            FROM FinancialLedger fl
            WHERE fl.ledgerType = :ledgerType
              AND fl.ledgerStatus = :ledgerStatus
              AND fl.recognizedAt >= :startDate
              AND fl.recognizedAt < :endDate
              AND EXISTS (
                  SELECT 1
                  FROM OrderDetail od
                  WHERE od.order = fl.order
                    AND od.product.createdBy.userId = :artisanUserId
              )
            """)
    BigDecimal sumLedgerAmountByArtisan(@Param("artisanUserId") Integer artisanUserId,
                                        @Param("ledgerType") FinancialLedgerType ledgerType,
                                        @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(od)
            FROM OrderDetail od
            WHERE od.product.createdBy.userId = :artisanUserId
              AND EXISTS (
                  SELECT 1
                  FROM FinancialLedger fl
                  WHERE fl.order = od.order
                    AND fl.ledgerType = :ledgerType
                    AND fl.ledgerStatus = :ledgerStatus
                    AND fl.recognizedAt >= :startDate
                    AND fl.recognizedAt < :endDate
              )
            """)
    long countCompletedSoldItemsByArtisan(@Param("artisanUserId") Integer artisanUserId,
                                          @Param("ledgerType") FinancialLedgerType ledgerType,
                                          @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT new com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO(
                od.order.orderCode,
                od.order.customerName,
                fl.recognizedAt,
                od.order.orderStatus,
                od.product.productName,
                od.priceAtPurchase * od.quantity,
                od.order.depositAmount,
                null,
                od.order.shippingFee,
                od.order.craneFee,
                od.priceAtPurchase * od.quantity
            )
            FROM OrderDetail od
            JOIN FinancialLedger fl ON fl.order = od.order
            WHERE od.product.createdBy.userId = :artisanUserId
              AND fl.ledgerType = :ledgerType
              AND fl.ledgerStatus = :ledgerStatus
              AND fl.recognizedAt >= :startDate
              AND fl.recognizedAt < :endDate
            ORDER BY fl.recognizedAt DESC, od.order.orderId DESC, od.orderDetailId DESC
            """)
    List<ArtisanDashboardSourceDTO> findCompletedRevenueSourcesByArtisan(
            @Param("artisanUserId") Integer artisanUserId,
            @Param("ledgerType") FinancialLedgerType ledgerType,
            @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT new com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO(
                fl.order.orderCode,
                fl.order.customerName,
                fl.recognizedAt,
                fl.order.orderStatus,
                null,
                null,
                fl.amount,
                null,
                fl.order.shippingFee,
                fl.order.craneFee,
                fl.amount
            )
            FROM FinancialLedger fl
            WHERE fl.ledgerType = :ledgerType
              AND fl.ledgerStatus = :ledgerStatus
              AND fl.recognizedAt >= :startDate
              AND fl.recognizedAt < :endDate
              AND EXISTS (
                  SELECT 1
                  FROM OrderDetail od
                  WHERE od.order = fl.order
                    AND od.product.createdBy.userId = :artisanUserId
              )
            ORDER BY fl.recognizedAt DESC, fl.order.orderId DESC, fl.financialLedgerId DESC
            """)
    List<ArtisanDashboardSourceDTO> findForfeitedDepositSourcesByArtisan(
            @Param("artisanUserId") Integer artisanUserId,
            @Param("ledgerType") FinancialLedgerType ledgerType,
            @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT new com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO(
                fl.order.orderCode,
                fl.order.customerName,
                fl.recognizedAt,
                fl.order.orderStatus,
                null,
                null,
                fl.order.depositAmount,
                fl.amount,
                fl.order.shippingFee,
                fl.order.craneFee,
                fl.amount
            )
            FROM FinancialLedger fl
            WHERE fl.ledgerType = :ledgerType
              AND fl.ledgerStatus = :ledgerStatus
              AND fl.recognizedAt >= :startDate
              AND fl.recognizedAt < :endDate
              AND EXISTS (
                  SELECT 1
                  FROM OrderDetail od
                  WHERE od.order = fl.order
                    AND od.product.createdBy.userId = :artisanUserId
              )
            ORDER BY fl.recognizedAt DESC, fl.order.orderId DESC, fl.financialLedgerId DESC
            """)
    List<ArtisanDashboardSourceDTO> findRefundSourcesByArtisan(
            @Param("artisanUserId") Integer artisanUserId,
            @Param("ledgerType") FinancialLedgerType ledgerType,
            @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT new com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO(
                o.orderCode,
                o.customerName,
                MAX(fl.recognizedAt),
                o.orderStatus,
                null,
                null,
                o.depositAmount,
                null,
                o.shippingFee,
                o.craneFee,
                o.shippingFee
            )
            FROM Order o
            JOIN FinancialLedger fl ON fl.order = o
            WHERE fl.ledgerType IN :ledgerTypes
              AND fl.ledgerStatus = :ledgerStatus
              AND fl.recognizedAt >= :startDate
              AND fl.recognizedAt < :endDate
              AND o.shippingFee IS NOT NULL
              AND o.shippingFee > 0
              AND EXISTS (
                  SELECT 1
                  FROM OrderDetail od
                  WHERE od.order = o
                    AND od.product.createdBy.userId = :artisanUserId
              )
            GROUP BY o.orderId, o.orderCode, o.customerName, o.orderStatus, o.depositAmount, o.shippingFee, o.craneFee
            ORDER BY MAX(fl.recognizedAt) DESC, o.orderId DESC
            """)
    List<ArtisanDashboardSourceDTO> findShippingFeeSourcesByArtisan(
            @Param("artisanUserId") Integer artisanUserId,
            @Param("ledgerTypes") List<FinancialLedgerType> ledgerTypes,
            @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT new com.example.bonsai_shop.finance.dto.ArtisanDashboardSourceDTO(
                o.orderCode,
                o.customerName,
                MAX(fl.recognizedAt),
                o.orderStatus,
                null,
                null,
                o.depositAmount,
                null,
                o.shippingFee,
                o.craneFee,
                o.craneFee
            )
            FROM Order o
            JOIN FinancialLedger fl ON fl.order = o
            WHERE fl.ledgerType IN :ledgerTypes
              AND fl.ledgerStatus = :ledgerStatus
              AND fl.recognizedAt >= :startDate
              AND fl.recognizedAt < :endDate
              AND o.craneFee IS NOT NULL
              AND o.craneFee > 0
              AND EXISTS (
                  SELECT 1
                  FROM OrderDetail od
                  WHERE od.order = o
                    AND od.product.createdBy.userId = :artisanUserId
              )
            GROUP BY o.orderId, o.orderCode, o.customerName, o.orderStatus, o.depositAmount, o.shippingFee, o.craneFee
            ORDER BY MAX(fl.recognizedAt) DESC, o.orderId DESC
            """)
    List<ArtisanDashboardSourceDTO> findCraneFeeSourcesByArtisan(
            @Param("artisanUserId") Integer artisanUserId,
            @Param("ledgerTypes") List<FinancialLedgerType> ledgerTypes,
            @Param("ledgerStatus") FinancialLedgerStatus ledgerStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
