-- Draft status for orders awaiting payment confirmation.
-- Orders in this status are hidden from the kitchen board and from customer/admin
-- history until a successful payment webhook promotes them to NEW.
INSERT INTO ref_order_statuses (code, name_ru, name_en, description) VALUES
    ('PENDING_PAYMENT', 'Ожидает оплаты', 'Awaiting Payment', 'Order draft awaiting payment confirmation');
