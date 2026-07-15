package com.example.bonsai_shop.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bonsai_shop.entity.OrderLog;

@Repository
public interface OrderLogRepository extends JpaRepository<OrderLog, Integer> {
}
