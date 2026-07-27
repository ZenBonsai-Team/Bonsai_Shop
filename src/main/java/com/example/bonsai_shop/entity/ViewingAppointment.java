package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "VIEWING_APPOINTMENT")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ViewingAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AppointmentID")
    private Integer appointmentId;

    @ManyToOne
    @JoinColumn(name = "CustomerID", nullable = false)
    private User customer;

    @Column(name = "AppointmentDate", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "Note", length = 500)
    private String note;

    @Column(name = "Status", length = 50)
    private String status = "PENDING";
}
