-- Add indexes on FK columns introduced in V8 and V9 to replace the dropped column indexes
CREATE INDEX idx_orders_status_id ON orders (status_id);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_coffee_shops_status_id ON coffee_shops (status_id);
CREATE INDEX idx_coffee_shops_city_id ON coffee_shops (city_id);
CREATE INDEX idx_toppings_type_id ON toppings (type_id);
CREATE INDEX idx_users_role_id ON users (role_id);
