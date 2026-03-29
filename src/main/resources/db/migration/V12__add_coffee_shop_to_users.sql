-- Add coffee_shop_id to users table
ALTER TABLE users
    ADD COLUMN coffee_shop_id BIGINT NULL
        REFERENCES coffee_shops (id) ON DELETE SET NULL;

-- Index for foreign key lookups
CREATE INDEX idx_users_coffee_shop_id ON users (coffee_shop_id);
