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
public class HandlingHistoryDTO {
    private Integer handlingId;
    private String moderatorName;
    private String action;
    private LocalDateTime handledAt;
    private String reason;
    private String durationFormatted;
}
