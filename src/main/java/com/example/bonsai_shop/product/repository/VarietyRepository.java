package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Variety;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VarietyRepository extends JpaRepository<Variety, Integer> {
    boolean existsByCategoryCategoryId(Integer categoryId);
}
