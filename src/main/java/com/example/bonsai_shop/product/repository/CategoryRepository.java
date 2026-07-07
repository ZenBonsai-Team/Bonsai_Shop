package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
