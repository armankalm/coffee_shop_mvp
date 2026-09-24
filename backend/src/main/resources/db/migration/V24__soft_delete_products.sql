-- Soft delete for products: past orders, favorites and saved combinations keep
-- pointing at the row, so it is hidden instead of removed.
ALTER TABLE products ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
