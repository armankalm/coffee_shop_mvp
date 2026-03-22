ALTER TABLE payment_transactions
    ADD CONSTRAINT uq_payment_transactions_external_id UNIQUE (external_id);
