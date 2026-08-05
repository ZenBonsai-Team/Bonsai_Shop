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
    long countByCreatedByUserId(Integer artisanUserId);
    List<Product> findByCreatedByUserIdAndProductStatusOrderByCreatedAtDesc(Integer artisanUserId, String productStatus);
    Optional<Product> findByProductIdAndCreatedByUserId(Integer productId, Integer artisanUserId);
    boolean existsByProductCode(String productCode);
    Optional<Product> findByProductCode(String productCode);
    boolean existsByVarietyCategoryCategoryId(Integer categoryId);
    boolean existsByVarietyVarietyId(Integer varietyId);
    List<Product> findTop5ByProductStatusAndIsVisibleTrueOrderByViewCountDesc(String productStatus);
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
          AND COALESCE(p.isVisible, true) = true
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
                AND COALESCE(p.isVisible, true) = true
                AND (m.isThumbnail = true OR m IS NULL)
    """)
    Page<ProductCardDTO> findMarketplaceProducts(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.variety LEFT JOIN FETCH p.createdBy WHERE COALESCE(p.isVisible, true) = true")
    Page<Product> findAllActiveProducts(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.variety LEFT JOIN FETCH p.createdBy WHERE p.productStatus = 'AVAILABLE' AND COALESCE(p.isVisible, true) = true")
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
      AND p.productStatus <> 'DRAFT'
      AND COALESCE(p.isVisible, true) = true
      AND (
          m.mediaId = (
              SELECT MIN(mt.mediaId)
              FROM ProductMedia mt
              WHERE mt.product = p
                AND mt.isThumbnail = true
          )
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
      AND p.productStatus <> 'DRAFT'
      AND COALESCE(p.isVisible, true) = true
      AND p.productId = :productId
      AND (
          m.mediaId = (
              SELECT MIN(mt.mediaId)
              FROM ProductMedia mt
              WHERE mt.product = p
                AND mt.isThumbnail = true
          )
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
