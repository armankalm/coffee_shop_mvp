package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coffee_shops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoffeeShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private RefShopStatus status;
}
