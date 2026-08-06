package com.example.bonsai_shop.owner.repository;

import com.example.bonsai_shop.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}
