package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "live_session")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SessionID")
    private Integer sessionId;

    @Column(name = "Title", nullable = false)
    private String title;

    @Column(name = "StreamURL", length = 500)
    private String streamUrl;

    @Column(name = "Status", nullable = false, length = 50)
    private String status = "ONGOING";

    @Column(name = "StartTime")
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "EndTime")
    private LocalDateTime endTime;
}
