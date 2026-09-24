package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "toppings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private RefToppingType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "topping_incompatibilities",
        joinColumns = @JoinColumn(name = "topping_id"),
        inverseJoinColumns = @JoinColumn(name = "incompatible_topping_id")
    )
    @Builder.Default
    private Set<Topping> incompatibleWith = new HashSet<>();
}
