CREATE TABLE otp_codes (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    code       VARCHAR(10)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_otp_codes_email ON otp_codes (email);
