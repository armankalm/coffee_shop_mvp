-- Prevent duplicate PENDING payment transactions for the same order (TOCTOU guard)
CREATE UNIQUE INDEX uq_payment_transactions_order_pending
    ON payment_transactions (order_id)
    WHERE status = 'PENDING';
