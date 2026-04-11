-- Distribute seed products across all shops.
-- V14 assigned all products to the first shop; this creates copies for other shops
-- so the demo/dev experience shows products in every shop's menu.

-- For each shop that has zero products, clone the product catalog from the first shop.
INSERT INTO products (name, category_id, base_price, available, coffee_shop_id, image_path)
SELECT p.name, p.category_id, p.base_price, p.available, cs.id, p.image_path
FROM coffee_shops cs
CROSS JOIN products p
WHERE p.coffee_shop_id = (SELECT id FROM coffee_shops ORDER BY id LIMIT 1)
  AND cs.id <> (SELECT id FROM coffee_shops ORDER BY id LIMIT 1)
  AND NOT EXISTS (
      SELECT 1 FROM products p2
      WHERE p2.coffee_shop_id = cs.id AND p2.name = p.name
  );

-- Copy topping associations for the newly created products
INSERT INTO product_toppings (product_id, topping_id)
SELECT new_p.id, pt.topping_id
FROM products new_p
JOIN products orig_p ON orig_p.name = new_p.name
    AND orig_p.coffee_shop_id = (SELECT id FROM coffee_shops ORDER BY id LIMIT 1)
JOIN product_toppings pt ON pt.product_id = orig_p.id
WHERE new_p.coffee_shop_id <> (SELECT id FROM coffee_shops ORDER BY id LIMIT 1)
  AND NOT EXISTS (
      SELECT 1 FROM product_toppings pt2
      WHERE pt2.product_id = new_p.id AND pt2.topping_id = pt.topping_id
  );
