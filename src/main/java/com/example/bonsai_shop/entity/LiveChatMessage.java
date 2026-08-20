package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ các tin nhắn trong phòng chat của phiên Live Stream.
 * Giúp tin nhắn không bị mất khi tải lại trang (reload).
 * Được liên kết với một phiên LiveSession; tải lại lịch sử khi mở trang và nhận tin nhắn mới qua WebSocket.
 */
@Entity
@Table(name = "live_chat_message")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LiveChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MessageID")
    private Integer messageId;

    // Liên kết với phiên LiveSession (SessionID) chứa tin nhắn này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SessionID", nullable = false)
    private LiveSession liveSession;

    // Tên người gửi tin nhắn (Tên hiển thị trên web hoặc trên YouTube)
    @Column(name = "Author", nullable = false, length = 100)
    private String author;

    // Nội dung tin nhắn chat
    @Column(name = "Message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** 
     * Nguồn gửi tin nhắn: 
     * - WEB: Khách hàng nhắn từ khung chat trên web
     * - YOUTUBE: Tin nhắn đồng bộ từ buổi stream trên YouTube Live về
     * - MODERATOR: Tin nhắn từ bảng điều khiển của kiểm duyệt viên
     */
    @Column(name = "Source", length = 20)
    private String source = "WEB";

    // Thời điểm gửi tin nhắn
    @Column(name = "SentAt")
    private LocalDateTime sentAt = LocalDateTime.now();
}
