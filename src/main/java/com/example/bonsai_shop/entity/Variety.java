package com.example.bonsai_shop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "VARIETY")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Variety {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VarietyID")
    private Integer varietyId;

    @ManyToOne
    @JoinColumn(name = "CategoryID", nullable = false)
    private Category category;

    @Column(name = "VarietyName", nullable = false, length = 255)
    private String varietyName;

    @Column(name = "Description", length = 500)
    private String description;

    @OneToMany(mappedBy = "variety", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Product> products;
}

