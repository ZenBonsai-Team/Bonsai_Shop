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
}
