package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductTag;
import com.example.bonsai_shop.entity.ProductTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, ProductTagId> {
    List<ProductTag> findByProduct(Product product);
    void deleteByProduct(Product product);

    @Query("select count(productTag) > 0 from ProductTag productTag where productTag.tag.tagId = :tagId")
    boolean existsForTagId(@Param("tagId") Integer tagId);

    long countByTagTagId(Integer tagId);
}
