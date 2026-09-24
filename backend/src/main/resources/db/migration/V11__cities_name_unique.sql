-- Add unique constraint on cities.name to prevent duplicate city entries
ALTER TABLE cities ADD CONSTRAINT uq_cities_name UNIQUE (name);
