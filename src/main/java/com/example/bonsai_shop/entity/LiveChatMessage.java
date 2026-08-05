package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Persisted live chat messages so they survive page refresh.
 * Linked to a LiveSession; loaded on page open and appended via WebSocket.
 */
@Entity
@Table(name = "live_chat_message")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LiveChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MessageID")
    private Integer messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SessionID", nullable = false)
    private LiveSession liveSession;

    @Column(name = "Author", nullable = false, length = 100)
    private String author;

    @Column(name = "Message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Source: WEB (user typed), YOUTUBE (synced from YT), MODERATOR (moderator typed) */
    @Column(name = "Source", length = 20)
    private String source = "WEB";

    @Column(name = "SentAt")
    private LocalDateTime sentAt = LocalDateTime.now();
}
