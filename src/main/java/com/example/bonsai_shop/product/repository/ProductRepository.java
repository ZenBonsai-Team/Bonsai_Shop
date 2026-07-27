package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.product.dto.ProductCardDTO;
import com.example.bonsai_shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    List<Product> findByCreatedByUserIdOrderByCreatedAtDesc(Integer artisanUserId);
    List<Product> findByCreatedByUserIdAndProductStatusOrderByCreatedAtDesc(Integer artisanUserId, String productStatus);
    Optional<Product> findByProductIdAndCreatedByUserId(Integer productId, Integer artisanUserId);
    boolean existsByProductCode(String productCode);
    boolean existsByVarietyVarietyId(Integer varietyId);
    boolean existsBySegmentSegmentId(Integer segmentId);
    List<Product> findTop5ByProductStatusOrderByViewCountDesc(String productStatus);
   @Modifying
    @Query("""
        UPDATE Product p
        SET p.viewCount = COALESCE(p.viewCount, 0) + 1
        WHERE p.productId = :productId
    """)
    int incrementViewCount(@Param("productId") Integer productId);


    @Modifying
    @Query("""
        UPDATE Product p
        SET p.productStatus = 'RESERVED'
        WHERE p.productId = :productId
          AND p.productStatus = 'AVAILABLE'
    """)
    int reserveIfAvailable(@Param("productId") Integer productId);

    @Query("""
        SELECT new com.example.bonsai_shop.product.dto.ProductCardDTO(
                p.productId,
                p.productCode,
                p.productName,
                v.varietyName,
                p.age,
                p.height,
                p.trunkDiameter,
                p.price,
                a.fullName,
                p.productStatus,
                m.mediaUrl
        )
        FROM Product p
        JOIN p.variety v
        JOIN p.createdBy a
        LEFT JOIN p.productMedias m
        WHERE
                p.productStatus = 'AVAILABLE'
                AND (m.isThumbnail = true OR m IS NULL)
    """)
    Page<ProductCardDTO> findMarketplaceProducts(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.variety LEFT JOIN FETCH p.createdBy WHERE p.productStatus <> 'HIDDEN'")
    Page<Product> findAllActiveProducts(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.variety LEFT JOIN FETCH p.createdBy WHERE p.productStatus = 'AVAILABLE'")
    Page<Product> findAvailableProductsOnly(Pageable pageable);

    @Query("""
    SELECT new com.example.bonsai_shop.product.dto.ProductCardDTO(
            p.productId,
            p.productCode,
            p.productName,
            v.varietyName,
            p.age,
            p.height,
            p.trunkDiameter,
            p.price,
            a.fullName,
            p.productStatus,
            m.mediaUrl
    )
    FROM Product p
    JOIN p.variety v
    JOIN p.createdBy a
    LEFT JOIN p.productMedias m
    WHERE p.segment.segmentId = 3
      AND p.productStatus NOT IN ('DRAFT', 'HIDDEN')
      AND (
          m.isThumbnail = true
          OR (
              m.mediaId = (
                  SELECT MIN(m2.mediaId)
                  FROM ProductMedia m2
                  WHERE m2.product = p
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM ProductMedia m3
                  WHERE m3.product = p
                    AND m3.isThumbnail = true
              )
          )
          OR m IS NULL
      )
""")
    Page<ProductCardDTO> findPremiumProducts(Pageable pageable);

    @Query("""
    SELECT new com.example.bonsai_shop.product.dto.ProductCardDTO(
            p.productId,
            p.productCode,
            p.productName,
            v.varietyName,
            p.age,
            p.height,
            p.trunkDiameter,
            p.price,
            a.fullName,
            p.productStatus,
            m.mediaUrl
    )
    FROM Product p
    JOIN p.variety v
    JOIN p.createdBy a
    LEFT JOIN p.productMedias m
    WHERE p.segment.segmentId = 3
      AND p.productStatus NOT IN ('DRAFT', 'HIDDEN')
      AND p.productId = :productId
      AND (
          m.isThumbnail = true
          OR (
              m.mediaId = (
                  SELECT MIN(m2.mediaId)
                  FROM ProductMedia m2
                  WHERE m2.product = p
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM ProductMedia m3
                  WHERE m3.product = p
                    AND m3.isThumbnail = true
              )
          )
          OR m IS NULL
      )
""")
    ProductCardDTO findPremiumProductById(@Param("productId") Integer productId);
}
