package com.example.bonsai_shop.notification.service;

import com.example.bonsai_shop.entity.ModerationNotification;
import com.example.bonsai_shop.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;


    public void createNotification(String targetUsername, String message) {
        ModerationNotification moderationNotification = ModerationNotification.builder()
                .targetUsername(targetUsername)
                .message(message)
                .build();
        notificationRepository.save(moderationNotification);
    }

    public List<ModerationNotification> getAllNotifications(String targetUsername) {
        return notificationRepository.findByTargetUsernameOrderByCreatedAtDesc(targetUsername);
    }

    public List<ModerationNotification> getAllNotificationsUnread(String targetUsername) {
        return notificationRepository.findByTargetUsernameAndIsReadFalseOrderByCreatedAtDesc(targetUsername);
    }

    public long countUnreadNotification(String targetUsername) {
        return notificationRepository.countByTargetUsernameAndIsReadFalse(targetUsername);
    }
}
