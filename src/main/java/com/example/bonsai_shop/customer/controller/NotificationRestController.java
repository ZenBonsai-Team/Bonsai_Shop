package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.repository.ModerationNotificationRepository;
import com.example.bonsai_shop.entity.ModerationNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final ModerationNotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("unreadCount", 0, "notifications", List.of()));
        }

        String username = userDetails.getUsername();
        List<ModerationNotification> list = notificationRepository.findTop10ByTargetUsernameOrderByCreatedAtDesc(username);
        long unreadCount = notificationRepository.countByTargetUsernameAndIsReadFalse(username);

        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        response.put("notifications", list);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("success", false));
        }

        String username = userDetails.getUsername();
        List<ModerationNotification> list = notificationRepository.findByTargetUsernameOrderByCreatedAtDesc(username);
        for (ModerationNotification notification : list) {
            if (Boolean.FALSE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
            }
        }
        notificationRepository.saveAll(list);

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") Integer id,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("success", false));
        }

        ModerationNotification notification = notificationRepository.findById(id).orElse(null);
        if (notification != null) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable("id") Integer id,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("success", false));
        }

        notificationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @RequestMapping(value = "/clear-read", method = {RequestMethod.POST, RequestMethod.DELETE})
    public ResponseEntity<?> clearReadNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("success", false));
        }

        String username = userDetails.getUsername();
        List<ModerationNotification> list = notificationRepository.findByTargetUsernameOrderByCreatedAtDesc(username);
        List<ModerationNotification> readList = list.stream().filter(n -> Boolean.TRUE.equals(n.getIsRead())).toList();
        notificationRepository.deleteAll(readList);

        return ResponseEntity.ok(Map.of("success", true));
    }
}
