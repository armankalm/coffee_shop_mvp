package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String country;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
