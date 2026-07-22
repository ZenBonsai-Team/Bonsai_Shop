package com.example.bonsai_shop.product.dto;

import java.time.LocalDateTime;

public class OrderHandlingDTO {
    private String assignedUsername;
    private String assignedFullname;
    private LocalDateTime assignedAt;
    private LocalDateTime releasedAt;
    private boolean isActive;
}
