package com.example.bonsai_shop.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bonsai_shop.entity.OrderHandling;

@Repository
public interface OrderHandlingRepository extends JpaRepository<OrderHandling, Integer> {
    List<OrderHandling> findByOrderOrderIdOrderByHandledAtDesc(Integer orderId);
}
