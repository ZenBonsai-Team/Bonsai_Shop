package com.example.bonsai_shop.notification.repository;


import com.example.bonsai_shop.entity.ModerationNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<ModerationNotification, Integer>{

    List<ModerationNotification> findByTargetUsernameOrderByCreatedAtDesc(String targetUsername);

    List<ModerationNotification> findByTargetUsernameAndIsReadFalseOrderByCreatedAtDesc(String targetUsername);

    long countByTargetUsernameAndIsReadFalse(String targetUsername);



}
