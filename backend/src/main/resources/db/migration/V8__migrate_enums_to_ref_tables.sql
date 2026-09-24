-- Migrate orders.status (VARCHAR) -> status_id (FK to ref_order_statuses)
ALTER TABLE orders ADD COLUMN status_id BIGINT;
UPDATE orders o SET status_id = (SELECT id FROM ref_order_statuses r WHERE r.code = o.status);
ALTER TABLE orders ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT fk_orders_status FOREIGN KEY (status_id) REFERENCES ref_order_statuses(id);
ALTER TABLE orders DROP COLUMN status;

-- Migrate coffee_shops.status (VARCHAR) -> status_id (FK to ref_shop_statuses)
ALTER TABLE coffee_shops ADD COLUMN status_id BIGINT;
UPDATE coffee_shops cs SET status_id = (SELECT id FROM ref_shop_statuses r WHERE r.code = cs.status);
ALTER TABLE coffee_shops ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE coffee_shops ADD CONSTRAINT fk_coffee_shops_status FOREIGN KEY (status_id) REFERENCES ref_shop_statuses(id);
ALTER TABLE coffee_shops DROP COLUMN status;

-- Migrate products.category (VARCHAR) -> category_id (FK to ref_product_categories)
ALTER TABLE products ADD COLUMN category_id BIGINT;
UPDATE products p SET category_id = (SELECT id FROM ref_product_categories r WHERE r.code = p.category);
ALTER TABLE products ALTER COLUMN category_id SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES ref_product_categories(id);
ALTER TABLE products DROP COLUMN category;

-- Migrate toppings.type (VARCHAR) -> type_id (FK to ref_topping_types)
ALTER TABLE toppings ADD COLUMN type_id BIGINT;
UPDATE toppings t SET type_id = (SELECT id FROM ref_topping_types r WHERE r.code = t.type);
ALTER TABLE toppings ALTER COLUMN type_id SET NOT NULL;
ALTER TABLE toppings ADD CONSTRAINT fk_toppings_type FOREIGN KEY (type_id) REFERENCES ref_topping_types(id);
ALTER TABLE toppings DROP COLUMN type;

-- Migrate users.role (VARCHAR) -> role_id (FK to ref_user_roles)
ALTER TABLE users ADD COLUMN role_id BIGINT;
UPDATE users u SET role_id = (SELECT id FROM ref_user_roles r WHERE r.code = u.role);
ALTER TABLE users ALTER COLUMN role_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES ref_user_roles(id);
ALTER TABLE users DROP COLUMN role;
