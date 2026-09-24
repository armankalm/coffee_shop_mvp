-- Add optional profile fields to users table
ALTER TABLE users
    ADD COLUMN name VARCHAR(255) NULL,
    ADD COLUMN phone VARCHAR(32) NULL;
