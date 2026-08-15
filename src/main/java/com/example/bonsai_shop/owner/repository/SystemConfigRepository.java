package com.example.bonsai_shop.owner.repository;

import com.example.bonsai_shop.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// Repository thao tac bang cau hinh he thong, khoa chinh la configKey dang String.
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}
