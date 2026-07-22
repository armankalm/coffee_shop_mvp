-- Stripe Customer id used to store and reuse saved payment methods (cards).
-- We store only this reference; card data itself lives with Stripe (PCI).
ALTER TABLE users ADD COLUMN stripe_customer_id VARCHAR(255);
