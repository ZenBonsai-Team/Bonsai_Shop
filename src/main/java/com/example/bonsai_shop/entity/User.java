package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "USER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "FullName", nullable = false, length = 255)
    private String fullName;

    @Column(name = "Username", unique = true, length = 255)
    private String username;

    @Column(name = "Email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "Avatar", length = 500)
    private String avatar;

    @Column(name = "Address", length = 500)
    private String address;

    @Column(name = "Status", length = 50)
    private String status = "PENDING";

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Order> orders;

    @Column(name = "avatar_public_id")
    private String avatarPublicId;

    @ManyToOne
    @JoinColumn(name = "RoleID", nullable = false)
    private Role role;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Wishlist> wishlists;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Cart cart;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    private List<Product> productsSold;
}
