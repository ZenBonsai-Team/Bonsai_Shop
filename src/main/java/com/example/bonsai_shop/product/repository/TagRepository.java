package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Integer> {
}
