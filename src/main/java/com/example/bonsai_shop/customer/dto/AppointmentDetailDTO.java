package com.example.bonsai_shop.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentDetailDTO {

    private Integer appointmentId;

    private LocalDateTime appointmentDate;

    private String status;

    private String note;
}
