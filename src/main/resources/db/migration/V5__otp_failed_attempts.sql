ALTER TABLE otp_codes
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0;
