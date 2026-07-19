-- Many-to-many assignment of staff (BARISTA/MANAGER/ADMIN) to the coffee shops
-- they are allowed to view and manage orders for.
--
-- The existing users.coffee_shop_id column is KEPT, but its meaning is narrowed to
-- "the regular USER's currently selected shop for placing orders" and is no longer
-- used for staff authorization. This migration moves any existing staff bindings from
-- that column into the new join table so staff do not lose access.

CREATE TABLE user_assigned_shops (
    user_id BIGINT NOT NULL REFERENCES users (id)        ON DELETE CASCADE,
    shop_id BIGINT NOT NULL REFERENCES coffee_shops (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, shop_id)
);

-- Index for the reverse lookup (find staff by shop); the PK already covers user_id lookups.
CREATE INDEX idx_user_assigned_shops_shop_id ON user_assigned_shops (shop_id);

-- Backfill: carry over the current coffee_shop_id of every STAFF user (role != USER)
-- into the new assignment table. Regular USERs keep coffee_shop_id only as their
-- selected shop and are intentionally excluded.
INSERT INTO user_assigned_shops (user_id, shop_id)
SELECT u.id, u.coffee_shop_id
FROM users u
JOIN ref_user_roles r ON r.id = u.role_id
WHERE u.coffee_shop_id IS NOT NULL
  AND r.code <> 'USER';

-- Clear the now-misused coffee_shop_id for staff so the column only ever holds a
-- regular USER's selected shop going forward. (Staff bindings now live in the join table.)
UPDATE users u
SET coffee_shop_id = NULL
FROM ref_user_roles r
WHERE r.id = u.role_id
  AND r.code <> 'USER'
  AND u.coffee_shop_id IS NOT NULL;
