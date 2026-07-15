package com.example.bonsai_shop.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerAppointmentDTO {

    private Integer appointmentId;
    private LocalDateTime appointmentDate;
    private String status;
    private String note;

    private String productCode;
    private String productName;

    private String customerName;
    private String customerPhone;
    private String customerEmail;
}