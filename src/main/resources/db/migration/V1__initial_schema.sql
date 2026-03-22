-- Users
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    role       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Coffee Shops
CREATE TABLE coffee_shops (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    city    VARCHAR(100) NOT NULL,
    address VARCHAR(500) NOT NULL,
    status  VARCHAR(30)  NOT NULL
);

-- Products
CREATE TABLE products (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255)   NOT NULL,
    category   VARCHAR(50)    NOT NULL,
    base_price NUMERIC(10, 2) NOT NULL,
    available  BOOLEAN        NOT NULL DEFAULT TRUE
);

-- Toppings
CREATE TABLE toppings (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(255)   NOT NULL,
    type  VARCHAR(20)    NOT NULL,
    price NUMERIC(10, 2) NOT NULL
);

-- Topping incompatibilities (self-referential many-to-many)
CREATE TABLE topping_incompatibilities (
    topping_id            BIGINT NOT NULL REFERENCES toppings (id) ON DELETE CASCADE,
    incompatible_topping_id BIGINT NOT NULL REFERENCES toppings (id) ON DELETE CASCADE,
    PRIMARY KEY (topping_id, incompatible_topping_id)
);

-- Product <-> Topping availability
CREATE TABLE product_toppings (
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    topping_id BIGINT NOT NULL REFERENCES toppings (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, topping_id)
);

-- Orders
CREATE TABLE orders (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT         NOT NULL REFERENCES users (id),
    shop_id    BIGINT         NOT NULL REFERENCES coffee_shops (id),
    status     VARCHAR(20)    NOT NULL,
    total      NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Order Items
CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id BIGINT         NOT NULL REFERENCES products (id),
    quantity   INT            NOT NULL,
    price      NUMERIC(10, 2) NOT NULL
);

-- Order Item <-> Toppings
CREATE TABLE order_item_toppings (
    order_item_id BIGINT NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    topping_id    BIGINT NOT NULL REFERENCES toppings (id),
    PRIMARY KEY (order_item_id, topping_id)
);

-- Saved Combinations
CREATE TABLE saved_combinations (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT       NOT NULL REFERENCES products (id),
    name       VARCHAR(255) NOT NULL
);

-- Saved Combination <-> Toppings
CREATE TABLE saved_combination_toppings (
    saved_combination_id BIGINT NOT NULL REFERENCES saved_combinations (id) ON DELETE CASCADE,
    topping_id           BIGINT NOT NULL REFERENCES toppings (id),
    PRIMARY KEY (saved_combination_id, topping_id)
);

-- Favorite Items
CREATE TABLE favorite_items (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    saved_combination_id BIGINT NOT NULL REFERENCES saved_combinations (id) ON DELETE CASCADE,
    UNIQUE (user_id, saved_combination_id)
);

-- Indexes
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_shop_id ON orders (shop_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_saved_combinations_user_id ON saved_combinations (user_id);
CREATE INDEX idx_favorite_items_user_id ON favorite_items (user_id);
CREATE INDEX idx_coffee_shops_city ON coffee_shops (city);
CREATE INDEX idx_products_category ON products (category);
