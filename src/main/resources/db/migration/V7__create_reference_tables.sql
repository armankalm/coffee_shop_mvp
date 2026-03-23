-- Reference table: order statuses
CREATE TABLE ref_order_statuses (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name_ru     VARCHAR(100) NOT NULL,
    name_en     VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

-- Reference table: shop statuses
CREATE TABLE ref_shop_statuses (
    id      BIGSERIAL    PRIMARY KEY,
    code    VARCHAR(30)  NOT NULL UNIQUE,
    name_ru VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL
);

-- Reference table: user roles
CREATE TABLE ref_user_roles (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name_ru     VARCHAR(100) NOT NULL,
    name_en     VARCHAR(100) NOT NULL,
    permissions VARCHAR(500)
);

-- Reference table: topping types
CREATE TABLE ref_topping_types (
    id      BIGSERIAL    PRIMARY KEY,
    code    VARCHAR(30)  NOT NULL UNIQUE,
    name_ru VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL
);

-- Reference table: product categories
CREATE TABLE ref_product_categories (
    id      BIGSERIAL    PRIMARY KEY,
    code    VARCHAR(30)  NOT NULL UNIQUE,
    name_ru VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    icon    VARCHAR(50)
);

-- Seed: order statuses
INSERT INTO ref_order_statuses (code, name_ru, name_en, description) VALUES
    ('NEW',               'Новый',            'New',               'Order has been placed and is awaiting processing'),
    ('IN_PROGRESS',       'В работе',         'In Progress',       'Order is being prepared by barista'),
    ('READY',             'Готов',            'Ready',             'Order is ready for pickup'),
    ('COMPLETED',         'Завершён',         'Completed',         'Order has been picked up by customer'),
    ('CANCELLED',         'Отменён',          'Cancelled',         'Order was cancelled');

-- Seed: shop statuses
INSERT INTO ref_shop_statuses (code, name_ru, name_en) VALUES
    ('OPEN',               'Открыто',          'Open'),
    ('CLOSED',             'Закрыто',          'Closed'),
    ('TEMPORARILY_CLOSED', 'Временно закрыто', 'Temporarily Closed');

-- Seed: user roles
INSERT INTO ref_user_roles (code, name_ru, name_en, permissions) VALUES
    ('USER',    'Пользователь', 'User',    'orders:read,orders:create,orders:cancel,combinations:manage,favorites:manage'),
    ('BARISTA', 'Бариста',      'Barista', 'orders:read,orders:update-status,orders:print'),
    ('MANAGER', 'Менеджер',     'Manager', 'orders:*,products:*,shops:*,toppings:*,users:read'),
    ('ADMIN',   'Администратор','Admin',   'orders:*,products:*,shops:*,toppings:*,users:*,reference:*');

-- Seed: topping types
INSERT INTO ref_topping_types (code, name_ru, name_en) VALUES
    ('MILK',   'Молоко',  'Milk'),
    ('SYRUP',  'Сиропы',  'Syrup'),
    ('TOPPING','Топпинги','Topping'),
    ('EXTRAS', 'Добавки', 'Extras');

-- Seed: product categories
INSERT INTO ref_product_categories (code, name_ru, name_en, icon) VALUES
    ('COFFEE',      'Кофе',         'Coffee',      'coffee'),
    ('TEA',         'Чай',          'Tea',         'tea'),
    ('COLD_DRINKS', 'Холодные напитки', 'Cold Drinks', 'cold-drink'),
    ('FOOD',        'Еда',          'Food',        'food'),
    ('DESSERTS',    'Десерты',      'Desserts',    'dessert'),
    ('OTHER',       'Прочее',       'Other',       'other');
