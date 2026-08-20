package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Order;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * [REPOSITORY TRUY VẤN DỮ LIỆU ĐƠN HÀNG - ORDER REPOSITORY]
 *
 * Chịu trách nhiệm:
 * - Thao tác CRUD và thực hiện các câu truy vấn phức tạp trên bảng ORDER (đặt hàng, phân trang, tìm kiếm, tính KPI).
 * - Eager fetching (LEFT JOIN FETCH) quan hệ OrderDetail và Product để tránh lỗi LazyInitializationException và N+1 queries.
 * - Quét các đơn hàng hết hạn (Expired Online/Offline/In-Person Orders).
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    /**
     * [TÌM ĐƠN HÀNG THEO MÃ ĐƠN (ORDER CODE)]
     */
    Optional<Order> findByOrderCode(String orderCode);

    /**
     * [TÌM ĐƠN HÀNG THEO MÃ ĐƠN KÈM CHI TIẾT SẢN PHẨM VÀ GIÁ]
     *
     * Mục đích: Eager load toàn bộ OrderDetail và Product để phục vụ hiển thị chi tiết và tính toán tiền.
     * Được gọi từ: OrderService.getOrderByCodeWithDetails(), OrderService.verifyOrder(), OrderEventListener.
     * Câu lệnh: LEFT JOIN FETCH orderDetails od LEFT JOIN FETCH od.product
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE LOWER(o.orderCode) = LOWER(:orderCode)")
    Optional<Order> findByOrderCodeWithDetails(@Param("orderCode") String orderCode);

    /**
     * [TÌM ĐƠN HÀNG VỚI KHÓA BI QUAN (PESSIMISTIC WRITE LOCK)]
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithDetailsForUpdate(@Param("orderId") Integer orderId);

    /**
     * [ĐẾM SỐ ĐƠN HÀNG THEO TRẠNG THÁI]
     */
    long countByOrderStatus(String orderStatus);

    /**
     * [LẤY DANH SÁCH ĐƠN HÀNG CỦA KHÁCH HÀNG KÈM CHI TIẾT SẢN PHẨM]
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE o.customer.userId = :customerId ORDER BY o.orderDate DESC")
    List<Order> findByCustomerUserIdWithDetailsOrderByOrderDateDesc(@Param("customerId") Integer customerId);

    /**
     * [ĐẾM SỐ ĐƠN ĐƯỢC GÁN CHO MODERATOR]
     */
    long countByAssignedToUserId(Integer moderatorId);

    /**
     * [ĐẾM SỐ ĐƠN ĐƯỢC GÁN CHO MODERATOR THEO TRẠNG THÁI]
     */
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

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN o.orderDetails od
            JOIN od.product p
            JOIN p.createdBy a
            WHERE a.userId = :artisanUserId
              AND o.orderType = :orderType
              AND (:status = 'ALL' OR o.orderStatus = :status)
              AND (:keyword IS NULL OR :keyword = '' OR
                   LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY o.orderDate DESC
            """)
    Page<Order> searchByArtisanUserIdAndTypeAndStatus(
            @Param("artisanUserId") Integer artisanUserId,
            @Param("orderType") String orderType,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN o.orderDetails od
            JOIN od.product p
            JOIN p.createdBy a
            JOIN a.role r
            WHERE LOWER(o.orderType) = LOWER(:orderType)
              AND UPPER(r.roleName) IN ('ARTISAN', 'ROLE_ARTISAN')
              AND (:status = 'ALL' OR o.orderStatus = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY o.orderDate DESC
            """)
    Page<Order> searchOwnerInPersonOrders(
            @Param("orderType") String orderType,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    /**
     * [TÌM KIẾM ĐƠN HÀNG TỔNG HỢP CHO MODERATOR THEO DANH SÁCH TRẠNG THÁI VÀ TỪ KHÓA]
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE (:statuses IS NULL OR o.orderStatus IN :statuses) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrdersForModerator(
            @Param("statuses") List<String> statuses,
            @Param("search") String search,
            Pageable pageable);

    /**
     * [TRUY VẤN KHO ĐƠN HÀNG CHUNG (ORDERS POOL)]
     *
     * Mục đích: Tìm các đơn hàng chưa ai nhận (assignedTo IS NULL) và đang ở trạng thái 'PENDING'.
     * Được gọi từ: OrderService.getPoolOrders()
     */
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

    /**
     * [TRUY VẤN ĐƠN HÀNG CỦA MODERATOR HIỆN TẠI (MY ORDERS)]
     *
     * Mục đích: Tìm các đơn hàng được gán cho moderatorId cụ thể.
     * Được gọi từ: OrderService.getMyOrders(), MyOrderService.
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE o.assignedTo.userId = :moderatorId AND " +
            "(:statuses IS NULL OR o.orderStatus IN :statuses) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchMyOrders(
            @Param("moderatorId") Integer moderatorId,
            @Param("statuses") List<String> statuses,
            @Param("search") String search,
            Pageable pageable);

    /**
     * [QUÉT ĐƠN HÀNG ONLINE QUÁ HẠN 15 PHÚT (EXPIRED ONLINE ORDERS)]
     *
     * Mục đích: Tìm các đơn ONLINE ở trạng thái PENDING hoặc PENDING_PAYMENT có thời gian tạo hoặc thời gian gán <= cutoffTime (now - 15 phút).
     * Được gọi từ: OrderExpirationService.cancelExpiredOrders()
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE (o.orderStatus = 'PENDING' OR o.orderStatus = 'PENDING_PAYMENT') AND LOWER(o.orderType) = 'online' AND (o.assignedAt <= :cutoffTime OR (o.assignedAt IS NULL AND o.orderDate <= :cutoffTime))")
    List<Order> findExpiredOnlineOrders(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);


    /**
     * [QUÉT ĐƠN HÀNG IN_PERSON QUÁ HẠN (EXPIRED IN-PERSON ORDERS)]
     *
     * Được gọi từ: OrderExpirationService.cancelExpiredInPersonOrders()
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.product WHERE (o.orderStatus = 'PENDING' OR o.orderStatus = 'PENDING_PAYMENT') AND LOWER(o.orderType) = 'in_person' AND o.orderDate <= :cutoffTime")
    List<Order> findExpiredInPersonOrders(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderDetails od WHERE o.customer.userId = :customerId AND o.orderStatus = :orderStatus AND od.product.productId = :productId")
    boolean existsByCustomerUserIdAndOrderStatusAndOrderDetails_Product_ProductId(
            @Param("customerId") Integer customerId,
            @Param("orderStatus") String orderStatus,
            @Param("productId") Integer productId);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderDetails od " +
           "WHERE o.customer.userId = :customerId " +
           "AND o.orderStatus = 'COMPLETED' " +
           "AND od.product.productId = :productId " +
           "AND o.completedAt >= :sinceTime")
    boolean existsEligibleOrderForReview(
            @Param("customerId") Integer customerId,
            @Param("productId") Integer productId,
            @Param("sinceTime") java.time.LocalDateTime sinceTime);

    @Query("SELECT o FROM Order o WHERE o.orderStatus = 'COMPLETED' AND o.completedAt <= :cutoffTime")
    List<Order> findCompletedOrdersBefore(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);
}
