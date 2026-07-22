package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "`ORDER`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @ManyToOne
    @JoinColumn(name = "CustomerID")
    private User customer;

    @Column(name = "OrderCode", nullable = false, unique = true, length = 100)
    private String orderCode;

    @Column(name = "CustomerName", length = 255)
    private String customerName;

    @Column(name = "CustomerPhone", length = 20)
    private String customerPhone;

    @Column(name = "CustomerEmail", length = 255)
    private String customerEmail;

    @Column(name = "ShippingAddress", length = 500)
    private String shippingAddress;

    @Column(name = "OrderDate")
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "TotalAmount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "DepositAmount", precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "OrderStatus", length = 50)
    private String orderStatus = "PENDING";

    @Column(name = "OrderType", length = 50, nullable = false)
    private String orderType = "ONLINE";

    @Column(name = "CraneFee", precision = 15, scale = 2)
    private BigDecimal craneFee = BigDecimal.ZERO;

    @Column(name = "ShippingFee", precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "Notes", length = 500)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Version
    @Column(name = "version")
    private Integer version = 0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderLog> orderLogs;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderHandling> orderHandlings;
}
