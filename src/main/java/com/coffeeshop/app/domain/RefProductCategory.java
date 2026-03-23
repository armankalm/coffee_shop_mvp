package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ref_product_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(length = 50)
    private String icon;
}
