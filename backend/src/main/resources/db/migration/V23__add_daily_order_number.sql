-- Per-shop, per-day sequential order number that resets at local midnight.
-- The "day" is computed in the business timezone (Asia/Almaty) so numbering
-- aligns with the working day rather than UTC.

ALTER TABLE orders ADD COLUMN daily_number INTEGER;
ALTER TABLE orders ADD COLUMN order_date   DATE;

-- Counter table: one row per (shop, day). The application atomically upserts this
-- row to allocate the next number, guaranteeing no duplicates under concurrency.
CREATE TABLE order_daily_counters (
    shop_id     BIGINT  NOT NULL REFERENCES coffee_shops (id) ON DELETE CASCADE,
    order_date  DATE    NOT NULL,
    last_number INTEGER NOT NULL,
    PRIMARY KEY (shop_id, order_date)
);

-- Backfill existing orders: assign order_date from created_at (in Asia/Almaty) and
-- number them 1..N within each (shop, day), ordered by creation time (id as tie-break).
WITH numbered AS (
    SELECT
        o.id,
        (o.created_at AT TIME ZONE 'Asia/Almaty')::date AS local_date,
        ROW_NUMBER() OVER (
            PARTITION BY o.shop_id, (o.created_at AT TIME ZONE 'Asia/Almaty')::date
            ORDER BY o.created_at, o.id
        ) AS seq
    FROM orders o
)
UPDATE orders o
SET order_date   = n.local_date,
    daily_number = n.seq
FROM numbered n
WHERE o.id = n.id;

-- Seed the counters so newly created orders continue after the backfilled maximum.
INSERT INTO order_daily_counters (shop_id, order_date, last_number)
SELECT o.shop_id, o.order_date, MAX(o.daily_number)
FROM orders o
WHERE o.order_date IS NOT NULL
GROUP BY o.shop_id, o.order_date;

-- Enforce uniqueness of the daily number within a shop and day.
ALTER TABLE orders
    ADD CONSTRAINT uq_orders_shop_date_number UNIQUE (shop_id, order_date, daily_number);

-- Index to look up a shop's orders for a given day efficiently.
CREATE INDEX idx_orders_shop_id_order_date ON orders (shop_id, order_date);
