package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Integer notificationId;

    @Column(name = "TargetUsername", nullable = false, length = 255)
    private String targetUsername;

    @Column(name = "Message", nullable = false, length = 1000)
    private String message;

    @Builder.Default
    @Column(name = "IsRead")
    private Boolean isRead = false;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
