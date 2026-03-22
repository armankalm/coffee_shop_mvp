-- Payment Transactions
CREATE TABLE payment_transactions (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT         NOT NULL REFERENCES orders (id),
    provider       VARCHAR(20)    NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    amount         NUMERIC(10, 2) NOT NULL,
    external_id    VARCHAR(255),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_transactions_order_id ON payment_transactions (order_id);
CREATE INDEX idx_payment_transactions_external_id ON payment_transactions (external_id);
