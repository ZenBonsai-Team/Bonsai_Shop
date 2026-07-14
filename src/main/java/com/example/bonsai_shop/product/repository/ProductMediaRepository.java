package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Integer> {
    List<ProductMedia> findByProductOrderByDisplayOrderAscMediaIdAsc(Product product);
    Optional<ProductMedia> findByMediaIdAndProduct(Integer mediaId, Product product);
}
