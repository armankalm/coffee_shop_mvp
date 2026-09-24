-- POS orders: a barista can place an order for a walk-in guest without
-- registering a user. The guest's display name is stored on the order itself;
-- the order is still owned by the barista's account (user_id).
ALTER TABLE orders ADD COLUMN customer_name VARCHAR(255);
