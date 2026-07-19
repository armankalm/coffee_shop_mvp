-- Kitchen board: track preparation status per order item (not just per order).
-- Each item advances independently NEW -> IN_PROGRESS -> READY -> COMPLETED.
-- The parent order's status is only advanced once all of its items reach the target status.

-- Add status_id FK to order_items (mirrors orders.status_id from V8).
ALTER TABLE order_items ADD COLUMN status_id BIGINT;

-- Backfill: existing items inherit the current status of their parent order.
UPDATE order_items oi
SET status_id = (SELECT o.status_id FROM orders o WHERE o.id = oi.order_id);

-- Any item whose parent order is CANCELLED (a terminal status not used on the
-- board's forward chain) is normalised to NEW so the board has a valid starting point.
UPDATE order_items
SET status_id = (SELECT id FROM ref_order_statuses WHERE code = 'NEW')
WHERE status_id IS NULL
   OR status_id = (SELECT id FROM ref_order_statuses WHERE code = 'CANCELLED');

ALTER TABLE order_items ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_status FOREIGN KEY (status_id) REFERENCES ref_order_statuses (id);

CREATE INDEX idx_order_items_status_id ON order_items (status_id);
