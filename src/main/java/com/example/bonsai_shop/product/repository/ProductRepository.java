package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.product.dto.ProductCardDTO;
import com.example.bonsai_shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
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
                u.fullName,
                p.productStatus,
                m.mediaUrl
        )
        FROM Product p
        JOIN p.variety v
        JOIN p.seller u
        LEFT JOIN p.productMedias m
        WHERE
                p.isPublicPrice = true
                AND p.productStatus = 'AVAILABLE'
                AND (m.isThumbnail = true OR m IS NULL)
    """)
    Page<ProductCardDTO> findMarketplaceProducts(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.variety JOIN FETCH p.seller WHERE p.isPublicPrice = true")
    Page<Product> findAllActiveProducts(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.variety JOIN FETCH p.seller WHERE p.isPublicPrice = true AND p.productStatus = 'AVAILABLE'")
    Page<Product> findAvailableProductsOnly(Pageable pageable);

    //---------
    //Prenium Bonsai
    //---------

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
            u.fullName,
            p.productStatus,
            m.mediaUrl
    )
    FROM Product p
    JOIN p.variety v
    JOIN p.seller u
    LEFT JOIN p.productMedias m
    WHERE p.segment.segmentId = 2
      AND p.isPublicPrice = false
      AND (m.isThumbnail = true OR m IS NULL)
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
            u.fullName,
            p.productStatus,
            m.mediaUrl
    )
    FROM Product p
    JOIN p.variety v
    JOIN p.seller u
    LEFT JOIN p.productMedias m
    WHERE p.segment.segmentId = 2
      AND p.isPublicPrice = false
      AND p.productId = :productId
      AND (m.isThumbnail = true OR m IS NULL)
""")
    ProductCardDTO findPremiumProductById(@Param("productId") Integer productId);
}


