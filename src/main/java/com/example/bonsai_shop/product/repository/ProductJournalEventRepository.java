package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductJournalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductJournalEventRepository extends JpaRepository<ProductJournalEvent, Integer> {
    List<ProductJournalEvent> findByProductOrderByEventDateDescEventIdDesc(Product product);

    List<ProductJournalEvent> findByProductAndIsPublicTrueOrderByEventDateDescEventIdDesc(Product product);

    Optional<ProductJournalEvent> findByEventIdAndProduct(Integer eventId, Product product);

}
