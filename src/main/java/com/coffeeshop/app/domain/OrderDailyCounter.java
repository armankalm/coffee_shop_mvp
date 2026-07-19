package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Per-shop, per-day counter used to hand out sequential order numbers that reset
 * daily (see {@link Order#getDailyNumber()}). A row is created lazily on the first
 * order of the day for a shop and atomically incremented via an upsert, so concurrent
 * order creation never yields duplicate numbers.
 */
@Entity
@Table(name = "order_daily_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(OrderDailyCounter.Key.class)
public class OrderDailyCounter {

    @Id
    @Column(name = "shop_id")
    private Long shopId;

    @Id
    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long shopId;
        private LocalDate orderDate;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(shopId, key.shopId) && Objects.equals(orderDate, key.orderDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(shopId, orderDate);
        }
    }
}
