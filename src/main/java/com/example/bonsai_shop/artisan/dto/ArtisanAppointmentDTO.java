package com.example.bonsai_shop.artisan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtisanAppointmentDTO {

    private Integer appointmentId;
    private LocalDateTime appointmentDate;
    private LocalDateTime createdAt;
    private String status;
    private String note;

    private String customerName;
    private String customerPhone;
    private String customerEmail;
}
