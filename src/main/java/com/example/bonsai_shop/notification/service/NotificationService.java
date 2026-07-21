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

    public long countUnreadNotification(String targetUsername) {
        return notificationRepository.countByTargetUsernameAndIsReadFalse(targetUsername);
    }

    public void markAsRead(Integer id, String username) {

        ModerationNotification notification =
                notificationRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        if (!notification.getTargetUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền.");
        }

        notification.setIsRead(true);

        notificationRepository.save(notification);
    }

    public void markAllAsRead(String targetUsername) {

        List<ModerationNotification> unread =
                notificationRepository
                        .findByTargetUsernameAndIsReadFalseOrderByCreatedAtDesc(targetUsername);

        if (unread.isEmpty()) {
            return;
        }
        unread.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
