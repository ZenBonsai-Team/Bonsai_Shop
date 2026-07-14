package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ORDER_HANDLING")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHandling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderHandlingID")
    private Integer orderHandlingId;

    @ManyToOne
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "ModeratorOrderID")
    private User moderator;

    @Column(name = "HandledAt")
    private LocalDateTime handledAt = LocalDateTime.now();

    @Column(name = "ReleasedAt")
    private LocalDateTime releasedAt;

    @Column(name = "IsActive")
    private Boolean isActive = true;
}