package com.example.bonsai_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "BUSSINESS_ACTION")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ActionID")
    private Integer actionId;

    @Column(name = "ActionCode", nullable = false, unique = true, length = 100)
    private String actionCode;

    @Column(name = "ActionName", nullable = false, length = 255)
    private String actionName;

    @Column(name = "Description", length = 500)
    private String description;

    @OneToMany(mappedBy = "action", cascade = CascadeType.ALL)
    private List<RoleAction> roleActions;
}
