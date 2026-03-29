ALTER TABLE products
    ADD COLUMN coffee_shop_id BIGINT
        REFERENCES coffee_shops(id) ON DELETE CASCADE;

-- Assign existing products to the first available coffee shop
UPDATE products
SET coffee_shop_id = (SELECT id FROM coffee_shops ORDER BY id LIMIT 1)
WHERE coffee_shop_id IS NULL;

ALTER TABLE products
    ALTER COLUMN coffee_shop_id SET NOT NULL;

CREATE INDEX idx_products_coffee_shop_id ON products (coffee_shop_id);
