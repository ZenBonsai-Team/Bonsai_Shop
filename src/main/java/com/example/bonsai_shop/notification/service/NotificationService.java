package com.example.bonsai_shop.notification.service;

import com.example.bonsai_shop.entity.ModerationNotification;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;


    public void createNotification(User user, String message) {
        ModerationNotification moderationNotification = ModerationNotification.builder()
                .targetUsername(user.getUsername())
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

    public void markAsRead(Integer notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(String targetUsername) {
        List<ModerationNotification> unread = notificationRepository.findByTargetUsernameAndIsReadFalseOrderByCreatedAtDesc(targetUsername);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
