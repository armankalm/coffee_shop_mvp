package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.OrderDailyCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface OrderDailyCounterRepository extends JpaRepository<OrderDailyCounter, OrderDailyCounter.Key> {

    /**
     * Atomically allocates the next sequential order number for the given shop and date.
     * Uses a PostgreSQL UPSERT: the first order of the day inserts a row with value 1,
     * subsequent orders increment it. The returned value is the number to assign to the
     * order. Concurrent callers are serialized on the (shop_id, order_date) row, so no
     * two orders ever receive the same number.
     */
    @Query(value = """
            INSERT INTO order_daily_counters (shop_id, order_date, last_number)
            VALUES (:shopId, :orderDate, 1)
            ON CONFLICT (shop_id, order_date)
            DO UPDATE SET last_number = order_daily_counters.last_number + 1
            RETURNING last_number
            """, nativeQuery = true)
    int nextNumber(@Param("shopId") Long shopId, @Param("orderDate") LocalDate orderDate);
}
