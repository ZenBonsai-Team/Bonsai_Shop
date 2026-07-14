package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    private Integer paymentId;

    @OneToOne
    @JoinColumn(name = "OrderID", nullable = false, unique = true)
    private Order order;

    @Column(name = "PaymentMethod", length = 100)
    private String paymentMethod;

    @Column(name = "PaymentStatus", length = 50)
    private String paymentStatus = "PENDING";

    @Column(name = "PaymentType", length = 100)
    private String paymentType;

    @Column(name = "Amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "PaymentDate")
    private LocalDateTime paymentDate;
}