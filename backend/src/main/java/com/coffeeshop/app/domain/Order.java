package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private CoffeeShop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private RefOrderStatus status;

    @Column(name = "customer_name")
    private String customerName;

    /**
     * Per-shop, per-day sequential order number shown to staff/customers (e.g. "№7").
     * Resets to 1 at local midnight (see {@link #orderDate}) for each coffee shop.
     * Distinct from {@link #id}, which is the global primary key.
     */
    @Column(name = "daily_number")
    private Integer dailyNumber;

    /**
     * The local business date (see app.order.timezone) this order's {@link #dailyNumber}
     * belongs to. Together with shop, uniquely identifies the daily sequence.
     */
    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OrderItem> items = new LinkedHashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
