package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "saved_combinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedCombination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "saved_combination_toppings",
        joinColumns = @JoinColumn(name = "saved_combination_id"),
        inverseJoinColumns = @JoinColumn(name = "topping_id")
    )
    @Builder.Default
    private Set<Topping> toppings = new HashSet<>();

    @Column(nullable = false)
    private String name;
}
