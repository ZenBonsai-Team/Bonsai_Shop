package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Wishlist;
import com.example.bonsai_shop.entity.WishlistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, WishlistId> {
    @Query(value = """
            SELECT *
            FROM wishlist
            WHERE CustomerID = :customerId
            ORDER BY CreatedAt DESC
            """, nativeQuery = true)
    List<Wishlist> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Integer customerId);

    @Query(value = """
            SELECT COUNT(*)
            FROM wishlist
            WHERE CustomerID = :customerId
              AND ProductID = :productId
            """, nativeQuery = true)
    long countByCustomerIdAndProductId(
            @Param("customerId") Integer customerId,
            @Param("productId") Integer productId);

    @Query(value = """
            SELECT COUNT(*)
            FROM wishlist
            WHERE CustomerID = :customerId
            """, nativeQuery = true)
    long countByCustomerId(@Param("customerId") Integer customerId);

    @Modifying
    @Query(value = """
            DELETE FROM wishlist
            WHERE CustomerID = :customerId
              AND ProductID = :productId
            """, nativeQuery = true)
    void deleteByCustomerIdAndProductId(
            @Param("customerId") Integer customerId,
            @Param("productId") Integer productId);
}
