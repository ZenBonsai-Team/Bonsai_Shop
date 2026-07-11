package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ORDER_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderLogID")
    private Integer orderLogId;

    @ManyToOne
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "ActionByID", nullable = false)
    private User actionBy;

    @Column(name = "ActionType", length = 100)
    private String actionType;

    @Column(name = "FromStatus", length = 50)
    private String fromStatus;

    @Column(name = "ToStatus", length = 50)
    private String toStatus;

    @Column(name = "ActionAt")
    private LocalDateTime actionAt = LocalDateTime.now();
}