-- Seed test data (idempotent via ON CONFLICT DO NOTHING)

-- Cities
INSERT INTO cities (name, region, country, active) VALUES
    ('Алматы', 'Алматинская область', 'Казахстан', TRUE),
    ('Астана', 'Акмолинская область', 'Казахстан', TRUE)
ON CONFLICT (name) DO NOTHING;

-- Coffee Shops
INSERT INTO coffee_shops (name, city_id, address, status_id)
SELECT 'CoffeeShop Алматы Центр',
       (SELECT id FROM cities WHERE name = 'Алматы'),
       'ул. Абая 12, Алматы',
       (SELECT id FROM ref_shop_statuses WHERE code = 'OPEN')
WHERE NOT EXISTS (SELECT 1 FROM coffee_shops WHERE name = 'CoffeeShop Алматы Центр');

INSERT INTO coffee_shops (name, city_id, address, status_id)
SELECT 'CoffeeShop Алматы Медеу',
       (SELECT id FROM cities WHERE name = 'Алматы'),
       'пр. Достык 88, Алматы',
       (SELECT id FROM ref_shop_statuses WHERE code = 'OPEN')
WHERE NOT EXISTS (SELECT 1 FROM coffee_shops WHERE name = 'CoffeeShop Алматы Медеу');

INSERT INTO coffee_shops (name, city_id, address, status_id)
SELECT 'CoffeeShop Астана',
       (SELECT id FROM cities WHERE name = 'Астана'),
       'пр. Республики 5, Астана',
       (SELECT id FROM ref_shop_statuses WHERE code = 'OPEN')
WHERE NOT EXISTS (SELECT 1 FROM coffee_shops WHERE name = 'CoffeeShop Астана');

-- Products
INSERT INTO products (name, category_id, base_price, available)
SELECT 'Эспрессо', (SELECT id FROM ref_product_categories WHERE code = 'COFFEE'), 800.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Эспрессо');

INSERT INTO products (name, category_id, base_price, available)
SELECT 'Капучино', (SELECT id FROM ref_product_categories WHERE code = 'COFFEE'), 1200.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Капучино');

INSERT INTO products (name, category_id, base_price, available)
SELECT 'Зелёный чай', (SELECT id FROM ref_product_categories WHERE code = 'TEA'), 700.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Зелёный чай');

INSERT INTO products (name, category_id, base_price, available)
SELECT 'Лимонад', (SELECT id FROM ref_product_categories WHERE code = 'COLD_DRINKS'), 900.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Лимонад');

INSERT INTO products (name, category_id, base_price, available)
SELECT 'Круассан', (SELECT id FROM ref_product_categories WHERE code = 'FOOD'), 650.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Круассан');

-- Toppings
INSERT INTO toppings (name, type_id, price)
SELECT 'Миндальное молоко', (SELECT id FROM ref_topping_types WHERE code = 'MILK'), 200.00
WHERE NOT EXISTS (SELECT 1 FROM toppings WHERE name = 'Миндальное молоко');

INSERT INTO toppings (name, type_id, price)
SELECT 'Ванильный сироп', (SELECT id FROM ref_topping_types WHERE code = 'SYRUP'), 150.00
WHERE NOT EXISTS (SELECT 1 FROM toppings WHERE name = 'Ванильный сироп');

INSERT INTO toppings (name, type_id, price)
SELECT 'Корица', (SELECT id FROM ref_topping_types WHERE code = 'TOPPING'), 50.00
WHERE NOT EXISTS (SELECT 1 FROM toppings WHERE name = 'Корица');

INSERT INTO toppings (name, type_id, price)
SELECT 'Взбитые сливки', (SELECT id FROM ref_topping_types WHERE code = 'EXTRAS'), 180.00
WHERE NOT EXISTS (SELECT 1 FROM toppings WHERE name = 'Взбитые сливки');

-- Users (seed only if email not already taken)
INSERT INTO users (email, role_id, coffee_shop_id)
SELECT
    'user@example.com',
    (SELECT id FROM ref_user_roles WHERE code = 'USER'),
    (SELECT id FROM coffee_shops WHERE name = 'CoffeeShop Алматы Центр')
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'user@example.com');

INSERT INTO users (email, role_id, coffee_shop_id)
SELECT
    'barista@example.com',
    (SELECT id FROM ref_user_roles WHERE code = 'BARISTA'),
    (SELECT id FROM coffee_shops WHERE name = 'CoffeeShop Алматы Центр')
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'barista@example.com');

INSERT INTO users (email, role_id, coffee_shop_id)
SELECT
    'manager@example.com',
    (SELECT id FROM ref_user_roles WHERE code = 'MANAGER'),
    (SELECT id FROM coffee_shops WHERE name = 'CoffeeShop Астана')
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'manager@example.com');

-- Orders
INSERT INTO orders (user_id, shop_id, status_id, total, created_at)
SELECT
    (SELECT id FROM users WHERE email = 'user@example.com'),
    (SELECT id FROM coffee_shops WHERE name = 'CoffeeShop Алматы Центр'),
    (SELECT id FROM ref_order_statuses WHERE code = 'NEW'),
    1850.00,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM orders o
    WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
      AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'NEW')
);

INSERT INTO orders (user_id, shop_id, status_id, total, created_at)
SELECT
    (SELECT id FROM users WHERE email = 'user@example.com'),
    (SELECT id FROM coffee_shops WHERE name = 'CoffeeShop Алматы Медеу'),
    (SELECT id FROM ref_order_statuses WHERE code = 'COMPLETED'),
    1600.00,
    NOW() - INTERVAL '1 day'
WHERE NOT EXISTS (
    SELECT 1 FROM orders o
    WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
      AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'COMPLETED')
);

-- Order Items for NEW order
INSERT INTO order_items (order_id, product_id, quantity, price)
SELECT
    (SELECT id FROM orders o
     WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
       AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'NEW')
     ORDER BY id ASC LIMIT 1),
    (SELECT id FROM products WHERE name = 'Капучино'),
    1,
    1200.00
WHERE NOT EXISTS (
    SELECT 1 FROM order_items oi
    WHERE oi.order_id = (
        SELECT id FROM orders o
        WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
          AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'NEW')
        ORDER BY id ASC LIMIT 1
    )
    AND oi.product_id = (SELECT id FROM products WHERE name = 'Капучино')
);

INSERT INTO order_items (order_id, product_id, quantity, price)
SELECT
    (SELECT id FROM orders o
     WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
       AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'NEW')
     ORDER BY id ASC LIMIT 1),
    (SELECT id FROM products WHERE name = 'Круассан'),
    1,
    650.00
WHERE NOT EXISTS (
    SELECT 1 FROM order_items oi
    WHERE oi.order_id = (
        SELECT id FROM orders o
        WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
          AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'NEW')
        ORDER BY id ASC LIMIT 1
    )
    AND oi.product_id = (SELECT id FROM products WHERE name = 'Круассан')
);

-- Order Items for COMPLETED order
INSERT INTO order_items (order_id, product_id, quantity, price)
SELECT
    (SELECT id FROM orders o
     WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
       AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'COMPLETED')
     ORDER BY id ASC LIMIT 1),
    (SELECT id FROM products WHERE name = 'Эспрессо'),
    2,
    1600.00
WHERE NOT EXISTS (
    SELECT 1 FROM order_items oi
    WHERE oi.order_id = (
        SELECT id FROM orders o
        WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
          AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'COMPLETED')
        ORDER BY id ASC LIMIT 1
    )
    AND oi.product_id = (SELECT id FROM products WHERE name = 'Эспрессо')
);

-- Payment Transaction for COMPLETED order
INSERT INTO payment_transactions (order_id, provider, status, amount, external_id, created_at, updated_at)
SELECT
    (SELECT id FROM orders o
     WHERE o.user_id = (SELECT id FROM users WHERE email = 'user@example.com')
       AND o.status_id = (SELECT id FROM ref_order_statuses WHERE code = 'COMPLETED')
     ORDER BY id ASC LIMIT 1),
    'KASPI',
    'SUCCESS',
    1600.00,
    'KASPI-SEED-001',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
WHERE NOT EXISTS (
    SELECT 1 FROM payment_transactions WHERE external_id = 'KASPI-SEED-001'
);
