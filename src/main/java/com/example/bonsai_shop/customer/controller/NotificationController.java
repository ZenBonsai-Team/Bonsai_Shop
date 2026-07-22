package com.example.bonsai_shop.customer.controller;

import com.example.bonsai_shop.customer.service.UserService;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.notification.service.NotificationService;
import com.example.bonsai_shop.entity.ModerationNotification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public List<ModerationNotification> getAllNotifications(Principal principal) {
        User user = userService.findByEmail(principal.getName());

        return notificationService.getAllNotifications(user.getUsername());
    }

    @GetMapping("/count")
    public long countUnreadNotifications(Principal principal) {
        User user = userService.findByEmail(principal.getName());

        return notificationService.countUnreadNotification(user.getUsername());

    }

    @PostMapping("/read/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable Integer id
                                     ,Principal principal) {
        User user = userService.findByEmail(principal.getName());
        try {
            notificationService.markAsRead(id, user.getUsername());
            return ResponseEntity.ok("Đánh dấu đã đọc thành công.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<String> readAllNotifications(Principal principal) {
         User user = userService.findByEmail(principal.getName());
         try{
             notificationService.markAllAsRead(user.getUsername());
             return ResponseEntity.ok("Đánh dấu đã đọc tất cả thông báo.");
         }catch (Exception e){
           return ResponseEntity.badRequest().body(e.getMessage());
         }
    }

}
