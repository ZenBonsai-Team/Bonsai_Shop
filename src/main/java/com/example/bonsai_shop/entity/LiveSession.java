package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ thông tin về một phiên Live Stream của nhà vườn.
 * Được điều phối bởi Nghệ nhân hoặc Điều phối viên thông qua Panel quản trị.
 */
@Entity
@Table(name = "live_session")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SessionID")
    private Integer sessionId;

    // Tiêu đề của phiên phát Live Stream (Ví dụ: "Giao lưu Bonsai VIP cuối tuần")
    @Column(name = "Title", nullable = false)
    private String title;

    // Đường dẫn luồng phát (thường là URL video của YouTube Live Stream)
    @Column(name = "StreamURL", length = 500)
    private String streamUrl;

    // Trạng thái phiên Live: "ONGOING" (Đang phát) hoặc "ENDED" (Đã kết thúc)
    @Column(name = "Status", nullable = false, length = 50)
    private String status = "ONGOING";

    // Thời gian bắt đầu phát Live
    @Column(name = "StartTime")
    private LocalDateTime startTime = LocalDateTime.now();

    // Thời gian đóng phiên Live
    @Column(name = "EndTime")
    private LocalDateTime endTime;
}
