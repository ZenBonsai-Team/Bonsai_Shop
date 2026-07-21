package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.ModerationNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ModerationNotificationRepository extends JpaRepository<ModerationNotification, Integer> {
    List<ModerationNotification> findByTargetUsernameOrderByCreatedAtDesc(String targetUsername);
    List<ModerationNotification> findTop10ByTargetUsernameOrderByCreatedAtDesc(String targetUsername);
    long countByTargetUsernameAndIsReadFalse(String targetUsername);
}
