package com.example.bonsai_shop.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineDTO {
    private String stage;
    private String label;
    private LocalDateTime timestamp;
    private boolean completed;
    private boolean current;
}
