package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RefUserRole role;

    /**
     * The regular USER's currently selected coffee shop for placing orders.
     * Not used for staff authorization — see {@link #assignedShops}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coffee_shop_id")
    private CoffeeShop coffeeShop;

    /**
     * Coffee shops a staff member (BARISTA/MANAGER/ADMIN) is assigned to.
     * Staff may view and update orders only for shops in this set. Empty for regular users.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_assigned_shops",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "shop_id"))
    @Builder.Default
    private Set<CoffeeShop> assignedShops = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
