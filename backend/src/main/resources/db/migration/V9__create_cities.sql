-- Create cities table
CREATE TABLE cities (
    id      BIGSERIAL    PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    region  VARCHAR(100),
    country VARCHAR(100),
    active  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Migrate coffee_shops.city (VARCHAR) -> city_id (FK to cities)
-- First, insert distinct city values from existing data
INSERT INTO cities (name, active)
SELECT DISTINCT city, TRUE
FROM coffee_shops
WHERE city IS NOT NULL AND city <> '';

-- Add city_id column and populate from the cities we just inserted
ALTER TABLE coffee_shops ADD COLUMN city_id BIGINT;
UPDATE coffee_shops cs
SET city_id = (SELECT id FROM cities c WHERE c.name = cs.city);

-- For any shops without a matching city (edge case), assign to a default
-- Insert a default city if needed and assign
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM coffee_shops WHERE city_id IS NULL) THEN
        INSERT INTO cities (name, active) VALUES ('Unknown', TRUE)
        ON CONFLICT DO NOTHING;
        UPDATE coffee_shops SET city_id = (SELECT id FROM cities WHERE name = 'Unknown')
        WHERE city_id IS NULL;
    END IF;
END $$;

ALTER TABLE coffee_shops ALTER COLUMN city_id SET NOT NULL;
ALTER TABLE coffee_shops ADD CONSTRAINT fk_coffee_shops_city FOREIGN KEY (city_id) REFERENCES cities(id);
ALTER TABLE coffee_shops DROP COLUMN city;
