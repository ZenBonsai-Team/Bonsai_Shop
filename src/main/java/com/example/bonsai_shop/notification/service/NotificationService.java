package com.example.bonsai_shop.notification.service;

import com.example.bonsai_shop.entity.ModerationNotification;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final ModerationNotificationRepository notificationRepository;

    public void createNotification(User user, String message) {
        ModerationNotification moderationNotification = ModerationNotification.builder()
                .targetUsername(user.getEmail())
                .message(message)
                .build();
        notificationRepository.save(moderationNotification);
    }


}
