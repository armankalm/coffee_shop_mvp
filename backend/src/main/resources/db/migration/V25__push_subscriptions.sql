-- Browser Web Push subscriptions (one per browser/device). The endpoint is unique:
-- a browser that re-subscribes under another account moves to that account.
CREATE TABLE push_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint   VARCHAR(1000) NOT NULL UNIQUE,
    p256dh     VARCHAR(200) NOT NULL,
    auth       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_push_subscriptions_user_id ON push_subscriptions (user_id);
