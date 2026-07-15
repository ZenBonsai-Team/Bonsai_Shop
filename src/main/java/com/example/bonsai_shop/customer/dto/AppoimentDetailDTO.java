package com.example.bonsai_shop.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppoimentDetailDTO {

    private Integer appointmentId;

    private String productName;

    private String productCode;

    private LocalDateTime appointmentDate;

    private String status;

    private String note;
}