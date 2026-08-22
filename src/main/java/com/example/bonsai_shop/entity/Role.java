package com.example.bonsai_shop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "ROLE")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    private Integer roleId;

    @Column(name = "RoleName", nullable = false, unique = true, length = 100)
    private String roleName;

    @Column(name = "Description", length = 500)
    private String description;


    @OneToMany(mappedBy = "role")
    @JsonIgnore
    private List<User> users;


    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
    private List<RoleAction> roleActions;
}
